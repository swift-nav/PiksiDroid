package com.swiftnav.sbp.msg;

public class MsgTrackingState extends SBPMessage {
    public TrackingChannelState[] states;

    public MsgTrackingState(SBPMessage msg) {
        super(msg);
        assert (msg.type == SBP_MSG_TRACKING_STATE);

        states = getTrackingChannelStates(-1);
    }

    public class TrackingChannelState {
        public static final int SIZE = 6;
        public int state;
        public int prn;
        public float cn0;

        public TrackingChannelState() {
            if (payloadBuffer == null)
                return;
            state = getU8();
            prn = getU8();
            cn0 = getFloat();
        }
    }

    public TrackingChannelState[] getTrackingChannelStates(int n) {
        if (n < 0) {
            n = payloadBuffer.remaining() / TrackingChannelState.SIZE;
        }
        TrackingChannelState[] ret = new TrackingChannelState[n];
        for (int i = 0; i < n; i++) {
            ret[i] = new TrackingChannelState();
        }
        return ret;
    }
}
