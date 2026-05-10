package org.g2fx.g2lib;

import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class WiresharkTest {


    @Test
    void wireshark() throws Exception {
        BufferedReader br = new BufferedReader(new StringReader("""
                0000  00 01 20 00 01 00 00 00 00 00 00 00 00 00 00 00   .. .............
                0010  0b 15 d4 00 00 00 00 00 00 00 60 14 02 01 81 03   ..........`.....
                
                """));
        ByteBuffer bb = Util.readWireshark(br);
        assertNotNull(bb);
        bb.rewind();
        List<Integer> l = buf2List(bb);
        assertEquals(List.of(
                        0x00, 0x01, 0x20, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x0b, 0x15, 0xd4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x60, 0x14, 0x02, 0x01, 0x81, 0x03),
                l);
        assertNull(Util.readWireshark(br));
    }

    private static List<Integer> buf2List(ByteBuffer bb) {
        List<Integer> l = new ArrayList<>();
        while (bb.hasRemaining()) { l.add(Util.b2i(bb.get())); }
        return l;
    }

    @Test
    void testStartup() throws Exception{
        BufferedReader br = new BufferedReader(new FileReader("data/poweron.txt"));
        ByteBuffer bb;
        Map<String,Set<String>> vals = new TreeMap<>();
        int count=0;
        while ((bb = Util.readWireshark(br))!=null) {
            String t = Integer.toHexString(Util.b2i(bb.get(0x1e)));
            String v = Integer.toHexString(Util.b2i(bb.get(0x1f)));
            vals.compute(t,(i,s) -> { s = s == null ? new TreeSet<>() : s; s.add(v); return s;});
            count++;
        }
        System.out.println(count);
        System.out.println(vals);
    }
}
