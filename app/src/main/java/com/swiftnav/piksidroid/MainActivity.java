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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TabHost;

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
	SBPHandler piksiHandler;
	SBPDriverJ2XX piksiDriver;

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
					piksiConnected(device);
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

	private void piksiConnected(UsbDevice usbdev) {
		if (piksiDriver != null) {
			piksiHandler.stop();
			piksiDriver.close();
			piksiDriver = null;
		}
		try {
			piksiDriver = new SBPDriverJ2XX(this, usbdev);
		} catch (IOException e) {
			Log.d(TAG, e.toString());
			e.printStackTrace();
			return;
		}
		piksiHandler = new SBPHandler(piksiDriver);

		try {
			File logfile = new File(getExternalFilesDir("logs"), "logfile");
			OutputStream logstream = new FileOutputStream(logfile);
			piksiHandler.addCallback(new JSONLogger(logstream));
		} catch (Exception e) {
			Log.e(TAG, "Error opening JSON log file: " + e.toString());
		}

		((ConsoleFragment) getFragmentManager().findFragmentById(R.id.console_fragment))
				.fixFragment(piksiHandler);
		((TrackingFragment) getFragmentManager().findFragmentById(R.id.tracking_fragment))
				.fixFragment(piksiHandler);
		((MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment))
				.fixFragment(piksiHandler);
		((ObservationFragment) getFragmentManager().findFragmentById(R.id.observation_fragment))
				.connectPiksi(piksiHandler);
		((RtkFragment) getFragmentManager().findFragmentById(R.id.rtk_fragment))
				.fixFragment(piksiHandler);

		Log.d(TAG, "All ready to go...");

		piksiHandler.start();
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					if (device != null) {
						((EditText) findViewById(R.id.console)).setText("");
						piksiConnected(device);
					}
				} else {
					Log.e(TAG, "Permission denied for device " + device);
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
					if (piksiDriver != null) {
						piksiHandler.stop();
						piksiDriver.close();
						piksiDriver = null;
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

		tabSpec = tabHost.newTabSpec("RTK");
		tabSpec.setContent(R.id.rtk);
		tabSpec.setIndicator("RTK");
		tabHost.addTab(tabSpec);

		tabHost.setOnTabChangedListener(tabChanger);

		((EditText) findViewById(R.id.console)).setText("Piksi not connected!");
		((EditText) findViewById(R.id.console)).setTextIsSelectable(false);
		findViewById(R.id.console).setClickable(false);

		findViewById(R.id.scrollView).setClickable(false);
		findViewById(R.id.scrollView).setFocusable(false);
		findViewById(R.id.scrollView).setOnTouchListener(null);
		findViewById(R.id.scrollView).setPressed(false);

		com.swiftnav.piksidroid.MapFragment mFrag = ((com.swiftnav.piksidroid.MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment));
		com.google.android.gms.maps.MapFragment mGFrag = (com.google.android.gms.maps.MapFragment)mFrag.getChildFragmentManager().findFragmentById(R.id.gmap_fragment);
		mGFrag.getMapAsync(mFrag);


		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
		decorView.setSystemUiVisibility(uiOptions);
	}

	public TabHost.OnTabChangeListener tabChanger = new TabHost.OnTabChangeListener() {
		@Override
		public void onTabChanged(String tabId) {
			TrackingFragment mTrack = ((TrackingFragment) getFragmentManager().findFragmentById(R.id.tracking_fragment));
			if (tabId == "Tracking") {
				LinearLayout l = ((LinearLayout)findViewById(R.id.tabItemsLayout));
				Switch barChartSwitch = new Switch(getApplicationContext());

				barChartSwitch.setText("Bar Chart");
				barChartSwitch.setEnabled(true);
				RelativeLayout.LayoutParams params  = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
				barChartSwitch.setLayoutParams(params);
				barChartSwitch.setOnCheckedChangeListener(swChange);

				l.addView(barChartSwitch);

				mTrack.getView().findViewById(R.id.tracking_line_chart).setVisibility(View.VISIBLE);
				mTrack.getView().findViewById(R.id.tracking_bar_chart).setVisibility(View.GONE);
			}
			else {
				LinearLayout l = ((LinearLayout)findViewById(R.id.tabItemsLayout));
				if (l.getChildCount() > 1)
					l.removeViewAt(1);
				mTrack.getView().findViewById(R.id.tracking_line_chart).setVisibility(View.GONE);
				mTrack.getView().findViewById(R.id.tracking_bar_chart).setVisibility(View.VISIBLE);
			}
		}
	};

	public CompoundButton.OnCheckedChangeListener swChange = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			TrackingFragment mTrack = ((TrackingFragment) getFragmentManager().findFragmentById(R.id.tracking_fragment));
			if (isChecked) {
				mTrack.getView().findViewById(R.id.tracking_line_chart).setVisibility(View.GONE);
				mTrack.getView().findViewById(R.id.tracking_bar_chart).setVisibility(View.VISIBLE);
			}
			else {
				mTrack.getView().findViewById(R.id.tracking_line_chart).setVisibility(View.VISIBLE);
				mTrack.getView().findViewById(R.id.tracking_bar_chart).setVisibility(View.GONE);
			}
		}
	};
}
