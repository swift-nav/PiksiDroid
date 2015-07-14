package com.swiftnav.piksidroid.test;

import android.util.Log;

import com.swiftnav.piksidroid.HexDump;
import com.swiftnav.sbp.client.CRC16;

import junit.framework.TestCase;


public class TestCRC16 extends TestCase {
    private final static String TAG = "CRC16";

    public void testCRC() {
        byte[] hello = "Hello".getBytes();
        Log.d(TAG, HexDump.dumpHexString(hello));
        Log.d(TAG, "CRC16(Hello) = " + CRC16.crc16(hello));
        assertEquals(CRC16.crc16(hello), 0xcbd6);
    }
}
