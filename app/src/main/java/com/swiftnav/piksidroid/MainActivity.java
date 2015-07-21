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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.loggers.JSONLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class MainActivity extends FragmentActivity {
	String TAG = "PiksiDroid";
	String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	SBPHandler piksiHandler;
	SBPDriverJ2XX piksiDriver;
	private static final String TAG_CONSOLE = "console";
	private static final String TAG_TRACKING = "tracking";
	private static final String TAG_MAP = "map";
	private static final String TAG_BASELINE = "baseline";
	private static final String[] LISTENER_TAGS = {
			TAG_CONSOLE, TAG_TRACKING, TAG_MAP, TAG_BASELINE
	};

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
				if (!mUsbManager.hasPermission(device)) {
					mUsbManager.requestPermission(device, mPermissionIntent);
				} else {
					piksiConnected(device);
				}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mUsbReceiver);
		unregisterReceiver(mUsbReceiverDisconnect);
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

		FragmentManager fm = getFragmentManager();
		for (String tag : LISTENER_TAGS) {
			PiksiListener l = (PiksiListener) fm.findFragmentByTag(tag);
			if (l != null)
				l.piksiConnected(piksiHandler);
		}

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
						//((EditText) findViewById(R.id.console)).setText("Piksi not connected!");
					}
				}
			}
		}
	};

	private void setupUI() {
		ActionBar actionBar = getActionBar();
		actionBar.setLogo(R.mipmap.ic_launcher);
		actionBar.setDisplayUseLogoEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);

		ActionBar.Tab tab = actionBar.newTab()
				.setText("Console")
				.setTabListener(new TabListener<>(
						this, TAG_CONSOLE, ConsoleFragment.class));
		actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setText("Tracking")
				.setTabListener(new TabListener<>(
						this, TAG_TRACKING, TrackingFragment.class));
		actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setText("Map")
				.setTabListener(new TabListener<>(
						this, TAG_MAP, MapFragment.class));
		actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setText("Baseline")
				.setTabListener(new TabListener<>(
						this, TAG_BASELINE, RtkFragment.class));
		actionBar.addTab(tab);

		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
		decorView.setSystemUiVisibility(uiOptions);
	}

	class TabListener<T extends Fragment> implements ActionBar.TabListener {
		private Fragment mFragment;
		private final Activity mActivity;
		private final String mTag;
		private final Class<T> mClass;

		public TabListener(Activity activity, String tag, Class<T> clz) {
			mActivity = activity;
			mTag = tag;
			mClass = clz;
		}

		public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
			// Check if the fragment is already initialized
			if (mFragment == null)
				mFragment = getFragmentManager().findFragmentByTag(mTag);

			if (mFragment == null) {
				// If not, instantiate and add it to the activity
				mFragment = Fragment.instantiate(mActivity, mClass.getName());
				ft.add(android.R.id.tabcontent, mFragment, mTag);
				if (piksiHandler != null)
					((PiksiListener)mFragment).piksiConnected(piksiHandler);
			} else {
				// If it exists, simply attach it in order to show it
				ft.attach(mFragment);
			}
		}

		public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
			if (mFragment != null) {
				// Detach the fragment, because another one is being attached
				ft.detach(mFragment);
			}
		}

		public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
			// User selected the already selected tab. Usually do nothing.
		}
	}
}