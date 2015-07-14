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

import android.content.Context;
import android.util.Log;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.swiftnav.sbp.client.SBPDriver;

import java.io.IOException;

/**
 * Created by vvu on 7/14/15.
 */
public class PiksiDriver implements SBPDriver {
	FT_Device piksi = null;
	String TAG = "PiksiDriver";

	public PiksiDriver(Context context) throws IOException {
			D2xxManager d2xx = D2xxManager.getInstance(context);
			int devCount;
			devCount = d2xx.createDeviceInfoList(context);
			D2xxManager.FtDeviceInfoListNode[] devList = new D2xxManager.FtDeviceInfoListNode[devCount];
			d2xx.getDeviceInfoList(devCount, devList);
			piksi = d2xx.openByIndex(context, 0);
			synchronized (piksi) {
				if (piksi == null) {
					throw new IOException("Cannot open device!");
				}
				if (!piksi.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8, D2xxManager.FT_STOP_BITS_1, D2xxManager.FT_PARITY_NONE)) {
					throw new IOException("Cannot set 8,1,N!");
				}
				if (!piksi.setBaudRate(Utils.baudrate)) {
					throw new IOException("Cannot set baudrate!!");
				}
				piksi.stopInTask();
				piksi.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
				piksi.restartInTask();
			}
	}

	public void close() {
		synchronized (piksi) {
			piksi.close();
		}
	}

	@Override
	public byte[] read(int len) throws IOException {
		byte[] data = new byte[len];
		synchronized (piksi) {
			if (!piksi.isOpen())
				throw new IOException("Piksi device is closed!");
			piksi.read(data, len);
		}
		return data;
	}

	@Override
	public void write(byte[] data) throws IOException {
		piksi.write(data);
	}
}
