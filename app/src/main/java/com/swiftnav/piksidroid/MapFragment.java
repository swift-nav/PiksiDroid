package com.swiftnav.piksidroid;

import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.navigation.MsgPosLLH;
import com.swiftnav.sbp.SBPMessage;

import java.util.LinkedList;

public class MapFragment extends com.google.android.gms.maps.MapFragment
		implements OnMapReadyCallback, PiksiListener {
	View view;
	SBPHandler piksiHandler;

	LinkedList<LatLng> allPiksiPoints = new LinkedList<>();
	LinkedList<Polyline> allPiksiPolylines = new LinkedList<>();

	public MapFragment() {
		this.getMapAsync(this);
		// Required empty public constructor
	}

	@Override
	public void piksiConnected(SBPHandler handler) {
		this.piksiHandler = handler;
		piksiHandler.addCallback(MsgPosLLH.TYPE, llhCallback);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(Utils.SWIFT_COORD)
				.zoom(18)
				.build();
		googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	public SBPCallback llhCallback = new SBPCallback() {
		@Override
		public void receiveCallback(SBPMessage msg_) {
			MsgPosLLH msg = (MsgPosLLH)msg_;

			synchronized (allPiksiPoints) {
				allPiksiPoints.add(new LatLng(msg.lat, msg.lon));
			}
			getActivity().runOnUiThread(new Runnable() {
											@Override
											public void run() {
												updateMap();
											}
										});
		}
	};

	private void updateMap() {
		GoogleMap gMap = this.getMap();
		synchronized (allPiksiPoints) {
			if (allPiksiPoints.size() > 2) {
				LatLng from = allPiksiPoints.get(allPiksiPoints.size() - 1);
				LatLng to = allPiksiPoints.get(allPiksiPoints.size() - 2);
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
}
