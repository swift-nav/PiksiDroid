package com.swiftnav.piksidroid.test;

import android.util.Log;

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

    public void testReceive() {
        int[] bini = {0x55, 0x15, 0x00, 0xab, 0x03, 0x0d, 0xab, 0xaa,
                0x86, 0x41, 0x00, 0xc0, 0x79, 0x44, 0xd8, 0x7c, 0x0c, 0xc5, 0x11, 0x2e, 0x45};
        byte[] bin = new byte[bini.length];
        /* It can't really be this bad */
        for (int i = 0; i < bini.length; i++) {
            bin[i] = (byte) (bini[i] & 0xff);
        }

        SBPHandler handler = new SBPHandler(new TestDriver(bin));
        SBPMessage msg = handler.receive();
        assertEquals(msg.sender, 939);
        assertEquals(msg.type, 0x15);
        byte[] payload = msg.getPayload();
        assertEquals(payload.length, 13);

        MsgPrint msgPrint = new MsgPrint("Hello World!");

        try {
            handler.send(msgPrint);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception in handler.send()");
        }
        try {
            SBPMessage recvmsg = handler.receive();
            assertTrue(recvmsg != null);
            MsgPrint recvPrint = new MsgPrint(recvmsg);
            assertEquals(recvPrint.text, "Hello World!");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in handler.receive()");
        }
    }

    class TestDriver implements SBPDriver {
        private byte[] data;
        private int pos = 0;

        TestDriver(byte[] dataToRead) {
            data = dataToRead;
        }

        @Override
        public byte[] read(int len) {
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
