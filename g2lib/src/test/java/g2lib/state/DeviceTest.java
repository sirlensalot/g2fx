package g2lib.state;

import g2lib.usb.Usb;
import g2lib.util.Util;
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
        Device d = new Device(usb);
        ByteBuffer buf = Util.readFile("data/msg_PatchListMessage00_19f4.msg");
        d.dispatchEntryList(buf.position(4).slice());
        Device.EntriesMsg m = d.getEntriesMsg();
        assertEquals(Device.EntryType.Patch, m.type());
        assertFalse(m.done());
        assertEquals(List.of(
                new Device.EntryBank(0,0,List.of(
                        new Device.Entry("-- Welcome G2 --",12), 
                        new Device.Entry("BackTo72",8), 
                        new Device.Entry("FM_Filter",12), 
                        new Device.Entry("HooverSeq2_DZ",2),
                        new Device.Entry("WhatIsSync",8),
                        new Device.Entry("NordSynth",12),
                        new Device.Entry("FATBass  NL2",3),
                        new Device.Entry("AccGuit",1),
                        new Device.Entry("HornModel",1),
                        new Device.Entry("YetAnotherOrgan",9),
                        new Device.Entry("ZeroHzLinFM",12),
                        new Device.Entry("Bells",1),
                        new Device.Entry("Cue   NL3",12),
                        new Device.Entry("LushModular",10),
                        new Device.Entry("GlassFright",7))
                )),m.banks());

    }
}
