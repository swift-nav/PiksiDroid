/*
 * # Copyright (C) 2011-2015 Swift Navigation Inc.
 * # Contact: Vlad Ungureanu <vvu@vdev.ro>
 * #
 * # This source is subject to the license found in the file 'LICENSE' which must
 * # be be distributed together with this source. All other rights reserved.
 * #
 * # THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
 * # EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * # WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package com.swiftnav.piksidroid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.msg.MsgPrint;
import com.swiftnav.sbp.msg.SBPMessage;

import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
	String TAG = "PiksiDroid";
	String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	SBPHandler handler = null;
	PiksiDriver piksi;

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);

		filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiverDisconnect, filter);

		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

		for (UsbDevice device : deviceList.values()) {
			if ((device.getVendorId() == 0x403) && (device.getProductId() == 0x6014))
				mUsbManager.requestPermission(device, mPermissionIntent);
		}

		this.setupUI();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mUsbReceiver);
		unregisterReceiver(mUsbReceiverDisconnect);
	}

	public class piksiTask extends AsyncTask<Void, Void, Long> {
		private Context mContext;
		public piksiTask (Context context){
			mContext = context;
		}

		@Override
		protected Long doInBackground(Void... params) {
			if (piksi != null) {
				handler.stop();
				piksi.close();
				piksi = null;
			}
			try {
				piksi = new PiksiDriver(mContext);
			} catch (IOException e) {
				Log.d(TAG, e.toString());
				e.printStackTrace();
				return null;
			}
			handler = new SBPHandler(piksi);
			handler.add_callback(SBPMessage.SBP_MSG_PRINT, new SBPCallback() {
				@Override
				public void receiveCallback(SBPMessage msg) {
					MsgPrint msgPrint = null;
					try {
						msgPrint = new MsgPrint(msg);
					} catch (Exception e) {
						e.printStackTrace();
					}
					final MsgPrint message = msgPrint;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							EditText console = (EditText) findViewById(R.id.console);
							final ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
							console.append(message.text);
							final EditText c = console;
							sv.post(new Runnable() {
								@Override
								public void run() {
									sv.smoothScrollTo(0, c.getBottom());
								}
							});
						}
					});
				}
			});
			Log.d(TAG, "All ready to go...");
			handler.start();
			return null;
		}
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (device != null) {
							new piksiTask(context).execute();
						}
					} else {
						Log.d(TAG, "permission denied for device " + device);
					}
				}
			}
		}
	};

	BroadcastReceiver mUsbReceiverDisconnect = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (device != null) {
					Log.e(TAG, "Device disconnected!");
					if (piksi != null ) {
						handler.stop();
						piksi.close();
						piksi = null;
						Log.d(TAG, "Closed Piksi from outside driver!");
					}
				}
			}
		}
	};

	private void showToast(final String message) {
		runOnUiThread(new Runnable() {
						  public void run() {
							  Context context = getApplicationContext();
							  int duration = Toast.LENGTH_SHORT;
							  Toast toast = Toast.makeText(context, message, duration);
							  toast.show();
						  }
					  }
		);
	}

	private void setupUI() {
		TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
		tabHost.setup();


		TabHost.TabSpec tabSpec = tabHost.newTabSpec("Piksi");
		tabSpec.setContent(R.id.piksi);
		tabSpec.setIndicator("Piksi");
		tabHost.addTab(tabSpec);

		tabSpec = tabHost.newTabSpec("Tracking");
		tabSpec.setContent(R.id.tracking);
		tabSpec.setIndicator("Tracking");
		tabHost.addTab(tabSpec);

		tabSpec = tabHost.newTabSpec("Map");
		tabSpec.setContent(R.id.map);
		tabSpec.setIndicator("Map");
		tabHost.addTab(tabSpec);

		tabSpec = tabHost.newTabSpec("Observation");
		tabSpec.setContent(R.id.observation);
		tabSpec.setIndicator("Observation");
		tabHost.addTab(tabSpec);

		MapFragment mapFragment = (MapFragment) getFragmentManager()
				.findFragmentById(R.id.map_fragment);
		mapFragment.getMapAsync(this);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		googleMap.addMarker(new MarkerOptions()
				.position(new LatLng(0, 0))
				.title("Marker"));
	}

}
