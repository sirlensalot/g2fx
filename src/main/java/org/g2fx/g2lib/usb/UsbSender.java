package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.state.Slot;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import static org.g2fx.g2lib.protocol.Codes.*;

public interface UsbSender {

    int sendBulk(String msg, boolean dispatch, byte[] data) throws Exception;

    default int sendSystemRequest(String msg, int... cdata) throws Exception {
        return sendBulk(msg, true, Util.concat(Util.asBytes(
                M_CMD,
                S_PERF_REQ,
                V_SYSTEM
        ),Util.asBytes(cdata)));
    }

    default int sendPerfRequest(int perfVersion, String msg, int... cdata) throws Exception {
        return sendBulk(msg, true, Util.concat(Util.asBytes(
                M_CMD,
                S_PERF_REQ,
                perfVersion
        ),Util.asBytes(cdata)));
    }

    default int sendSlotRequest(Slot slot, int version, String msg, int... cdata) throws Exception {
        return sendBulk(msg, true, Util.concat(Util.asBytes(
                M_CMD,
                S_SLOT_REQ + slot.ordinal(),
                version
        ),Util.asBytes(cdata)));
    }

    void shutdown();

    void setDispatcher(Dispatcher dispatcher);

    default boolean online() { return true; }

    class OfflineSender implements UsbSender {

        private final Logger log = Util.getLogger(getClass());

        public static final int LENGTH_OK = 1;

        @Override
        public int sendBulk(String msg, boolean dispatch, byte[] data) throws Exception {
            log.info(String.format("sendBulk: %s %s %s\n",
                    msg,dispatch, Util.dumpBufferString(ByteBuffer.wrap(data))));
            return LENGTH_OK;
        }

        @Override
        public void setDispatcher(Dispatcher dispatcher) {}

        @Override
        public void shutdown() {}

        @Override
        public boolean online() {
            return false;
        }
    }

}
