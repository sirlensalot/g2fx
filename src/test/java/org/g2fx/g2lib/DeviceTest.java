package org.g2fx.g2lib;

import org.g2fx.g2lib.state.Entries;
import org.g2fx.g2lib.usb.Usb;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class DeviceTest {

    @Test
    public void dispatchEntryList() throws Exception {
        Usb usb = mock(Usb.class);
        Entries d = new Entries(usb);
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
}
