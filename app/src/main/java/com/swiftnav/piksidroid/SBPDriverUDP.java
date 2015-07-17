package com.swiftnav.piksidroid;

import android.util.Log;

import com.swiftnav.sbp.client.SBPDriver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SBPDriverUDP implements SBPDriver {
    static final String TAG = "SBPDriverUDP";
    static final int RECV_SIZE = 65535;
    private DatagramSocket socket;
    private String server;
    private int port;
    byte[] rxdata;

    public SBPDriverUDP(String server_, int port_) {
        server = server_;
        rxdata = new byte[0];
        port = port_;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    openSocket();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public byte[] read(int len) throws IOException {
        if (socket == null)
            openSocket();

        while (rxdata.length < len) {
            DatagramPacket packet = new DatagramPacket(new byte[RECV_SIZE], RECV_SIZE);
            socket.receive(packet);
            ByteBuffer bb = ByteBuffer.allocate(rxdata.length + packet.getLength());
            bb.put(rxdata);
            bb.put(packet.getData(), 0, packet.getLength());
            rxdata = bb.array();
        }
        byte[] ret = Arrays.copyOf(rxdata, len);
        rxdata = Arrays.copyOfRange(rxdata, len, rxdata.length);
        return ret;
    }

    @Override
    public void write(byte[] data) throws IOException {
        if (socket == null)
            openSocket();

        DatagramPacket packet = new DatagramPacket(data, data.length);
        socket.send(packet);
    }

    private void openSocket() throws IOException {
        try {
            socket = new DatagramSocket();
            socket.connect(InetAddress.getByName(server), port);
            socket.send(new DatagramPacket(new byte[]{1, 2, 3}, 3));
            Log.d(TAG, "Sent junk");
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup socket");
            e.printStackTrace();
        }
    }
}
