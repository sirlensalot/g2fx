package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.state.Slot;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;

import static org.g2fx.g2lib.protocol.Codes.*;

public interface UsbSender {

    default int sendBulk(String msg, boolean dispatch, byte[] data) throws Exception {
        return sendBulk(msg,dispatch,ByteBuffer.wrap(data));
    }

    int sendBulk(String msg, boolean dispatch, ByteBuffer data) throws Exception;

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

    default int sendStartStopComm(boolean start) throws Exception {
        return sendSystemRequest(start ? "Start comm" : "Stop comm"
                , O_START_STOP_COM
                , start ? 0 : 1
        );
    }

    void shutdown();

    void setDispatcher(Dispatcher dispatcher);

    default boolean online() { return true; }

}
