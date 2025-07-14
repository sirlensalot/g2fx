package org.g2fx.g2lib;

import org.g2fx.g2lib.state.UsbDevice;
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
        UsbDevice d = new UsbDevice(usb);
        ByteBuffer buf = Util.readFile("data/msg_PatchListMessage00_19f4.msg");
        d.dispatchEntryList(buf.position(4).slice());
        UsbDevice.EntriesMsg m = d.getEntriesMsg();
        assertEquals(UsbDevice.EntryType.Patch, m.type());
        assertFalse(m.done());
        assertEquals(List.of(
                new UsbDevice.EntryBank(0,0,List.of(
                        new UsbDevice.Entry("-- Welcome G2 --",12), 
                        new UsbDevice.Entry("BackTo72",8), 
                        new UsbDevice.Entry("FM_Filter",12), 
                        new UsbDevice.Entry("HooverSeq2_DZ",2),
                        new UsbDevice.Entry("WhatIsSync",8),
                        new UsbDevice.Entry("NordSynth",12),
                        new UsbDevice.Entry("FATBass  NL2",3),
                        new UsbDevice.Entry("AccGuit",1),
                        new UsbDevice.Entry("HornModel",1),
                        new UsbDevice.Entry("YetAnotherOrgan",9),
                        new UsbDevice.Entry("ZeroHzLinFM",12),
                        new UsbDevice.Entry("Bells",1),
                        new UsbDevice.Entry("Cue   NL3",12),
                        new UsbDevice.Entry("LushModular",10),
                        new UsbDevice.Entry("GlassFright",7))
                )),m.banks());

    }
}
