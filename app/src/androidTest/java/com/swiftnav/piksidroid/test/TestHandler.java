package com.swiftnav.piksidroid.test;

import android.util.Log;

import com.swiftnav.piksidroid.HexDump;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPDriver;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.msg.MsgPrint;
import com.swiftnav.sbp.msg.SBPMessage;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by gareth on 7/13/15.
 */
public class TestHandler extends TestCase {
    static final String TAG = "SBPTestHandler";
    static final String TEST_TEXT = "Hello World!";
    boolean gotPrint;
    String printText;

    public void testReceive() {
        SBPHandler handler = new SBPHandler(new TestDriver());
        handler.add_callback(SBPMessage.SBP_MSG_PRINT, new SBPCallback() {
            @Override
            public void receiveCallback(SBPMessage msg) {
                MsgPrint msgPrint = null;
                try {
                    msgPrint = new MsgPrint(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                assertTrue(msgPrint != null);
                Log.d(TAG, "MsgPrint: " + msgPrint.text);
                printText = msgPrint.text;
                gotPrint = true;
            }
        });

        MsgPrint sendPrint = new MsgPrint(TEST_TEXT);

        try {
            handler.send(sendPrint);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception in handler.send()");
        }

        handler.start();
        while(!gotPrint);
        handler.stop();

        assertEquals(printText, TEST_TEXT);
    }

    class TestDriver implements SBPDriver {
        private byte[] data;
        private int pos = 0;

        TestDriver() {
            data = new byte[0];
        }

        TestDriver(byte[] dataToRead) {
            data = dataToRead;
        }

        @Override
        public byte[] read(int len) {
            if (pos + len > data.length) {
                Log.d(TAG, "Read past end of buffer!");
                return null;
            }

            byte[] ret = Arrays.copyOfRange(data, pos, pos+len);
            pos += len;
            return ret;
        }

        @Override
        public void write(byte[] data_) throws IOException {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(data);
            stream.write(data_);
            data = stream.toByteArray();
        }
    }
}
