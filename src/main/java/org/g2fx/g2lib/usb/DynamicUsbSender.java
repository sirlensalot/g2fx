package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.device.DeviceListener;

import java.nio.ByteBuffer;

/**
 * "Data object facing" UsbSender, that is only offering send functionality;
 * allows re-using sender when on- and offline. Listens for device update
 * to switch.
 */
public class DynamicUsbSender implements UsbSender, DeviceListener {

    private Device device;
    private final UsbSender offlineSender = new OfflineSender();
    private UsbSender sender;

    public DynamicUsbSender() {
        sender = offlineSender;
    }

    @Override
    public void onDeviceInitialized(Device d) throws Exception {
        device = d;
        sender = device.getUsb();
    }

    @Override
    public void onDeviceDisposal(Device d) throws Exception {
        d = null;
        sender = offlineSender;
    }

    @Override
    public int sendBulk(String msg, boolean dispatch, ByteBuffer data) throws Exception {
        return sender.sendBulk(msg,dispatch,data);
    }

    @Override
    public void shutdown() {}

    @Override
    public void setDispatcher(Dispatcher dispatcher) {
        //not a dispatcher, so noop (online devices are created with online-only senders)
    }

    @Override
    public boolean online() {
        return sender.online();
    }
}
