package org.g2fx.g2lib;

import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.protocol.Codes;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.state.*;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.usb.UsbMessage;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Predicate;

import static org.g2fx.g2lib.PerformanceTest.dropCrcTrailer;
import static org.g2fx.g2lib.PerformanceTest.overwriteBytes;
import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    public static final String CAP_OO4_POWERON = "data/capture/capture-004-poweron-init-save.pcapng";

    @BeforeAll
    public static void beforeAll() {
        Util.configureLogging();
    }



    @Test
    void testPerfNameSettings() throws Exception {
        String data = """
                01 0c 07 29 70 65 72 66 2d 32 30 32 34 30 38 30
                32 2d 73 63 11 00 58 00 04 00 78 00 00 00 00 4e
                6f 20 6e 61 6d 65 00 01 01 00 00 00 00 7f 02 00
                00 73 69 6d 70 6c 65 20 73 79 6e 74 68 20 30 30
                31 01 01 00 00 00 00 7f 03 00 00 4e 6f 20 6e 61
                6d 65 00 01 00 00 00 00 00 7f 04 00 00 4e 6f 20
                6e 61 6d 65 00 01 00 00 00 00 00 7f 05 00 00 ff
                63                                            \s""";
        ByteBuffer buf = Util.readTextColsByteBuffer(data);
        Device d = new Device();
        d.getPerf().setVersion(7);
        assertTrue(d.dispatch(new UsbMessage(buf.limit(),true,0xff63,buf)));
        assertEquals("perf-20240802-sc",d.getPerf().perfName().get());
        //WARNING: dispatchCmd: unrecognized perf or sys version: 7 <- gotta figure this out.
    }

    @Test
    void testPatchDescrMsg() throws Exception {
        String data = """
                01 01 01 21 00 0f 00 ec 00 00 01 00 00 21 81 76
                3f c0 10 00 00 13 a9                          \s""";
        ByteBuffer buf = Util.readTextColsByteBuffer(data);
        Device d = new Device();
        d.getPerf().setVersion(7);
        assertTrue(d.dispatch(new UsbMessage(buf.limit(),true,0x13a9,buf)));
        PatchSettings ps = d.getPerf().getSlot(Slot.B).getPatchSettings();
        //just testing for nonzero values
        assertEquals(6,ps.voices().get());
    }

    public static List<MessageRecorder.RecordedUsbMessage> parseCapture(String file, Predicate<Byte> epPred)
            throws Exception {
        ByteBuffer bb = Util.readFile(file);
        List<Util.UsbPacket> ps = Util.readPcapNg(bb);
        List<MessageRecorder.RecordedUsbMessage> ms = MessageRecorder.readCapture(ps, epPred);
        return ms;
    }

    /**
     * Legacy editor requests AFTER sending in a new blank perf result in inbounds that are here used
     * to regress dispatch and values coming from data objects in Performance,Patch etc.
     * {@link PerformanceTest#regressNewPerf()} regresses perf initialization and the outbound.
     */
    @Test
    void regressNewPerf() throws Exception {

        //dispatch inbound messages
        Device d = dispatchMsgs(parseCapture("data/capture/capture-newperf.pcapng", MessageRecorder.INBOUND));
        Performance p = d.getPerf();

        //regress resulting values
        //versions
        assertEquals(4,p.getVersion());
        assertEquals(4,p.getSlot(Slot.A).getVersion());
        assertEquals(4,p.getSlot(Slot.B).getVersion());
        assertEquals(4,p.getSlot(Slot.C).getVersion());
        assertEquals(4,p.getSlot(Slot.D).getVersion());

        //perf settings
        assertEquals("Empty perf",p.getName());
        assertEquals(Slot.A,p.getSelectedSlot());
        PerformanceSettings perfSettings = p.getPerfSettings();
        assertFalse(perfSettings.keyboardRangeEnabled().get());
        assertEquals(120,perfSettings.masterClock().get());
        assertFalse(perfSettings.masterClockRun().get());

        // slot settings and patch
        for (Slot s : Slot.values()) {

            SlotSettings ss = perfSettings.getSlotSettings(s);
            Patch patch = p.getSlot(s);
            String n = s.toString();
            assertTrue(ss.enabled().get(),n);
            assertEquals(s==Slot.A,ss.keyboard().get(),n);//only A ena
            assertEquals("No name",ss.patchName().get(),n);
            assertFalse(ss.hold().get(),n);
            assertEquals(0,ss.bankIndex().get(),n);
            assertEquals(0,ss.patchIndex().get(),n);
            assertEquals(0,ss.keyboardRangeFrom().get(),n);
            assertEquals(127,ss.keyboardRangeTo().get(),n);

            // WTF legacy editor doesn't request params,labels,selparam for slot D after new perf init.
            if (s != Slot.D) {
                // patch "settings module" settings
                PatchArea psa = patch.getSettingsArea();
                for (int v = 0; v < 9; v++) {
                    String vn = n + ":" + v;
                    PatchModule m = psa.getSettingsModule(SettingsModules.Misc);
                    assertEquals(1, m.getSettingsValueProperty(ModParam.MiscSustain, v).get(), vn);
                    assertEquals(2, m.getSettingsValueProperty(ModParam.MiscOctShift, v).get(), vn);
                    m = psa.getSettingsModule(SettingsModules.Gain);
                    assertEquals(100, m.getSettingsValueProperty(ModParam.GainVolume, v).get(), vn);
                    assertEquals(1, m.getSettingsValueProperty(ModParam.GainActiveMuted, v).get(), vn);
                    m = psa.getSettingsModule(SettingsModules.Glide);
                    assertEquals(0, m.getSettingsValueProperty(ModParam.GlideControl, v).get(), vn);
                    assertEquals(28, m.getSettingsValueProperty(ModParam.GlideSpeed, v).get(), vn);
                    m = psa.getSettingsModule(SettingsModules.Bend);
                    assertEquals(1, m.getSettingsValueProperty(ModParam.BendEnable, v).get(), vn);
                    assertEquals(1, m.getSettingsValueProperty(ModParam.BendSemi, v).get(), vn);
                    m = psa.getSettingsModule(SettingsModules.Vibrato);
                    assertEquals(0, m.getSettingsValueProperty(ModParam.VibratoControl, v).get(), vn);
                    assertEquals(50, m.getSettingsValueProperty(ModParam.VibCents, v).get(), vn);
                    assertEquals(64, m.getSettingsValueProperty(ModParam.VibRate, v).get(), vn);
                    m = psa.getSettingsModule(SettingsModules.Arpeggiator);
                    assertEquals(0, m.getSettingsValueProperty(ModParam.ArpEnable, v).get(), vn);
                    assertEquals(0, m.getSettingsValueProperty(ModParam.ArpDir, v).get(), vn);
                    assertEquals(0, m.getSettingsValueProperty(ModParam.ArpOctaves, v).get(), vn);
                    assertEquals(3, m.getSettingsValueProperty(ModParam.ArpTime, v).get(), vn);

                    m = psa.getSettingsModule(SettingsModules.Morphs);
                    for (int j = 0; j < 16; j++) {
                        assertEquals(j < 8 ? 0 : 1,m.getParamValueProperty(v,j).get(),vn+":"+j);
                    }
                }

                // morph labels
                PatchModule m = psa.getSettingsModule(SettingsModules.Morphs);
                assertEquals("Wheel",m.getMorphLabel(0).get(),n);
                assertEquals("Vel",m.getMorphLabel(1).get(),n);
                assertEquals("Keyb",m.getMorphLabel(2).get(),n);
                assertEquals("Aft.Tch",m.getMorphLabel(3).get(),n);
                assertEquals("Sust.Pd",m.getMorphLabel(4).get(),n);
                assertEquals("Ctrl.Pd",m.getMorphLabel(5).get(),n);
                assertEquals("P.Stick",m.getMorphLabel(6).get(),n);
                assertEquals("G.Wh 2",m.getMorphLabel(7).get(),n);

                //selected param TODO this logic is bad, needs to be area-level invariant
                assertEquals(new PatchArea.SelectedParam(1,0),
                        patch.getArea(AreaId.Settings).getSelectedParam(),n);

            }


        }

    }

    public static Device dispatchMsgs(List<MessageRecorder.RecordedUsbMessage> ms) {
        Device d = new Device();
        dispatchMsgs(ms, d);
        return d;
    }

    public static void dispatchMsgs(List<MessageRecorder.RecordedUsbMessage> ms, Device d) {
        int i = 0;
        for (MessageRecorder.RecordedUsbMessage m : ms) {
            assertTrue(d.dispatch(m.msg()), "dispatch message " + i + ": " +
                Util.dumpBufferString(m.msg().buffer()));
            i++;
        }
    }

    @Test
    void regress003_Inbound() throws Exception {

        Device d = dispatchMsgs(
                parseCapture("data/capture/capture-003-loadmem-g2fx-perf1.pcapng", MessageRecorder.INBOUND));

        Performance p = d.getPerf();
        // match file version and current notes
        p.setVersion(1);
        for (Patch s : p.slots()) {
            s.getCurrentNote().update(Protocol.CurrentNote.NoteCount.value(0));
            List<FieldValues> ns = Protocol.CurrentNote.Notes.subfieldsValue(s.getCurrentNote());
            FieldValues n = ns.get(0);
            ns.clear();
            ns.add(n);
        }

        ByteBuffer pbuf = p.writeFile();

        ByteBuffer fbuf = Util.readFile(PerformanceTest.PERF_001);

        //fix module params version count
        overwriteBytes(fbuf,
                0x237,0,0x238,0,0x23d,0,0x23e,0,
                0x58f,0,0x590,0,0x595,0,0x596,0,
                0x8f4,0,0x8f5,0,0x8fa,0,0x8fb,0,
                0xc7d,0,0xc7e,0,0xc83,0,0xc84,0
        );

        assertEquals(Util.dumpBufferString(dropCrcTrailer(fbuf)),
                Util.dumpBufferString(dropCrcTrailer(pbuf)));

    }

    @Test
    void testVersionDispatch() throws Exception {
        List<MessageRecorder.RecordedUsbMessage> ms = captureCmd(
                List.of(Codes.I_VERSION1, Codes.I_VERSION2),
                CAP_OO4_POWERON);
        Device d = new Device();
        //init device to version 10; device inits to 0 at power on, but hard to diff below.
        d.getPerf().setVersion(10);
        d.getPerf().slots().forEach(s -> s.setVersion(10));
        int i = 0;
        for (MessageRecorder.RecordedUsbMessage m : ms) {
            d.dispatch(m.msg());
            assertEquals(
                    switch (i++) {
                        // 40 36 04 00 (perf -> 0)
                        case 0 -> List.of(0,10,10,10,10);
                        // 40 36 00 00 (A -> 0)
                        case 1 -> List.of(0, 0,10,10,10);
                        // 40 36 01 00 (B -> 0)
                        case 2 -> List.of(0, 0, 0,10,10);
                        // 40 36 02 00 (C -> 0)
                        case 3 -> List.of(0, 0, 0, 0,10);
                        // 40 36 03 00 (D -> 0)
                        case 4 -> List.of(0, 0, 0, 0, 0);
                        // 40 36 04 00 (perf -> 0)
                        case 5 -> List.of(0, 0, 0, 0, 0);
                        // 40 36 00 01 36 01 01 36 02 01 36 03 01 36 04 01 (all -> 1)
                        case 6 -> List.of(1, 1, 1, 1, 1);
                        // 40 1f 01 36 00 02 36 01 02 36 02 02 36 03 02 (slots -> 2)
                        case 7 -> List.of(1, 2, 2, 2, 2);
                        // 40 36 04 01 (perf -> 1)
                        case 8 -> List.of(1, 2, 2, 2, 2);
                        default -> fail("Only expecting 8 version msgs");
                    },
                List.of(d.getPerf().getVersion(),
                        d.getPerf().getSlot(Slot.A).getVersion(),
                        d.getPerf().getSlot(Slot.B).getVersion(),
                        d.getPerf().getSlot(Slot.C).getVersion(),
                        d.getPerf().getSlot(Slot.D).getVersion()),
                    "msg " + i + ": " + Util.dumpBufferString(m.msg().buffer()));

        }
    }


    public static List<MessageRecorder.RecordedUsbMessage> captureCmd(List<Integer> cmdCodes, String capFile) throws Exception {
        List<MessageRecorder.RecordedUsbMessage> ms =
                parseCapture(capFile, MessageRecorder.INBOUND)
                        .stream().filter(mkCmdFilter(cmdCodes)).toList();
        return ms;
    }

    private static Predicate<MessageRecorder.RecordedUsbMessage> mkCmdFilter(List<Integer> cmdCodes) {
        return m -> cmdCodes.contains(
                (int) m.msg().buffer().get(m.msg().extended() ? 3 : 4));
    }


}
