package com.swiftnav.piksidroid;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.swiftnav.sbp.SBPMessage;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.tracking.MsgTrackingStateDepA;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class TrackingFragment extends Fragment {
	View view;
	ArrayList<ArrayList<Entry>> lineEntries = new ArrayList<>();
	ArrayList<ArrayList<BarEntry>> barEntries = new ArrayList<>();

	ArrayList<LineDataSet> lineDataSets = new ArrayList<>();
	ArrayList<BarDataSet> barDataSets = new ArrayList<>();

	ArrayList<String> xValsLine = new ArrayList<>();
	ArrayList<String> xValsBar = new ArrayList<>();

	Boolean firstTrackingMessage = true;
	SBPHandler piksiHandler;

	public TrackingFragment() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_tracking, container, false);

		for (int i = 0; i < 100; i++)
			xValsLine.add("" + i);

		xValsBar.add("");
		xValsBar.add("");

		setupLineChart();
		setupBarChart();

		return view;
	}

	private void setupLineChart() {
		LineChart mLineChart = (LineChart) view.findViewById(R.id.tracking_line_chart);
		Legend mLegend = mLineChart.getLegend();
		XAxis bottomAxis = mLineChart.getXAxis();
		YAxis rightAxis = mLineChart.getAxisRight();
		YAxis leftAxis = mLineChart.getAxisLeft();

		mLineChart.setHardwareAccelerationEnabled(true);
		mLineChart.setDescription("");
		mLegend.setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);
		mLegend.setTextColor(Color.WHITE);
		mLegend.setTextSize(4f);
		mLegend.setYOffset(4f);
		mLegend.setEnabled(true);
		bottomAxis.setEnabled(false);
		leftAxis.setTextColor(Color.WHITE);
		rightAxis.setTextColor(Color.WHITE);
	}

	private void setupBarChart() {
		BarChart mBarChart = (BarChart)view.findViewById(R.id.tracking_bar_chart);
		Legend mLegend = mBarChart.getLegend();
		XAxis bottomAxis = mBarChart.getXAxis();
		YAxis rightAxis = mBarChart.getAxisRight();
		YAxis leftAxis = mBarChart.getAxisLeft();

		mBarChart.setHardwareAccelerationEnabled(true);
		mBarChart.setDescription("");
		mBarChart.setDrawValuesForWholeStack(true);
		mBarChart.setHighlightEnabled(false);
		mLegend.setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);
		mLegend.setTextColor(Color.WHITE);
		mLegend.setTextSize(4f);
		mLegend.setEnabled(true);

		bottomAxis.setEnabled(true);
		leftAxis.setTextColor(Color.WHITE);
		rightAxis.setTextColor(Color.WHITE);
	}

	public void fixFragment(SBPHandler handler) {
		this.piksiHandler = handler;
		this.piksiHandler.add_callback(MsgTrackingStateDepA.TYPE, trackingCallback);
	}

	public SBPCallback trackingCallback = new SBPCallback() {
		@Override
		public void receiveCallback(SBPMessage msg) {
			MsgTrackingStateDepA t = null;
			try {
				t = new MsgTrackingStateDepA(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
			final MsgTrackingStateDepA track = t;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					int len = track.states.length;

					if (firstTrackingMessage) {
						firstTrackingMessage = false;

						for (int i = 0; i < len; i++) {
							lineEntries.add(new ArrayList<Entry>());
							barEntries.add(new ArrayList<BarEntry>());

							LineDataSet tmpLineDataSet = new LineDataSet(lineEntries.get(i), "Chan " + i);
							tmpLineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
							tmpLineDataSet.setDrawCircles(false);
							tmpLineDataSet.setDrawCircleHole(false);
							tmpLineDataSet.setDrawCubic(false);
							tmpLineDataSet.setLineWidth(1f);
							tmpLineDataSet.setDrawValues(false);
							tmpLineDataSet.setColor(Utils.COLOR_LIST[i]);


							BarDataSet tmpBarDataSet = new BarDataSet(barEntries.get(i), "Chan " + i);
							tmpBarDataSet.setDrawValues(true);
							tmpBarDataSet.setColor(Utils.COLOR_LIST[i]);

							lineDataSets.add(tmpLineDataSet);
							barDataSets.add(tmpBarDataSet);
						}
					} else {
						for (int i = 0; i < len; i++) {
							MsgTrackingStateDepA.TrackingChannelStateDepA chanState = track.states[i];
							float cn0 = chanState.cn0;
							int state = chanState.state;
							int prn = chanState.prn;
							barEntries.get(i).clear();

							LineDataSet tmpLineDataSet = lineDataSets.get(i);
							BarDataSet tmpBarDataSet = barDataSets.get(i);

							tmpBarDataSet.addEntry(new BarEntry(cn0, i));

							if (state == 1) {
								tmpLineDataSet.setLabel("C-" + i + " PRN " + prn);
								tmpBarDataSet.setLabel("C-" + i + " PRN " + prn);
							} else {
								tmpLineDataSet.setLabel("Chan " + i);;
								tmpBarDataSet.setLabel("Chan " + i);
							}

							if (tmpLineDataSet.getEntryCount() == 100) {
								for (int j = 0; j < 100; j += 1) {
									Entry current = tmpLineDataSet.getEntryForXIndex(j);
									Entry next = tmpLineDataSet.getEntryForXIndex(j + 1);
									current.setVal(next.getVal());
								}
								tmpLineDataSet.getEntryForXIndex(99).setVal(cn0);
							} else {
								Entry e = new Entry(cn0, tmpLineDataSet.getEntryCount());
								tmpLineDataSet.addEntry(e);
							}
						}

						LineData lineData = new LineData(xValsLine, lineDataSets);
						BarData barData = new BarData(xValsBar, barDataSets);
						LineChart mLineChart = ((LineChart)view.findViewById(R.id.tracking_line_chart));
						BarChart mBarChart = ((BarChart)view.findViewById(R.id.tracking_bar_chart));

						try {
							mLineChart.setData(lineData);
							mBarChart.setData(barData);

							if (mLineChart.getVisibility() == View.VISIBLE) {
								mLineChart.invalidate();
							}
							if (mBarChart.getVisibility() == View.VISIBLE) {
								mBarChart.invalidate();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
	};
}