package org.g2fx.g2lib;

import org.g2fx.g2gui.CaptureSender;
import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.model.CableDelta;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.protocol.Codes;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.state.*;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.usb.OfflineSender;
import org.g2fx.g2lib.usb.UsbMessage;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.g2fx.g2lib.usb.MessageRecorder.parseCapture;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerformanceTest {


    public static final String PERF_001 = "data/perf/g2fx-perf-01.prf2";
    public static final String PERF_002 = "data/perf/g2fx-perf-002.prf2";
    public static final String PATCH_UPRATE_4MOD = "data/patch/g2fx-uprate-4mod.pch2";

    @BeforeAll
    public static void beforeAll() {
        Util.configureLogging();
        //WARNING: Protocol subfield init can fail with tests running in multiple threads!!
        Protocol.ModuleParams.ParamSet.toString(); // force fields init
    }


    public static ByteBuffer dropCrcTrailer(ByteBuffer buf) {
        return buf.limit(buf.limit() - 2);
    }


    /**
     * outbound are not intended for app dispatch, call performance/slot read methods to adapt
     * to get logging. Enable INFO for Patch,Fields,Sections.
     */
    public static void readOutboundPerf(ByteBuffer buf) {
        //System.out.println(Util.dumpBufferString(buf));
        Performance p = new Performance(new OfflineSender());
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


    @Test
    void regressNewPerf() throws Exception {
        AtomicReference<ByteBuffer> pbuf = new AtomicReference<>();
        Performance p = new Performance(getPerfCreateListener(pbuf));
        p.initNew();
        p.sendPerf();

        List<MessageRecorder.RecordedUsbMessage> ms =
                    parseCapture("data/capture/capture-newperf.pcapng", MessageRecorder.OUTBOUND);
        ByteBuffer b = ms.get(1).msg().buffer().position(2).slice();
        dropCrcTrailer(b);

        //overwrite ModuleNames reserved values
        overwriteBytes(b,
            0x2fa,0x40,
            0x2ff,0x00,
            0x58c,0x40,
            0x591,0x00,
            0x81e,0x40);

        assertEquals(Util.dumpBufferString(b),Util.dumpBufferString(pbuf.get()));

    }

    private static OfflineSender getPerfCreateListener(AtomicReference<ByteBuffer> pbuf) {
        return new OfflineSender((d, b) -> {
            if (b.get(3) == Codes.O_CREATE) pbuf.set(b);
        });
    }


    @Test
    void roundtripPerformanceFile() throws Exception {
        String filePath = "data/perf/perf-20240802.prf2";
        Performance perf = Performance.readFromFile(filePath,new OfflineSender());
        ByteBuffer fbuf = Util.readFile(filePath).rewind();

        // B description has different Unk2 (3 bits)
        // 0 0 0 0 0 0 0 0 5 176 >1 1 1 1 1 1< 1 1 0 1 0 0
        // 0 0 0 0 0 0 0 0 5 176 >2 1 1 1 1 1< 1 1 0 1 0 0
        overwriteBytes(fbuf,
                //       2..11111
                0x181b,0b01011111);

        assertEquals(Util.dumpBufferString(dropCrcTrailer(fbuf)),
                Util.dumpBufferString(dropCrcTrailer(perf.writeFile())));

    }


    @Test
    void regress001_LoadPerfSend() throws Exception {

        UsbMessage msg = get001PerfLoadMsg();
        ByteBuffer m = msg.buffer().position(2).slice();
        dropCrcTrailer(m);

        AtomicReference<ByteBuffer> pbuf = new AtomicReference<>();
        Performance perf = Performance.readFromFile(PERF_001,getPerfCreateListener(pbuf));
        perf.sendPerf();

        // overwrite ModuleNames reserved values
        overwriteBytes(m,
                0x30f,0x40,
                0x6a2,0,
                0xa32,0);

        assertEquals(Util.dumpBufferString(m),Util.dumpBufferString(pbuf.get()));

    }

    public static UsbMessage get001PerfLoadMsg() throws Exception {
        List<MessageRecorder.RecordedUsbMessage> ms =
                parseCapture("data/capture/capture-001-load-g2fx-perf1.pcapng", (i) -> true);
        UsbMessage msg = ms.get(2).msg();
        return msg;
    }

    public static void overwriteBytes(ByteBuffer buf, int... addrValPairs) {
        for (int i = 0; i < addrValPairs.length; i += 2) {
            buf.put(addrValPairs[i], (byte) addrValPairs[i+1]);
        }
    }

    @Test
    void regress001_RoundtripFile() throws Exception {
        Performance perf = Performance.readFromFile(PERF_001,new OfflineSender());
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
        assertEquals(Util.dumpBufferString(dropCrcTrailer(buf)),
                Util.dumpBufferString(dropCrcTrailer(wbuf)));
    }


    @Test
    void regress002_LoadPerfSend() throws Exception {

        List<MessageRecorder.RecordedUsbMessage> ms =
                parseCapture("data/capture/capture-002-load-g2fx-perf2.pcapng", (i) -> true);

        ByteBuffer m = ms.get(2).msg().buffer().position(2).slice();
        dropCrcTrailer(m);

        AtomicReference<ByteBuffer> pbuf = new AtomicReference<>();
        Performance perf = Performance.readFromFile(PERF_002,getPerfCreateListener(pbuf));
        perf.sendPerf();

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
        overwriteBytes(m,
        //0 0 0 0 0 0 0 0 17 1ef 1 0 0 1 1 1 1 1 0 1 b 0
        //0 0 0 0 0 0 0 0 17 1ef 2 0 0 1 1 1 1 1 0 1 b 0
                //       2--
                0x0094,0b01000111,
        //0 0 0 0 0 0 0 0 1 258 0 1 1 1 1 1 1 1 1 6 3 0
        //0 0 0 0 0 0 0 0 1 258 2 1 1 1 1 1 1 1 1 6 3 0
                //       2--
                0x1105,0b01011111
        );
        assertEquals(Util.dumpBufferString(m),Util.dumpBufferString(pbuf.get()));

    }


    @Test
    void regress002_RoundtripFile() throws Exception {
        Performance perf = Performance.readFromFile(PERF_002,new OfflineSender());
        ByteBuffer wbuf = perf.writeFile();
        ByteBuffer buf = Util.readFile(PERF_002);
        //module names reserved values
        overwriteBytes(buf,0x2aae,0x40,
                //patch description unk3
                0x00c7,0b01000111,
                0x1033,0b01011111
        );
        //have to skip CRC for overwrites
        assertEquals(Util.dumpBufferString(dropCrcTrailer(buf)),
                Util.dumpBufferString(dropCrcTrailer(wbuf)));
    }

    @Test
    void loadPatch008() throws Exception {
        CaptureSender sender = new CaptureSender("data/capture/capture-008-loadfile-patch-g2fx-uprate-4mod.pcapng");
        Device d = new Device(sender, LifecycleListener.noopListener(), LifecycleListener.noopListener());
        Performance p = new Performance(sender);
        d.setPerf(p);
        for (Patch slot : p.slots()) {
            slot.setVersion(2);
        }
        p.readPatchFromFile(Slot.A, PATCH_UPRATE_4MOD);
        sender.assertScriptDone();
    }

    @Test
    void doCables011() throws Exception {
        CaptureSender sender = new CaptureSender("data/capture/capture-011-cables1-g2fx-uprate-4mod.pcapng");
        Device d = new Device(sender, LifecycleListener.noopListener(), LifecycleListener.noopListener());
        Performance p = new Performance(sender);
        d.setPerf(p);
        sender.setStrict(false);
        p.readPatchFromFile(Slot.A, PATCH_UPRATE_4MOD);
        p.getSlot(Slot.A).setVersion(3);
        p.getSlot(Slot.B).setVersion(2);
        p.getSlot(Slot.C).setVersion(2);
        p.getSlot(Slot.D).setVersion(2);
        sender.setStrict(true);
        p.getSlot(Slot.A).getArea(AreaId.Voice).execCableDelta(new CableDelta<>(
            List.of(new CableDelta.CableIndex(4,0,-1,2,0,-1,-1)),
                false,
                Map.of(2,false),
                Map.of()
        ));
        p.serviceLoadResponses(true);
        p.getSlot(Slot.A).getArea(AreaId.Voice).execCableDelta(new CableDelta<>(
                List.of(new CableDelta.CableIndex(1,0,-1,2,1,-1,-1)),
                false,
                Map.of(),
                Map.of()
        ));
        p.getSlot(Slot.A).getArea(AreaId.Voice).sendAreaResourcesRequest(); //TODO why do only some of these send resource reqs?
        p.serviceLoadResponses(true);
        p.getSlot(Slot.A).getArea(AreaId.Voice).execCableDelta(new CableDelta<>(
                List.of(new CableDelta.CableIndex(1,0,1,2,1,0,1)), //TODO check against gui code
                true,
                Map.of(),
                Map.of()
        ));
        p.getSlot(Slot.A).getArea(AreaId.Voice).sendAreaResourcesRequest(); //TODO why do only some of these send resource reqs?
        p.serviceLoadResponses(true);
        p.getSlot(Slot.A).getArea(AreaId.Voice).execCableDelta(new CableDelta<>(
                List.of(new CableDelta.CableIndex(4,0,1,2,0,0,0)), //TODO check against gui code
                true,
                Map.of(2,true),
                Map.of()
        ));
        p.serviceLoadResponses(true);
        sender.assertScriptDone();
    }

}
