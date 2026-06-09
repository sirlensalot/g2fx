package org.g2fx.g2gui;

import org.g2fx.g2lib.usb.*;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.List;

import static org.g2fx.g2lib.DeviceTest.parseCapture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mock sender + dispatcher driver using capture files. Expects outbounds to match exactly;
 * dispatches all inbounds until next outbound.
 */
public class CaptureSender implements UsbSender {

    private final List<MessageRecorder.RecordedUsbMessage> script;
    private Dispatcher dispatcher;

    public CaptureSender(List<MessageRecorder.RecordedUsbMessage> script) {
        this.script = script;
    }

    public CaptureSender(String f) throws Exception {
        this(parseCapture(f, _ -> true));
    }

    @Override
    public int sendBulk(String msg, boolean dispatch, ByteBuffer data) throws Exception {
        MessageRecorder.RecordedUsbMessage m = script.removeFirst();
        String expected = Util.dumpBufferString(m.msg().buffer());
        String actual = Util.dumpBufferString(Usb.prepareSendBuffer(data));
        assertEquals(expected,actual);
        dispatchInbounds();
        return 0;
    }

    public void dispatchInbounds() throws Exception {
        while (!script.isEmpty() && script.getFirst().inbound()) {
            UsbMessage msg = script.removeFirst().msg();
            assertTrue(dispatcher.dispatch(msg),"Dispatch of message: " +
                    Util.dumpBufferString(msg.buffer()));
        }

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}
