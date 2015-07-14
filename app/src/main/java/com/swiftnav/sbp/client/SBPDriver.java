package com.swiftnav.sbp.client;

import java.io.IOException;

public interface SBPDriver {
    byte[] read(int len) throws IOException;
    void write(byte[] data) throws IOException;
}
