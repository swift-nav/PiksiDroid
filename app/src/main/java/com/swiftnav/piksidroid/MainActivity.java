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
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPDriver;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.msg.MsgPrint;
import com.swiftnav.sbp.msg.SBPMessage;

import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
	String TAG = "PiksiDroid";
	String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

	static final int READBUF_SIZE = 256;
	byte[] rbuf = new byte[READBUF_SIZE];

	SBPHandler handler = null;

	Socket piksiConsole;
	OutputStream output;

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
	}

	View.OnClickListener read_listen = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (piksi == null) {
				showToast("Piksi not connected!");
				return;
			}
		}
	};

	View.OnClickListener connect_listen = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			new Thread(socketLoop).start();
		}
	};

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (device != null) {
							piksi = new PiksiDriver(context);
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
									Log.d(TAG, "MsgPrint: " + msgPrint.text);
								}
							});
							Log.d(TAG, "All ready to go...");
							//handler.start();
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
					// call your method that cleans up and closes communication with the device
					Log.e(TAG, "Device disconnected!");
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

	private Runnable socketLoop = new Runnable() {
		@Override
		public void run() {
			int serverPort;
			String serverIP = ((EditText)findViewById(R.id.serverIP)).getText().toString().trim();
			try {
				serverPort = Integer.parseInt(((EditText)findViewById(R.id.serverPort)).getText().toString().trim());
			}
			catch (NumberFormatException e) {
				showToast("Please insert a good server port!");
				return;
			}
			try {
				piksiConsole = new Socket(serverIP, serverPort);
				output = piksiConsole.getOutputStream();
				showToast("Connected to " + serverIP);
			} catch (Exception e) {
				Log.d(TAG, e.toString());
				showToast("Could not connect to " + serverIP);
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

		Button read_button = (Button) findViewById(R.id.read_button);
		read_button.setEnabled(false);
		read_button.setOnClickListener(read_listen);

		Button connect_button = (Button) findViewById(R.id.serverConnect);
		connect_button.setOnClickListener(connect_listen);

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
