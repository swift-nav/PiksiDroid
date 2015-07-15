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
import android.graphics.Color;
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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.loggers.JSONLogger;
import com.swiftnav.sbp.msg.MsgPosLLH;
import com.swiftnav.sbp.msg.MsgPrint;
import com.swiftnav.sbp.msg.MsgTrackingState;
import com.swiftnav.sbp.msg.SBPMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
	String TAG = "PiksiDroid";
	String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	SBPHandler handler = null;
	PiksiDriver piksi;
	LinkedList<PiksiPoint> allPiksiPoints = new LinkedList<>();
	LinkedList<Polyline> allPiksiPolylines = new LinkedList<>();
	ArrayList<ArrayList<Entry>> chanEntries = new ArrayList<>();
	ArrayList<String> xVals = new ArrayList<String>();
	ArrayList<Entry> valsChan1 = new ArrayList<>();
	ArrayList<LineDataSet> dataSets = new ArrayList<>();
	int msgCount = 0;
	Boolean firstTrackingMessage = true;
	Semaphore chart = new Semaphore(1);

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
			if ((device.getVendorId() == Utils.PIKSI_VID) && (device.getProductId() == Utils.PIKSI_PID))
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

	public SBPCallback printCallback = new SBPCallback() {
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
	};

	public SBPCallback llhCallback = new SBPCallback() {
		@Override
		public void receiveCallback(SBPMessage msg) {
			MsgPosLLH posLLH = null;
			try {
				posLLH = new MsgPosLLH(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
			final double lat = posLLH.lat;
			final double lon = posLLH.lon;

			synchronized (allPiksiPoints) {
				allPiksiPoints.add(new PiksiPoint(lat, lon));
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					MapFragment mapFragment = (MapFragment) getFragmentManager()
							.findFragmentById(R.id.map_fragment);
					GoogleMap gMap = mapFragment.getMap();
					synchronized (allPiksiPoints) {
						if (allPiksiPoints.size() > 2) {
							LatLng from = allPiksiPoints.get(allPiksiPoints.size() - 1).getLatLng();
							LatLng to = allPiksiPoints.get(allPiksiPoints.size() - 2).getLatLng();
							Polyline line = gMap.addPolyline(
									new PolylineOptions()
											.add(from)
											.add(to).width(2)
											.color(Color.RED));
							allPiksiPolylines.add(line);
							CameraPosition cameraPosition = new CameraPosition.Builder()
									.target(to)
									.zoom(18)
									.build();
							gMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
							if (allPiksiPolylines.size() > Utils.LASTLINES) {
								Polyline rLine = allPiksiPolylines.get(0);
								rLine.remove();

								allPiksiPoints.remove(0);
								allPiksiPoints.remove(1);
								allPiksiPolylines.remove(0);
							}
						}
					}
				}
			});
		}
	};

	public SBPCallback trackingCallback = new SBPCallback() {
		@Override
		public void receiveCallback(SBPMessage msg) {
			MsgTrackingState track = null;
			try {
				track = new MsgTrackingState(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}

			int len = track.states.length;
			if (firstTrackingMessage) {
				firstTrackingMessage = false;
				for (int i = 0; i < len; i++) {
					chanEntries.add(new ArrayList<Entry>());
					LineDataSet tmpDataSet = new LineDataSet(chanEntries.get(i), "Chan " + i);
					tmpDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
					tmpDataSet.setDrawCircles(false);
					tmpDataSet.setDrawCircleHole(false);
					tmpDataSet.setDrawCubic(false);
					tmpDataSet.setLineWidth(2f);
					tmpDataSet.setDrawValues(false);
					dataSets.add(tmpDataSet);
				}
			} else {
				try {
					chart.acquire();
					for (int i = 0; i < len; i++) {
						MsgTrackingState.TrackingChannelState chanState = track.states[i];
						float cn0 = chanState.cn0;
						int running = chanState.state;
						int prn = chanState.prn;
						ArrayList<Entry> cEntries = chanEntries.get(i);

						if (cEntries.size() > 99) {
							for (int pos = 0; pos < 99; pos++) {
								Entry tmp = cEntries.get(pos + 1);
								tmp.setXIndex(pos);
								cEntries.set(pos, tmp);
							}
							Entry e = new Entry(cn0, cEntries.size());
							cEntries.set(99, e);
						} else {
							Entry e = new Entry(cn0, cEntries.size());
							cEntries.add(e);
						}
						dataSets.set(i, new LineDataSet(cEntries, "Chan " + i));
						LineDataSet tmpLine = dataSets.get(i);

						tmpLine.setAxisDependency(YAxis.AxisDependency.LEFT);
						tmpLine.setDrawCircles(false);
						tmpLine.setDrawCircleHole(false);
						tmpLine.setDrawCubic(false);
						tmpLine.setLineWidth(1f);
						tmpLine.setDrawValues(false);
						tmpLine.setColor(Utils.COLOR_LIST[i]);
					}
					final ArrayList<LineDataSet> fDataSets = dataSets;
					final ArrayList<String> fxVals = xVals;
					chart.release();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							try {
								chart.acquire();
								LineData data = new LineData(fxVals, fDataSets);
								LineChart mLineChart = ((LineChart) findViewById(R.id.tracking_chart));
								mLineChart.setData(data);
								mLineChart.invalidate();
							} catch (InterruptedException e) {
								e.printStackTrace();
							} finally {
								chart.release();
							}
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {

				}
			}
		}

	};

	public class piksiTask extends AsyncTask<Void, Void, Long> {
		private Context mContext;

		public piksiTask(Context context) {
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

			try {
				File logfile = new File(getExternalFilesDir("logs"), "logfile");
				OutputStream logstream = new FileOutputStream(logfile);
				handler.add_callback(new JSONLogger(logstream));
			} catch (Exception e) {
				Log.e(TAG, "Error opening JSON log file: " + e.toString());
			}
			handler.add_callback(SBPMessage.SBP_MSG_PRINT, printCallback);
			handler.add_callback(SBPMessage.SBP_MSG_POS_LLH, llhCallback);
			handler.add_callback(SBPMessage.SBP_MSG_TRACKING_STATE, trackingCallback);

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
							new piksiTask(context).execute();
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

		MapFragment mapFragment = (MapFragment) getFragmentManager()
				.findFragmentById(R.id.map_fragment);
		mapFragment.getMapAsync(this);

		((EditText) findViewById(R.id.console)).setText("Piksi not connected!");

		LineChart mLineChart = (LineChart) findViewById(R.id.tracking_chart);
		Legend mLegend = mLineChart.getLegend();
		XAxis bottomAxis = mLineChart.getXAxis();
		YAxis rightAxis = mLineChart.getAxisRight();
		YAxis leftAxis = mLineChart.getAxisLeft();

		mLineChart.setHardwareAccelerationEnabled(true);
		mLineChart.setTouchEnabled(false);
		mLineChart.setDescription("");
		mLineChart.setDescriptionColor(Color.DKGRAY);
		mLegend.setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);
		mLegend.setTextColor(Color.WHITE);
		mLegend.setTextSize(9f);
		mLegend.setEnabled(true);
		bottomAxis.setEnabled(false);
		leftAxis.setTextColor(Color.WHITE);
		rightAxis.setTextColor(Color.WHITE);

		for (int i = 0; i < 100; i++)
			xVals.add("" + i);
	}

	@Override
	public void onMapReady(GoogleMap gMap) {
		CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(Utils.SWIFT_COORD)
				.zoom(18)
				.build();
		gMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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
