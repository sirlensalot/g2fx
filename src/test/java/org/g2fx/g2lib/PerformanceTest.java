package org.g2fx.g2lib;

import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.usb.UsbSender;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
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

        //overwrite ModuleNames reserved values
        b.put(0x2fa,(byte)0x40);
        b.put(0x2ff,(byte)0x00);
        b.put(0x58c,(byte)0x40);
        b.put(0x591,(byte)0x00);
        b.put(0x81e,(byte)0x40);

        assertEquals(Util.dumpBufferString(b),Util.dumpBufferString(pb));

    }


    @Test
    void regress001_LoadPerfSend() throws Exception {

        List<MessageRecorder.RecordedUsbMessage> ms =
                parseCapture("data/capture/capture-001-load-g2fx-perf1.pcapng", (i) -> true);

        ByteBuffer m = ms.get(2).msg().buffer().position(2).slice();
        m.limit(m.limit()-2);
//        readInboundPerf(m.buffer());
//        if (true) return;

        Performance perf = Performance.readFromFile("data/perf/g2fx-perf-01.prf2",new UsbSender.OfflineSender());
        perf.setFileName("g2fx-perf-01");
        ByteBuffer bulkMsg = perf.writeMessage();

        // overwrite ModuleNames reserved values
        m.put(0x30f,(byte)0x40);
        m.put(0x6a2,(byte)0);
        m.put(0xa32,(byte)0);

        assertEquals(Util.dumpBufferString(m),Util.dumpBufferString(bulkMsg));

    }


    @Test
    void regress002_LoadPerfSend() throws Exception {

        List<MessageRecorder.RecordedUsbMessage> ms =
                parseCapture("data/capture/capture-002-load-g2fx-perf2.pcapng", (i) -> true);

        ByteBuffer m = ms.get(2).msg().buffer().position(2).slice();
        m.limit(m.limit()-2);

//        readOutboundPerf(m);
//        if (true) return;

        Performance perf = Performance.readFromFile("data/perf/g2fx-perf-002.prf2",new UsbSender.OfflineSender());
        perf.setFileName("g2fx-perf-002"); //TODO!!
//        perf.dumpYaml("data/perf/g2fx-perf-002.yaml");
        ByteBuffer bulkMsg = perf.writeMessage();

        /*
        image breadcrumb:
        03fd Patch A => 4d VA, 0ba0 4d FX, 0cb0 65 0122,
        0dd5 62 011e, 0ef6 60, 0f21 5b Morph, 0f78 5b VA
        0f89 5b Fx, 5a VA 133, 10c4 5a
        10f8 Patch B => 1100 4a, 1160 4a, 69
        1170 52, 11b0 52, 11c0 4d 165, 1328 4d 362, 168c 4d
        1693 65, 1726 62, 17aa 60, 17d6 5b morph,
        182c 5b VA, 185a 5b+5a
        */

        assertEquals(Util.dumpBufferString(m),Util.dumpBufferString(bulkMsg));

    }


    /**
     * outbound are not intended for app dispatch, call performance/slot read methods to adapt
     * to get logging. Enable INFO for Patch,Fields,Sections.
     */
    public static void readOutboundPerf(ByteBuffer buf) {
        //System.out.println(Util.dumpBufferString(buf));
        Performance p = new Performance();
        ByteBuffer b = buf.position(0x18);
        p.readPerformanceNameAndSettings(b);
        p.slots().forEach(s -> {
            s.readFileSections(b);
            Arrays.stream(AreaId.USER_AREAS).forEach(a ->
                s.getArea(a).getModules().forEach(m -> {
                    if (m.getValues() == null) { return; }
                    List<Integer> v9s = m.getValues().getVarValues(9);
                    List<Integer> defs = m.getUserModuleData().getType().getParams().stream().map(NamedParam::def).toList();
                    if (!v9s.equals(defs)) {
                        System.out.printf("%s: OUT  V9: %s\n", m.getUserModuleData().getType(), v9s);
                        System.out.printf("%s: DEFAULT: %s\n", m.getUserModuleData().getType(), defs);
                    }
            }));
        });
    }
}
