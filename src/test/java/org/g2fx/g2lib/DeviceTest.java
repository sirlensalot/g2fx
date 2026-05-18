package org.g2fx.g2lib;

import org.g2fx.g2lib.state.*;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.usb.UsbMessage;
import org.g2fx.g2lib.usb.UsbSender;
import org.g2fx.g2lib.util.CRC16;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @BeforeAll
    public static void beforeAll() {
        Util.configureLogging();
    }


    @Test
    public void dispatchEntryList() throws Exception {
        Entries d = new Entries(new UsbSender.OfflineSender());
        ByteBuffer buf = Util.readFile("data/msg_PatchListMessage00_19f4.msg");
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

    @Test
    void writePerfMessage() throws Exception {

        List<MessageRecorder.RecordedUsbMessage> ms =
                parseCapture("data/capture/capture-001-load-g2fx-perf1.pcapng", (i) -> true);

        UsbMessage m = ms.get(2).msg();
        //this is to get logging for comparing bitbuffer images, turn on Fields,Sections,Patch in logging.properties
        ByteBuffer mpb = m.buffer().slice(0x7d, m.buffer().capacity()-0x7d);
        for (Slot s : Slot.values()) {
            new Patch(s,new UsbSender.OfflineSender()).readFileSections(mpb);
        }

        //begin test. load same file and write data for sendBulk
        Performance perf = Performance.readFromFile("data/perf/g2fx-perf-01.prf2",new UsbSender.OfflineSender());
        perf.setFileName("g2fx-perf-01");
        ByteBuffer bulkMsg = perf.writeMessage();

        //add length preface
        ByteBuffer buf = ByteBuffer.allocate(bulkMsg.limit() + 4);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) buf.limit());


        // fix incompatibilities.
        // unknown values in ModuleNames.Reserved(6)
        bulkMsg.put(0x0311-2,(byte)0x42); // offset -2 from pcap (no length prefix)
        bulkMsg.put(0x06a4-2,(byte)0x08);
        bulkMsg.put(0x0a34-2,(byte)0x38);
        // variation count saved as 9 in empty ModuleParams but 0 in message
        // 2 per slot
        for (int i : List.of(
                0x0226,0x022c,
                0x05a9,0x05af,
                0x0939,0x093f,
                0x0ced,0x0cf3)) {
            bulkMsg.putShort(i-2,(byte) 0);
        }

        buf.put(bulkMsg.rewind());
        buf.putShort((short) CRC16.crc16(bulkMsg, 0, bulkMsg.limit()));
        assertEquals(Util.dumpBufferString(m.buffer()),Util.dumpBufferString(buf));

    }

    public static List<MessageRecorder.RecordedUsbMessage> parseCapture(String file, Predicate<Byte> epPred)
            throws Exception {
        ByteBuffer bb = Util.readFile(file);
        List<Util.UsbPacket> ps = Util.readPcapNg(bb);
        List<MessageRecorder.RecordedUsbMessage> ms = MessageRecorder.readCapture(ps, epPred);
        return ms;
    }

    @Test
    void regressNewPerf() throws Exception {
        List<MessageRecorder.RecordedUsbMessage> ms =
                parseCapture("data/capture/capture-newperf.pcapng", MessageRecorder.INBOUND);
        Device d = new Device();
        int i = 0;
        for (MessageRecorder.RecordedUsbMessage m : ms) {
            assertTrue(d.dispatch(m.msg()), "dispatch message " + i + ": " +
                Util.dumpBufferString(m.msg().buffer()));
            i++;
        }
        //TODO regress all perf data
        assertEquals(4,d.getPerf().getSlot(Slot.A).getVersion());
        /*
2026-05-18 16:00:59.641 INFO Fields: PerformanceSettings [READ]: 0-280, 80.000 bytes
0 0 0 0 78 0 0 0 [ 4e 6f 20 6e 61 6d 65 0 ]
1 1 0 0 0 0 7f 20000 [ 4e 6f 20 6e 61 6d
65 0 ] 1 0 0 0 0 0 7f 30000 [ 4e 6f 20 6e
61 6d 65 0 ] 1 0 0 0 0 0 7f 40000 [ 4e 6f
20 6e 61 6d 65 0 ] 1 0 0 0 0 0 7f 50000

2026-05-18 16:00:59.644 INFO Fields: ModuleParams [READ]: 2-b25, 356.375 bytes
7 a 1 10 0 0 0 0 0 0 0 0 0 1 1 1 1 1
1 1 1 1 0 0 0 0 0 0 0 0 1 1 1 1 1 1
1 1 2 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1
1 3 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 4
0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 5 0
0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 6 0 0
0 0 0 0 0 0 1 1 1 1 1 1 1 1 7 0 0 0
0 0 0 0 0 1 1 1 1 1 1 1 1 8 0 0 0 0
0 0 0 0 1 1 1 1 1 1 1 1 9 0 0 0 0 0
0 0 0 1 1 1 1 1 1 1 1 2 2 0 64 1 1 64
1 2 64 1 3 64 1 4 64 1 5 64 1 6 64 1 7
64 1 8 64 1 9 64 1 3 2 0 0 1c 1 0 1c 2 0
1c 3 0 1c 4 0 1c 5 0 1c 6 0 1c 7 0 1c 8
0 1c 9 0 1c 4 2 0 1 1 1 1 1 2 1 1 3 1
1 4 1 1 5 1 1 6 1 1 7 1 1 8 1 1 9
1 1 5 3 0 0 32 40 1 0 32 40 2 0 32 40 3 0
32 40 4 0 32 40 5 0 32 40 6 0 32 40 7 0 32
40 8 0 32 40 9 0 32 40 6 4 0 0 3 0 0 1 0
3 0 0 2 0 3 0 0 3 0 3 0 0 4 0 3 0 0
5 0 3 0 0 6 0 3 0 0 7 0 3 0 0 8 0 3
0 0 9 0 3 0 0 7 2 0 2 1 1 2 1 2 2
1 3 2 1 4 2 1 5 2 1 6 2 1 7 2 1 8 2
1 9 2 1
2026-05-18 16:00:59.645 INFO Patch:A: updateSection: SPatchParams_4d[4d:Settings]
2026-05-18 16:00:59.651 INFO Fields: MorphLabels [READ]: 2-29a, 83.000 bytes
1 1 50 1 8 8 [ 57 68 65 65 6c 0 0 ] 1 8 9
[ 56 65 6c 0 0 0 0 ] 1 8 a [ 4b 65 79 62 0 0
0 ] 1 8 b [ 41 66 74 2e 54 63 68 ] 1 8 c [ 53 75
73 74 2e 50 64 ] 1 8 d [ 43 74 72 6c 2e 50 64 ] 1
8 e [ 50 2e 53 74 69 63 6b ] 1 8 f [ 47 2e 57 68
20 32 0 ]
2026-05-18 16:00:59.651 INFO Patch:A: updateSection: SMorphLabels_5b[5b:Settings]
2026-05-18 16:00:59.652 INFO Fields: SelectedParam [READ]: 0-20, 4.000 bytes
1 2 1 0
2026-05-18 16:00:59.661 INFO Fields: ModuleParams [READ]: 2-b25, 356.375 bytes
7 a 1 10 0 0 0 0 0 0 0 0 0 1 1 1 1 1
1 1 1 1 0 0 0 0 0 0 0 0 1 1 1 1 1 1
1 1 2 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1
1 3 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 4
0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 5 0
0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 6 0 0
0 0 0 0 0 0 1 1 1 1 1 1 1 1 7 0 0 0
0 0 0 0 0 1 1 1 1 1 1 1 1 8 0 0 0 0
0 0 0 0 1 1 1 1 1 1 1 1 9 0 0 0 0 0
0 0 0 1 1 1 1 1 1 1 1 2 2 0 64 1 1 64
1 2 64 1 3 64 1 4 64 1 5 64 1 6 64 1 7
64 1 8 64 1 9 64 1 3 2 0 0 1c 1 0 1c 2 0
1c 3 0 1c 4 0 1c 5 0 1c 6 0 1c 7 0 1c 8
0 1c 9 0 1c 4 2 0 1 1 1 1 1 2 1 1 3 1
1 4 1 1 5 1 1 6 1 1 7 1 1 8 1 1 9
1 1 5 3 0 0 32 40 1 0 32 40 2 0 32 40 3 0
32 40 4 0 32 40 5 0 32 40 6 0 32 40 7 0 32
40 8 0 32 40 9 0 32 40 6 4 0 0 3 0 0 1 0
3 0 0 2 0 3 0 0 3 0 3 0 0 4 0 3 0 0
5 0 3 0 0 6 0 3 0 0 7 0 3 0 0 8 0 3
0 0 9 0 3 0 0 7 2 0 2 1 1 2 1 2 2
1 3 2 1 4 2 1 5 2 1 6 2 1 7 2 1 8 2
1 9 2 1
2026-05-18 16:00:59.661 INFO Patch:B: updateSection: SPatchParams_4d[4d:Settings]
2026-05-18 16:00:59.663 INFO Fields: MorphLabels [READ]: 2-29a, 83.000 bytes
1 1 50 1 8 8 [ 57 68 65 65 6c 0 0 ] 1 8 9
[ 56 65 6c 0 0 0 0 ] 1 8 a [ 4b 65 79 62 0 0
0 ] 1 8 b [ 41 66 74 2e 54 63 68 ] 1 8 c [ 53 75
73 74 2e 50 64 ] 1 8 d [ 43 74 72 6c 2e 50 64 ] 1
8 e [ 50 2e 53 74 69 63 6b ] 1 8 f [ 47 2e 57 68
20 32 0 ]
2026-05-18 16:00:59.663 INFO Patch:B: updateSection: SMorphLabels_5b[5b:Settings]
2026-05-18 16:00:59.664 INFO Fields: SelectedParam [READ]: 0-20, 4.000 bytes
1 2 1 0
2026-05-18 16:00:59.665 INFO Fields: ModuleParams [READ]: 2-b25, 356.375 bytes
7 a 1 10 0 0 0 0 0 0 0 0 0 1 1 1 1 1
1 1 1 1 0 0 0 0 0 0 0 0 1 1 1 1 1 1
1 1 2 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1
1 3 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 4
0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 5 0
0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 6 0 0
0 0 0 0 0 0 1 1 1 1 1 1 1 1 7 0 0 0
0 0 0 0 0 1 1 1 1 1 1 1 1 8 0 0 0 0
0 0 0 0 1 1 1 1 1 1 1 1 9 0 0 0 0 0
0 0 0 1 1 1 1 1 1 1 1 2 2 0 64 1 1 64
1 2 64 1 3 64 1 4 64 1 5 64 1 6 64 1 7
64 1 8 64 1 9 64 1 3 2 0 0 1c 1 0 1c 2 0
1c 3 0 1c 4 0 1c 5 0 1c 6 0 1c 7 0 1c 8
0 1c 9 0 1c 4 2 0 1 1 1 1 1 2 1 1 3 1
1 4 1 1 5 1 1 6 1 1 7 1 1 8 1 1 9
1 1 5 3 0 0 32 40 1 0 32 40 2 0 32 40 3 0
32 40 4 0 32 40 5 0 32 40 6 0 32 40 7 0 32
40 8 0 32 40 9 0 32 40 6 4 0 0 3 0 0 1 0
3 0 0 2 0 3 0 0 3 0 3 0 0 4 0 3 0 0
5 0 3 0 0 6 0 3 0 0 7 0 3 0 0 8 0 3
0 0 9 0 3 0 0 7 2 0 2 1 1 2 1 2 2
1 3 2 1 4 2 1 5 2 1 6 2 1 7 2 1 8 2
1 9 2 1
2026-05-18 16:00:59.665 INFO Patch:C: updateSection: SPatchParams_4d[4d:Settings]
2026-05-18 16:00:59.667 INFO Fields: MorphLabels [READ]: 2-29a, 83.000 bytes
1 1 50 1 8 8 [ 57 68 65 65 6c 0 0 ] 1 8 9
[ 56 65 6c 0 0 0 0 ] 1 8 a [ 4b 65 79 62 0 0
0 ] 1 8 b [ 41 66 74 2e 54 63 68 ] 1 8 c [ 53 75
73 74 2e 50 64 ] 1 8 d [ 43 74 72 6c 2e 50 64 ] 1
8 e [ 50 2e 53 74 69 63 6b ] 1 8 f [ 47 2e 57 68
20 32 0 ]
2026-05-18 16:00:59.667 INFO Patch:C: updateSection: SMorphLabels_5b[5b:Settings]
2026-05-18 16:00:59.667 INFO Fields: SelectedParam [READ]: 0-20, 4.000 bytes
1 2 1 0
         */
    }



}
