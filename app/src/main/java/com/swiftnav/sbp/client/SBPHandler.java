package com.swiftnav.sbp.client;

import android.util.Log;

import com.swiftnav.piksidroid.HexDump;
import com.swiftnav.sbp.msg.SBPMessage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

public class SBPHandler {
    static final byte PREAMBLE = 0x55;
    static final String TAG = "SBPHandler";

    private SBPDriver driver;
    LinkedList<Callback> callbacks;
    ReceiveThread receiveThread;

    public SBPHandler(SBPDriver driver_) {
        driver = driver_;
        callbacks = new LinkedList<>();
    }

    public void start() {
        assert(receiveThread == null);
        receiveThread = new ReceiveThread();
        receiveThread.start();
    }

    public void stop() {
        receiveThread.finish();
        try {
            receiveThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        receiveThread = null;
    }

    public SBPMessage receive() {
        byte[] preamble = driver.read(1);
        if ((preamble == null) || (preamble[0] != PREAMBLE)) {
            return null;
        }

        byte[] bheader = driver.read(Header.SIZE);
        int calccrc = CRC16.crc16(bheader);
        Header header = new Header(bheader);

        byte[] data = driver.read(header.len);
        calccrc = CRC16.crc16(data, calccrc);

        int crc = ByteBuffer.wrap(driver.read(2)).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
        if (crc != calccrc)
            Log.d(TAG, String.format("Barf 0x%X != 0x%X", crc, calccrc));

        Log.d(TAG, "Type = " + header.type);
        Log.d(TAG, "Sender = " + header.sender);
        Log.d(TAG, "Length = " + header.len);
        Log.d(TAG, HexDump.dumpHexString(data));

        return new SBPMessage(header.sender, header.type, data);
    }

    public void send(SBPMessage msg) throws IOException {
        byte[] payload = msg.getPayload();
        Header header = new Header(msg.sender, msg.type, payload.length);
        byte[] headerb = header.build();
        driver.write(new byte[] {PREAMBLE});
        driver.write(headerb);
        driver.write(payload);
        int crc = CRC16.crc16(headerb);
        crc = CRC16.crc16(payload, crc);
        driver.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) crc).array());
    }

    public void add_callback(int id, SBPCallback cb) {
        callbacks.add(new Callback(id, cb));
    }

    public void add_callback(int[] id, SBPCallback cb) {
        callbacks.add(new Callback(id, cb));
    }

    private class ReceiveThread extends Thread {
        boolean stopFlag = false;

        @Override
        public void run() {
            while (!stopFlag) {
                SBPMessage msg = receive();
                if (msg == null)
                    break;

                for (Callback cb : callbacks) {
                    cb.dispatch(msg);
                }
            }
        }

        public void finish() {
            stopFlag = true;
        }
    }

    private class Header {
        public static final int SIZE = 5;
        int type;
        int sender;
        int len;

        Header(byte[] binary) {
            ByteBuffer bb = ByteBuffer.wrap(binary);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            type = bb.getShort() & 0xffff;
            sender = bb.getShort() & 0xffff;
            len = bb.get() & 0xff;
            Log.d(TAG, String.format("%04X %04X %02X", type, sender, len));
        }

        Header(int sender_, int type_, int len_) {
            sender = sender_;
            type = type_;
            len = len_;
        }

        byte[] build() {
            ByteBuffer bb = ByteBuffer.allocate(SIZE);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putShort((short)type);
            bb.putShort((short)sender);
            bb.put((byte)len);
            return bb.array();
        }
    }

    class Callback {
        int[] msg_id;
        SBPCallback cb;

        Callback(int id, SBPCallback cb_) {
            msg_id = new int[] {id};
            cb = cb_;
        }

        Callback(int[] id, SBPCallback cb_) {
            msg_id = id;
            cb = cb_;
        }

        void dispatch(SBPMessage msg) {
            for (int i : msg_id) {
                if (i == msg.type)
                    cb.receiveCallback(msg);
            }
        }
    }
}
