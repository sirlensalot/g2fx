package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class OfflineSender implements UsbSender {

    private final Logger log = Util.getLogger(getClass());

    public static final int LENGTH_OK = 1;

    public interface OfflineSendListener {
        void onSend(boolean dispatch,ByteBuffer data);
    }

    private final OfflineSendListener listener;


    public OfflineSender() {
        listener = (a,b) -> {};
    }

    public OfflineSender(OfflineSendListener listener) {
        this.listener = listener;
    }

    @Override
    public int sendBulk(String msg, boolean dispatch, ByteBuffer data) throws Exception {
        log.info(() -> String.format("sendBulk: %s %s %s\n",
                msg, dispatch, Util.dumpBufferString(data)));
        listener.onSend(dispatch,data);
        return LENGTH_OK;
    }

    @Override
    public void setDispatcher(Dispatcher dispatcher) {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public boolean online() {
        return false;
    }
}
