package org.g2fx.g2lib;

import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.util.CRC16;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void crcClavia() {


        assertEquals(0x9188, CRC16.crc16(new byte[] {(byte) 0x80},0,1));
        byte[] msg = Util.asBytes(
                0x80, 0x0a, 0x03, 0x00, 0x00, 0x1a, 0x00, 0x8c, 0x00, 0x12, 0x4d, 0x6f, 0x64, 0x75, 0x6c, 0x61,
                0x72, 0x47, 0x32, 0x00, 0x30, 0x03, 0x4c, 0x52, 0x00, 0x00, 0x01, 0x96, 0x28, 0x61, 0x00, 0x05,
                0x01, 0x0a, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        );
        assertEquals(0x3e24,CRC16.crc16(msg,0,msg.length));

        assertEquals(8, Util.b2i((byte) 0x82) >> 4);

        assertEquals(0x1bd6, Util.addb((byte) 0x1b, (byte) 0xd6));

        assertArrayEquals(Util.asBytes(0,1,2,3,4,5),
                Util.concat(Util.asBytes(0,1,2),Util.asBytes(3,4),Util.asBytes(5)));

        assertEquals("S&H", ModuleType.M_S_and_H.shortName);
        assertEquals("T&H", ModuleType.M_T_and_H.shortName);
        assertEquals("2-In",ModuleType.M_2_In.shortName);

        TreeSet<ModuleType.ModPageIx> ixs = new TreeSet<>();
        for (ModuleType mt : ModuleType.values()) {
            if (!ixs.add(mt.modPageIx)) {
                fail("Bad mod page ix: " + mt);
            }
        }
        {
            int i = -1;
            ModuleType.ModPage page = null;
            for (ModuleType.ModPageIx ix : ixs) {
                if (ix.page() != page) {
                    page = ix.page();
                    i = ix.ix();
                    if (i != 0) {
                        fail("Bad starting index: " + ix);
                    }

                } else {
                    if (++i != ix.ix()) {
                        fail("Non-monotonic ix: " + ix);
                    }
                }
            }
        }

        {
            Consumer<String> f = s -> {
                int i = Integer.parseInt(s,2);
                System.out.printf("%s: ",new StringBuilder(s));
                int mask = 0x03;
                int j = 0;
                int r = (i & mask) >> j;
                System.out.format("%s ",r>0);
                mask = mask << 2;
                j++;
                r = (i & mask) >> j;
                System.out.format("%s ",r>0);
                mask = mask << 2;
                j++;
                r = (i & mask) >> j;
                System.out.format("%s ",r>0);
                mask = mask << 2;
                j++;
                r = (i & mask) >> j;
                System.out.format("%s \n",r>0);
            };
            f.accept("00000000");
            f.accept("00000001");
            f.accept("00000100");
            f.accept("00000101");

            f.accept("00010000");
            f.accept("00010001");
            f.accept("00010100");
            f.accept("00010101");

            f.accept("01000000");
            f.accept("01000001");
            f.accept("01000100");
            f.accept("01000101");
            f.accept("01010000");
            f.accept("01010001");
            f.accept("01010100");
            f.accept("01101101");

        }
    }

}
