package com.swiftnav.piksidroid;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by vvu on 7/15/15.
 */
public class PiksiPoint {
	double mLat;
	double mLng;

	PiksiPoint(double lat, double lng) {
		mLat = lat;
		mLng = lng;
	}

	LatLng getLatLng() {
		return new LatLng(mLat, mLng);
	}
}
