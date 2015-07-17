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


/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback{

	View view;
	SBPHandler piksiHandler;

	LinkedList<PiksiPoint> allPiksiPoints = new LinkedList<>();
	LinkedList<Polyline> allPiksiPolylines = new LinkedList<>();

	public MapFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.fragment_map, container, false);
		return view;
	}

	public void fixFragment(SBPHandler handler) {
		this.piksiHandler = handler;
		piksiHandler.add_callback(MsgPosLLH.TYPE, llhCallback);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {

	}

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
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					com.google.android.gms.maps.MapFragment mapFragment = (com.google.android.gms.maps.MapFragment) getChildFragmentManager()
							.findFragmentById(R.id.gmap_fragment);
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
}
