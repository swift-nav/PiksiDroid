package com.swiftnav.piksidroid;


import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPDriver;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.msg.SBPMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * A simple {@link Fragment} subclass.
 */
public class ObservationFragment extends Fragment {
    static final String TAG = "ObservationFragment";
    static final int[] OBS_MESSAGE_LIST = {
            SBPMessage.SBP_MSG_BASE_POS,
            SBPMessage.SBP_MSG_OBS,
            SBPMessage.SBP_MSG_EPHEMERIS,
            SBPMessage.SBP_MSG_PRINT};
    View view;
    Button obs_button;
    EditText obs_address;

    UDPDriver driver;
    SBPHandler piksiHandler;
    SBPHandler handler;

    public ObservationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_observation, container, false);

        piksiHandler = ((MainActivity)getActivity()).handler;
        obs_button = (Button) view.findViewById(R.id.obs_connect);
        obs_address = (EditText) view.findViewById(R.id.obs_address);
        obs_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                driver = new UDPDriver(obs_address.getText().toString());
                handler = new SBPHandler(driver);
                handler.add_callback(OBS_MESSAGE_LIST, new SBPCallback() {
                    @Override
                    public void receiveCallback(SBPMessage msg) {
                        try {
                            piksiHandler.send(msg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        });

        return view;
    }

    public void connectPiksi(SBPHandler handler_) {
        piksiHandler = handler_;
        piksiHandler.add_callback(OBS_MESSAGE_LIST, new SBPCallback() {
            @Override
            public void receiveCallback(SBPMessage msg) {
                if (handler != null)
                    try {
                        handler.send(msg);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to send observation on UDP: " + e.toString());
                        e.printStackTrace();
                    }
            }
        });
    }

    class UDPDriver implements SBPDriver {
        static final int RECV_SIZE = 256;
        static final int DGRAM_PORT = 2000;
        DatagramSocket socket;
        String server;
        byte[] rxdata;

        public UDPDriver(String server_) {
            server = server_;
            rxdata = new byte[0];
        }

        @Override
        public byte[] read(int len) throws IOException {
            if (socket == null)
                openSocket();

            while (rxdata.length < len) {
                DatagramPacket packet = new DatagramPacket(new byte[RECV_SIZE], RECV_SIZE);
                socket.receive(packet);
                ByteBuffer bb = ByteBuffer.wrap(new byte[rxdata.length + packet.getLength()]);
                bb.put(rxdata);
                bb.put(packet.getData(), rxdata.length, packet.getLength());
                rxdata = bb.array();
            }
            byte[] ret = Arrays.copyOf(rxdata, len);
            rxdata = Arrays.copyOfRange(rxdata, len, rxdata.length - len);
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
                socket.connect(InetAddress.getByName(server), DGRAM_PORT);
            } catch (Exception e) {
                Log.e(TAG, "Failed to setup socket");
                e.printStackTrace();
            }
        }
    }
}
