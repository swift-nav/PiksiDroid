package com.swiftnav.piksidroid;


import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.msg.MsgTrackingState;
import com.swiftnav.sbp.msg.SBPMessage;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;


/**
 * A simple {@link Fragment} subclass.
 */
public class TrackingFragment extends Fragment {
	View view;
	ArrayList<ArrayList<Entry>> chanEntries = new ArrayList<>();
	ArrayList<String> xVals = new ArrayList<>();
	ArrayList<LineDataSet> dataSets = new ArrayList<>();
	Boolean firstTrackingMessage = true;
	Semaphore chart = new Semaphore(1);
	SBPHandler piksiHandler;

	public TrackingFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_tracking, container, false);

		LineChart mLineChart = (LineChart) view.findViewById(R.id.tracking_chart);
		Legend mLegend = mLineChart.getLegend();
		XAxis bottomAxis = mLineChart.getXAxis();
		YAxis rightAxis = mLineChart.getAxisRight();
		YAxis leftAxis = mLineChart.getAxisLeft();

		mLineChart.setHardwareAccelerationEnabled(true);
		mLineChart.setTouchEnabled(false);
		mLineChart.setDescription("");
		mLegend.setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);
		mLegend.setTextColor(Color.WHITE);
		mLegend.setTextSize(4f);
		mLegend.setEnabled(true);
		bottomAxis.setEnabled(false);
		leftAxis.setTextColor(Color.WHITE);
		rightAxis.setTextColor(Color.WHITE);

		for (int i = 0; i < 100; i++)
			xVals.add("" + i);

		return view;
	}

	public void fixFragment(SBPHandler handler) {
		this.piksiHandler = handler;
		piksiHandler.add_callback(SBPMessage.SBP_MSG_TRACKING_STATE, trackingCallback);
	}

	public SBPCallback trackingCallback = new SBPCallback() {
		@Override
		public void receiveCallback(SBPMessage msg) {
			MsgTrackingState t = null;
			try {
				t = new MsgTrackingState(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
			final MsgTrackingState track = t;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
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
							tmpDataSet.setLineWidth(1f);
							tmpDataSet.setDrawValues(false);
							tmpDataSet.setColor(Utils.COLOR_LIST[i]);

							dataSets.add(tmpDataSet);
						}
					} else {
						for (int i = 0; i < len; i++) {
							MsgTrackingState.TrackingChannelState chanState = track.states[i];
							float cn0 = chanState.cn0;
							int state = chanState.state;
							LineDataSet tmpDataSet = dataSets.get(i);
							if (state > 0) {
								tmpDataSet.setLabel("PRN " + chanState.prn);
							} else {
								tmpDataSet.setLabel("Chan " + i);
							}
							if (tmpDataSet.getEntryCount() == 100) {
								for (int j = 0; j < 100; j += 1) {
									Entry current = tmpDataSet.getEntryForXIndex(j);
									Entry next = tmpDataSet.getEntryForXIndex(j + 1);
									current.setVal(next.getVal());
								}
								tmpDataSet.getEntryForXIndex(99).setVal(cn0);
							} else {
								Entry e = new Entry(cn0, tmpDataSet.getEntryCount());
								tmpDataSet.addEntry(e);
							}
						}

						final ArrayList<LineDataSet> fDataSets = (ArrayList<LineDataSet>) dataSets.clone();
						final ArrayList<String> fxVals = (ArrayList<String>) xVals.clone();

						LineData data = new LineData(fxVals, fDataSets);
						LineChart mLineChart = ((LineChart) view.findViewById(R.id.tracking_chart));
						try {
							mLineChart.setData(data);
							mLineChart.invalidate();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
	};
}