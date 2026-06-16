package org.g2fx.g2gui;

import org.g2fx.g2lib.usb.*;
import org.g2fx.g2lib.util.Util;
import org.opentest4j.AssertionFailedError;

import java.nio.ByteBuffer;
import java.util.ArrayList;
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
    private final List<AssertionFailedError> errors = new ArrayList<>();
    /**
     * whether to match outbounds to script
     */
    private boolean strict = true;

    public CaptureSender(List<MessageRecorder.RecordedUsbMessage> script) {
        this.script = new ArrayList<>(script);
    }

    public CaptureSender(String f) throws Exception {
        this(parseCapture(f, _ -> true));
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public void setScript(String f) throws Exception {
        script.clear();
        script.addAll(parseCapture(f,_->true));
    }

    @Override
    public int sendBulk(String msg, boolean dispatch, ByteBuffer data) throws Exception {
        if (!strict) { return 0; }
        try {
            MessageRecorder.RecordedUsbMessage m = script.removeFirst();
            String expected = Util.dumpBufferString(m.msg().buffer());
            String actual = Util.dumpBufferString(Usb.prepareSendBuffer(data));
            assertEquals(expected, actual);
            dispatchInbounds();
        } catch (AssertionFailedError e) {
            System.err.println("sendBulk: trapped assertion error: " + e);
            errors.add(e);
        }
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

    public List<MessageRecorder.RecordedUsbMessage> getScript() {
        return script;
    }

    public void throwErrors() {
        for (AssertionFailedError e : errors) {
            throw e;
        }
    }
}
