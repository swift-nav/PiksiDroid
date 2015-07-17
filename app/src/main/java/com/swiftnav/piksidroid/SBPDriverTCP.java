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
    private Semaphore sem;

    public SBPDriverTCP(String server_, int port_) {
        server = server_;
        port = port_;
        sem = new Semaphore(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sem.acquire();
                    openSocket();
                    sem.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public byte[] read(int len) throws IOException {

        try {
            sem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] ret = new byte[len];
        int i = 0;
        while (i < len) {
            try {
                if (!socket.isConnected()) {
                    Log.e(TAG, "Socket not connected");
                }
                int count = socket.getInputStream().read(ret, i, len - i);
                i += count;
            } catch (Exception e) {
                Log.e(TAG, "Error reading from socket: " + e.toString());
                e.printStackTrace();
            }

        }
        sem.release();
        return ret;
    }

    @Override
    public void write(byte[] data) throws IOException {
        try {
            sem.acquire();
            if (!socket.isConnected()) {
                Log.e(TAG, "Socket not connected");
            }
            socket.getOutputStream().write(data);
            sem.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void openSocket() throws IOException {
        try {
            socket = new Socket();
            SocketAddress addr = new InetSocketAddress(InetAddress.getByName(server), port);
            socket.connect(addr, port);
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup socket");
            e.printStackTrace();
        }
    }

}
