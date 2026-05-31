package org.g2fx.g2lib;

import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.protocol.Codes;
import org.g2fx.g2lib.state.Entries;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.usb.OfflineSender;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.g2fx.g2lib.DeviceTest.captureCmd;
import static org.g2fx.g2lib.DeviceTest.dispatchMsgs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class EntriesTest {

    @BeforeAll
    public static void beforeAll() {
        Util.configureLogging();
    }


    @Test
    public void dispatchEntryList() throws Exception {
        Entries d = new Entries(new OfflineSender());
        ByteBuffer buf = Util.readFile("data/msg/msg_PatchListMessage00_19f4.msg");
        d.dispatchEntryList(buf.position(4).slice());
        Entries.EntriesMsg m = d.getEntriesMsg();
        assertEquals(Entries.EntryType.Patch, m.type());
        assertFalse(m.done());
        assertEquals(List.of(
                new Entries.EntryBank(0,0,List.of(
                        new Entries.Entry("-- Welcome G2 --",12),
                        new Entries.Entry("BackTo72",8),
                        new Entries.Entry("FM_Filter",12),
                        new Entries.Entry("HooverSeq2_DZ",2),
                        new Entries.Entry("WhatIsSync",8),
                        new Entries.Entry("NordSynth",12),
                        new Entries.Entry("FATBass  NL2",3),
                        new Entries.Entry("AccGuit",1),
                        new Entries.Entry("HornModel",1),
                        new Entries.Entry("YetAnotherOrgan",9),
                        new Entries.Entry("ZeroHzLinFM",12),
                        new Entries.Entry("Bells",1),
                        new Entries.Entry("Cue   NL3",12),
                        new Entries.Entry("LushModular",10),
                        new Entries.Entry("GlassFright",7))
                )),m.banks());

    }

    @Test
    public void testNonContiguous() throws Exception {
        ByteBuffer b = Util.readBufferDump(
                """
                        0000  5b 04 16 01 01 03 01 01 63 61 72 6e 69 76 6f 72   [.......carnivor
                        0010  65 00 00 73 74 6f 6e 65 72 00 00 61 64 61 67 69   e..stoner..adagi
                        0020  6f 00 00 70 72 6f 63 65 73 73 69 6f 6e 00 00 63   o..procession..c
                        0030  6f 6c 6f 73 73 75 73 00 00 67 6f 20 66 6f 72 74   olossus..go.fort
                        0040  68 20 6e 6f 74 00 00 01 14 73 74 6f 6e 65 72 2d   h.not....stoner-
                        0050  53 54 55 44 49 4f 00 00 02 63 6f 6c 6f 73 73 75   STUDIO...colossu
                        0060  73 2d 53 54 55 44 49 4f 00 00 61 64 61 67 69 6f   s-STUDIO..adagio
                        0070  2d 53 54 55 44 49 4f 00 00 63 61 72 6e 69 76 6f   -STUDIO..carnivo
                        0080  72 65 2d 53 54 55 44 49 4f 00 61 6c 65 67 72 65   re-STUDIO.alegre
                        0090  74 74 6f 2d 53 54 55 44 49 4f 00 67 6f 20 66 6f   tto-STUDIO.go.fo
                        00a0  72 74 68 20 6e 6f 74 2d 53 54 55 00 70 72 6f 63   rth.not-STU.proc
                        00b0  65 73 73 69 6f 6e 2d 53 54 55 44 49 00 05 42 57   ession-STUDI..BW
                        """
        );
        Entries d = new Entries(new OfflineSender());
        d.dispatchEntryList(b);
        AtomicReference<Entries.EntriesEvent> ee = new AtomicReference<>();
        d.getEventProp().addListener((o,e) -> ee.set(e));
        d.fireRefreshAll();
        Map<Integer, Entries.Entry> bank = ee.get().entries().get(Entries.EntryType.Performance).get(1);
        assertEquals(new TreeSet<>(Set.of(1, 2, 3, 4, 5, 6, 20, 22, 23, 24, 25, 26, 27)),bank.keySet());
    }

    @Test
    public void testEntriesDispatch() throws Exception {
        List<MessageRecorder.RecordedUsbMessage> ms =
                captureCmd(List.of(Codes.I_VERSION1, Codes.I_ENTRY_LIST), DeviceTest.CAP_OO4_POWERON);
        Device d = new Device();
        List<Entries.EntriesEvent> evs = new ArrayList<>();
        d.getEntries().getEventProp().addListener((o,n) -> evs.add(n));
        dispatchMsgs(ms,d);
        assertEquals(3,evs.size());
        //patches done
        assertEquals(new TreeSet<>(Set.of(0,1,2,10,11)),evs.get(0).entries().get(Entries.EntryType.Patch).keySet());
        assertEquals(new TreeSet<>(Set.of()),evs.get(0).entries().get(Entries.EntryType.Performance).keySet());
        //perfs done
        assertEquals(new TreeSet<>(Set.of(0,1,2,10,11)),evs.get(1).entries().get(Entries.EntryType.Patch).keySet());
        assertEquals(new TreeSet<>(Set.of(0,1,2,3,5)),evs.get(1).entries().get(Entries.EntryType.Performance).keySet());
        //perf store
        assertEquals(new TreeSet<>(Set.of(0,1,2,10,11)),evs.get(2).entries().get(Entries.EntryType.Patch).keySet());
        assertEquals(new TreeSet<>(Set.of(0,1,2,3,4,5)),evs.get(2).entries().get(Entries.EntryType.Performance).keySet());

//        MessageRecorder mr = new MessageRecorder("entries2",new File("data/record"));
//        for (MessageRecorder.RecordedUsbMessage m : ms) {
//            mr.record(m.msg());
//        }
//        mr.stop();
    }


}
