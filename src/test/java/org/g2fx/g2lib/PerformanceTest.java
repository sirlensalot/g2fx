package org.g2fx.g2lib;

import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.usb.UsbSender;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.g2fx.g2lib.DeviceTest.parseCapture;

public class PerformanceTest {

    @Test
    void regressNewPerf() throws Exception {
        Performance p = new Performance(new UsbSender.OfflineSender());
        p.initNew();
        ByteBuffer pb = p.writeMessage();
        List<MessageRecorder.RecordedUsbMessage> ms =
                    parseCapture("data/capture/capture-newperf.pcapng", MessageRecorder.OUTBOUND);
        ByteBuffer b = ms.get(1).msg().buffer();


    }
}
