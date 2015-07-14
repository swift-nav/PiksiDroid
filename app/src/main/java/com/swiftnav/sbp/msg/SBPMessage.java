package com.swiftnav.sbp.msg;

/**
 * Created by gareth on 7/13/15.
 */
public class SBPMessage {
    /* Message IDs */
    public static final int SBP_MSG_PRINT = 0x0010;

    public final int sender;
    public final int type;
    protected byte[] payload;

    public SBPMessage(SBPMessage msg) {
        sender = msg.sender;
        type = msg.type;
        payload = msg.payload;
    }

    public SBPMessage(int sender_, int type_, byte[] payload_) {
        sender = sender_;
        type = type_;
        payload = payload_;
    }

    public SBPMessage(int type_, byte[] payload_) {
        this(0x42, type_, payload_);
    }

    public byte[] getPayload() {
        return payload;
    }
}
