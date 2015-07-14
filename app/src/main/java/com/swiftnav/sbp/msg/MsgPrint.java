package com.swiftnav.sbp.msg;

import java.io.UnsupportedEncodingException;

/**
 * Created by gareth on 7/14/15.
 */
public class MsgPrint extends SBPMessage {

    public String text;

    public MsgPrint(SBPMessage msg) throws Exception {
        super(msg);
        assert (msg.type == SBP_MSG_PRINT);
        text = new String(msg.payload, "UTF-8");
    }

    public MsgPrint(String text) {
        super(SBP_MSG_PRINT, text.getBytes());
    }
}
