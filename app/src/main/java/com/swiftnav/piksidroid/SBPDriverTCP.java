package com.swiftnav.piksidroid;

import android.util.Log;

import com.swiftnav.sbp.client.SBPDriver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

/**
 * Created by gareth on 7/17/15.
 */
public class SBPDriverTCP implements SBPDriver {
    static final String TAG = "SBPDriverTCP";
    private Socket socket;
    private String server;
    private int port;

    public SBPDriverTCP(String server_, int port_) {
        server = server_;
        port = port_;
    }

    @Override
    public byte[] read(int len) throws IOException {
        byte[] ret = new byte[len];
        int i = 0;

        synchronized (this) {
            if (socket == null)
                openSocket();
        }

        while (i < len) {
            try {
                if (!socket.isConnected()) {
                    Log.e(TAG, "Socket not connected");
                }
                int count = socket.getInputStream().read(ret, i, len - i);
                if (count < 0) {
                    throw new IOException("socket.read() returned negative");
                }
                i += count;
            } catch (Exception e) {
                Log.e(TAG, "Error reading from socket: " + e.toString());
                e.printStackTrace();
                throw e;
            }

        }
        return ret;
    }

    @Override
    public void write(byte[] data) throws IOException {
        synchronized (this) {
            if (socket == null)
                openSocket();
        }

        if (!socket.isConnected()) {
            Log.e(TAG, "Socket not connected");
            throw new IOException("socket not connected");
        }

        socket.getOutputStream().write(data);
    }

    private void openSocket() throws IOException {
        socket = new Socket();
        SocketAddress addr = new InetSocketAddress(InetAddress.getByName(server), port);
        socket.connect(addr, port);
    }
}
