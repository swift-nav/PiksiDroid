package com.swiftnav.sbp.client;

public interface SBPDriver {
    public byte[] read(int len);
    public void write(byte[] data);
}
