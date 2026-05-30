package org.g2fx.g2lib.device;

public interface DeviceListener {
    void onDeviceInitialized(Device d) throws Exception;

    void onDeviceDisposal(Device d) throws Exception;
}
