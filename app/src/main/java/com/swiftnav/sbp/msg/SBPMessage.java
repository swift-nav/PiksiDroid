package com.swiftnav.sbp.msg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by gareth on 7/13/15.
 */
public class SBPMessage {
    /* Message IDs */
    public static final int SBP_MSG_PRINT = 0x0010;
    public static final int SBP_MSG_TRACKING_STATE = 0x0016;
    public static final int SBP_MSG_OBS = 0x0043;
    public static final int SBP_MSG_BASE_POS = 0x0044;
    public static final int SBP_MSG_EPHEMERIS = 0x0047;
    public static final int SBP_MSG_POS_LLH = 0x0201;

    public final int sender;
    public final int type;
    protected byte[] payload;
    protected ByteBuffer payloadBuffer;

    public SBPMessage(SBPMessage msg) {
        sender = msg.sender;
        type = msg.type;
        payload = msg.payload;
        payloadBuffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
    }

    public SBPMessage(int sender_, int type_, byte[] payload_) {
        sender = sender_;
        type = type_;
        payload = payload_;
    }

    public SBPMessage(int type_, byte[] payload_) {
        this(0x42, type_, payload_);
    }

    public SBPMessage(int type_) {
        this(0x42, type_, new byte[0]);
    }

    public byte[] getPayload() {
        return payload;
    }


    protected int getU8() {
        return payloadBuffer.get() & 0xff;
    }

    protected int getU16() {
        return payloadBuffer.getShort() & 0xffff;
    }

    protected long getU32() {
        return payloadBuffer.getInt() & 0xffffffffl;
    }

    protected float getFloat() {
        return payloadBuffer.getFloat();
    }

    protected double getDouble() {
        return payloadBuffer.getDouble();
    }

    protected void putU8(int x) {
        payloadBuffer.put((byte) x);
    }

    protected void putU16(int x) {
        payloadBuffer.putShort((short) x);
    }

    protected void putU32(long x) {
        payloadBuffer.putInt((int) x);
    }

    protected void putFloat(float x) {
        payloadBuffer.putFloat(x);
    }

    protected void putDouble(double x) {
        payloadBuffer.putDouble(x);
    }


}
