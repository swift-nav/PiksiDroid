package com.swiftnav.sbp.client;

import java.io.IOException;

public interface SBPDriver {
    public byte[] read(int len);
    public void write(byte[] data) throws IOException;
}
