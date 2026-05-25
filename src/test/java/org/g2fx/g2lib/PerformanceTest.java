package org.g2fx.g2lib;

import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.protocol.Protocol;
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


    public static final String PERF_001 = "data/perf/g2fx-perf-01.prf2";
    public static final String PERF_002 = "data/perf/g2fx-perf-002.prf2";

    @BeforeAll
    public static void beforeAll() {
        Util.configureLogging();
        //WARNING: Protocol subfield init can fail with tests running in multiple threads!!
        Protocol.ModuleParams.ParamSet.toString(); // force fields init
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
        overwriteBytes(b,
            0x2fa,0x40,
            0x2ff,0x00,
            0x58c,0x40,
            0x591,0x00,
            0x81e,0x40);

        assertEquals(Util.dumpBufferString(b),Util.dumpBufferString(pb));

    }


    @Test
    void roundtripPerformanceFile() throws Exception {
        String filePath = "data/perf/perf-20240802.prf2";
        Performance perf = Performance.readFromFile(filePath,new UsbSender.OfflineSender());
        assertEquals(Util.dumpBufferString(Util.readFile(filePath).rewind()),
                Util.dumpBufferString(perf.writeFile().rewind()));

    }


    @Test
    void regress001_LoadPerfSend() throws Exception {

        List<MessageRecorder.RecordedUsbMessage> ms =
                parseCapture("data/capture/capture-001-load-g2fx-perf1.pcapng", (i) -> true);

        ByteBuffer m = ms.get(2).msg().buffer().position(2).slice();
        m.limit(m.limit()-2);
//        readInboundPerf(m.buffer());
//        if (true) return;

        Performance perf = Performance.readFromFile(PERF_001,new UsbSender.OfflineSender());

        ByteBuffer bulkMsg = perf.writeMessage();

        // overwrite ModuleNames reserved values
        overwriteBytes(m,
                0x30f,0x40,
                0x6a2,0,
                0xa32,0);

        assertEquals(Util.dumpBufferString(m),Util.dumpBufferString(bulkMsg));

    }

    void overwriteBytes(ByteBuffer buf, int... addrValPairs) {
        for (int i = 0; i < addrValPairs.length; i += 2) {
            buf.put(addrValPairs[i], (byte) addrValPairs[i+1]);
        }
    }

    @Test
    void regress001_RoundtripFile() throws Exception {
        Performance perf = Performance.readFromFile(PERF_001,new UsbSender.OfflineSender());
        ByteBuffer wbuf = perf.writeFile();
        ByteBuffer buf = Util.readFile(PERF_001);
        //sigh ... here, empty ModuleParams have variation count :(
        overwriteBytes(buf,
            0x237,0,0x238,0,0x23d,0,0x23e,0,
            0x58f,0,0x590,0,0x595,0,0x596,0,
            0x8f4,0,0x8f5,0,0x8fa,0,0x8fb,0,
            0xc7d,0,0xc7e,0,0xc83,0,0xc84,0
        );
        //have to skip CRC for overwrites
        assertEquals(Util.dumpBufferString(buf.limit(buf.limit()-2)),
                Util.dumpBufferString(wbuf.limit(wbuf.limit()-2)));
    }


    @Test
    void regress002_LoadPerfSend() throws Exception {

        List<MessageRecorder.RecordedUsbMessage> ms =
                parseCapture("data/capture/capture-002-load-g2fx-perf2.pcapng", (i) -> true);

        ByteBuffer m = ms.get(2).msg().buffer().position(2).slice();
        m.limit(m.limit()-2);

        Performance perf = Performance.readFromFile(PERF_002,new UsbSender.OfflineSender());

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


    @Test
    void regress002_RoundtripFile() throws Exception {
        Performance perf = Performance.readFromFile(PERF_002,new UsbSender.OfflineSender());
        ByteBuffer wbuf = perf.writeFile();
        ByteBuffer buf = Util.readFile(PERF_002);
        //module names reserved values
        overwriteBytes(buf,0x2aae,0x40);
        //have to skip CRC for overwrites
        assertEquals(Util.dumpBufferString(buf.limit(buf.limit()-2)),
                Util.dumpBufferString(wbuf.limit(wbuf.limit()-2)));
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
