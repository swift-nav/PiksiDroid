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

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;

public class Utils {
	public static int LASTLINES = 200;
	public static int PIKSI_VID = 0x403;
	public static int PIKSI_PID = 0x6014;
	public static LatLng SWIFT_COORD = new LatLng(37.7625047, -122.3889322);
	public static int[] COLOR_LIST = {
			Color.RED,
			Color.BLACK,
			Color.GREEN,
			Color.YELLOW,
			Color.BLUE,
			Color.MAGENTA,
			Color.CYAN,
			Color.DKGRAY,
			Color.LTGRAY,
			Color.MAGENTA,
			Color.BLUE
	};
}
