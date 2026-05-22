package org.g2fx.g2lib;

import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.g2fx.g2lib.DeviceTest.parseCapture;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerformanceTest {


    @BeforeAll
    public static void beforeAll() {
        Util.configureLogging();
    }


    @Test
    void regressNewPerf() throws Exception {
        Performance p = new Performance();
        p.initNew();
        ByteBuffer pb = p.writeMessage();

        List<MessageRecorder.RecordedUsbMessage> ms =
                    parseCapture("data/capture/capture-newperf.pcapng", MessageRecorder.OUTBOUND);
        ByteBuffer b = ms.get(1).msg().buffer().position(2).slice();
        b.limit(b.limit()-2);

        //overwrite ModuleNames unknown reserved values
        b.put(0x2fa,(byte)0x40);
        b.put(0x2ff,(byte)0x00);
        b.put(0x58c,(byte)0x40);
        b.put(0x591,(byte)0x00);
        b.put(0x81e,(byte)0x40);

        assertEquals(Util.dumpBufferString(b),Util.dumpBufferString(pb));

    }
}
