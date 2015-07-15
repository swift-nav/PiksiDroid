package com.swiftnav.sbp.msg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by gareth on 7/14/15.
 */
public class MsgPosLLH extends SBPMessage {
    public static final int SIZE = 34;
    public long tow;
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

        tow = getU32();
        lat = getDouble();
        lon = getDouble();
        height = getDouble();
        h_accuracy = getU16();
        v_accuracy = getU16();
        n_sats = getU8();
        flags = getU8();
    }

    @Override
    public byte[] getPayload() {
        payloadBuffer = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN);

        putU32(tow);
        putDouble(lat);
        putDouble(lon);
        putDouble(height);
        putU16(h_accuracy);
        putU16(v_accuracy);
        putU8(n_sats);
        putU8(flags);
        payload = payloadBuffer.array();
        return payload;
    }
}
