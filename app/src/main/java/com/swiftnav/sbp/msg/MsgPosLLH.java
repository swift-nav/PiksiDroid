package com.swiftnav.sbp.msg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by gareth on 7/14/15.
 */
public class MsgPosLLH extends SBPMessage {
    public static final int SIZE = 34;
    public int tow;
    public double lat;
    public double lon;
    public double height;
    public int h_accuracy;
    public int v_accuracy;
    public int n_sats;
    public int flags;

    public MsgPosLLH() {
        super(SBP_MSG_POS_LLH);
    }

    public MsgPosLLH(SBPMessage msg) {
        super(msg);
        assert (msg.type == SBP_MSG_POS_LLH);
        assert (msg.payload.length == SIZE);

        ByteBuffer bb = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        tow = bb.getInt();
        lat = bb.getDouble();
        lon = bb.getDouble();
        height = bb.getDouble();
        h_accuracy = bb.getShort() & 0xffff;
        v_accuracy = bb.getShort() & 0xffff;
        n_sats = bb.get() & 0xff;
        flags = bb.get() & 0xff;
    }

    @Override
    public byte[] getPayload() {
        ByteBuffer bb = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN);

        bb.putInt(tow);
        bb.putDouble(lat);
        bb.putDouble(lon);
        bb.putDouble(height);
        bb.putShort((short) h_accuracy);
        bb.putShort((short) v_accuracy);
        bb.put((byte) n_sats);
        bb.put((byte)flags);
        payload = bb.array();
        return payload;
    }
}
