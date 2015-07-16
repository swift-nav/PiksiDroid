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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.loggers.JSONLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

		this.setupUI();

		UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);

		filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiverDisconnect, filter);

		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

		for (UsbDevice device : deviceList.values()) {
			if ((device.getVendorId() == Utils.PIKSI_VID) && (device.getProductId() == Utils.PIKSI_PID))
				if (!mUsbManager.hasPermission(device)){
					mUsbManager.requestPermission(device, mPermissionIntent);
				}
				else {
					((EditText) findViewById(R.id.console)).setText("");
					new piksiTask(getApplicationContext(), device).execute();
				}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mUsbReceiver);
		unregisterReceiver(mUsbReceiverDisconnect);
	}

	@Override
	public void onMapReady(GoogleMap gMap) {
		CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(Utils.SWIFT_COORD)
				.zoom(18)
				.build();
		gMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	public class piksiTask extends AsyncTask<Void, Void, Long> {
		private Context mContext;
		private UsbDevice mUsbPiksi;

		public piksiTask(Context context, UsbDevice piksi) {
			mContext = context;
			mUsbPiksi = piksi;
		}

		@Override
		protected Long doInBackground(Void... params) {
			if (piksi != null) {
				handler.stop();
				piksi.close();
				piksi = null;
			}
			try {
				piksi = new PiksiDriver(mContext, mUsbPiksi);
			} catch (IOException e) {
				Log.d(TAG, e.toString());
				e.printStackTrace();
				return null;
			}
			handler = new SBPHandler(piksi);

			try {
				File logfile = new File(getExternalFilesDir("logs"), "logfile");
				OutputStream logstream = new FileOutputStream(logfile);
				handler.add_callback(new JSONLogger(logstream));
			} catch (Exception e) {
				Log.e(TAG, "Error opening JSON log file: " + e.toString());
			}

			((ConsoleFragment)getFragmentManager().findFragmentById(R.id.console_fragment))
					.fixFragment(handler);
			((TrackingFragment)getFragmentManager().findFragmentById(R.id.tracking_fragment))
					.fixFragment(handler);
			((MapFragment)getFragmentManager().findFragmentById(R.id.map_fragment))
					.fixFragment(handler);
			((ObservationFragment)getFragmentManager().findFragmentById(R.id.observation_fragment))
					.connectPiksi(handler);

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
							((EditText) findViewById(R.id.console)).setText("");
							new piksiTask(context, device).execute();
						}
					} else {
						Log.d(TAG, "Permission denied for device " + device);
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
					if (piksi != null) {
						handler.stop();
						piksi.close();
						piksi = null;
						((EditText) findViewById(R.id.console)).setText("Piksi not connected!");
					}
				}
			}
		}
	};

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


		((EditText) findViewById(R.id.console)).setText("Piksi not connected!");
		((EditText) findViewById(R.id.console)).setTextIsSelectable(false);
		((EditText) findViewById(R.id.console)).setClickable(false);

		((ScrollView) findViewById(R.id.scrollView)).setClickable(false);
		((ScrollView) findViewById(R.id.scrollView)).setFocusable(false);
		((ScrollView) findViewById(R.id.scrollView)).setOnTouchListener(null);
		((ScrollView) findViewById(R.id.scrollView)).setPressed(false);

		com.swiftnav.piksidroid.MapFragment mFrag = ((com.swiftnav.piksidroid.MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment));
		com.google.android.gms.maps.MapFragment mGFrag = (com.google.android.gms.maps.MapFragment)mFrag.getChildFragmentManager().findFragmentById(R.id.gmap_fragment);
		mGFrag.getMapAsync(mFrag);
	}

	public void showToast(final String message) {
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
}
