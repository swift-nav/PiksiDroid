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
import android.hardware.usb.UsbDevice;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.swiftnav.sbp.client.SBPDriver;

import java.io.IOException;

public class SBPDriverJ2XX implements SBPDriver {
    public static final int BAUDRATE_DEFAULT = 1000000;
    public static final int TIMEOUT_READ = 100;
    final FT_Device ftdev;
    boolean doneInit;

    public SBPDriverJ2XX(Context context, UsbDevice usbdev) throws IOException {
        this(context, usbdev, BAUDRATE_DEFAULT);
    }

    public SBPDriverJ2XX(Context context, UsbDevice usbdev, int baudrate) throws IOException {
        D2xxManager d2xx = D2xxManager.getInstance(context);

        d2xx.addUsbDevice(usbdev);
        ftdev = d2xx.openByUsbDevice(context, usbdev);
        if (ftdev == null) {
            throw new IOException("Cannot open device!");
        }

        if (!ftdev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8,
                D2xxManager.FT_STOP_BITS_1,
                D2xxManager.FT_PARITY_NONE)) {
            throw new IOException("Cannot set 8,1,N!");
        }
        if (!ftdev.setBaudRate(baudrate)) {
            throw new IOException("Cannot set baudrate!!");
        }
    }

    public void close() {
        synchronized (this) {
            ftdev.close();
        }
    }

    @Override
    public byte[] read(int len) throws IOException {
        if (!doneInit)
            doInit();
        byte[] data = new byte[len];
        synchronized (this) {
            if (!ftdev.isOpen())
                throw new IOException("Piksi device is closed!");
            ftdev.read(data, len, TIMEOUT_READ);
        }
        return data;
    }

    @Override
    public void write(byte[] data) throws IOException {
        if (!doneInit)
            doInit();
        ftdev.write(data);
    }

    private void doInit() {
        synchronized (this) {
            if (doneInit)
                return;
            ftdev.stopInTask();
            ftdev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
            ftdev.restartInTask();
            doneInit = true;
        }
    }
}
