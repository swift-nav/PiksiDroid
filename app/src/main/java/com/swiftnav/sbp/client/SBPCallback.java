package com.swiftnav.sbp.client;

import com.swiftnav.sbp.msg.SBPMessage;

public interface SBPCallback {
    void receiveCallback(SBPMessage msg);
}