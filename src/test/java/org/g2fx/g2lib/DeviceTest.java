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
        assertEquals(4,d.getPerf().getSlot(Slot.A).getVersion());
    }



}
