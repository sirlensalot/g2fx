package org.g2fx.g2lib;

import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WiresharkTest {


    @Test
    void wireshark() throws Exception {
        BufferedReader br = new BufferedReader(new StringReader("""
                0000  00 01 20 00 01 00 00 00 00 00 00 00 00 00 00 00   .. .............
                0010  0b 15 d4 00 00 00 00 00 00 00 60 14 02 01 81 03   ..........`.....
                
                """));
        List<Integer> l = Util.readWiresharkList(br);
        assertEquals(List.of(
                        0x00, 0x01, 0x20, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x0b, 0x15, 0xd4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x60, 0x14, 0x02, 0x01, 0x81, 0x03),
                l);
        assertNull(Util.readWireshark(br));
    }



    @Test
    void testStartup() throws Exception{
        BufferedReader br = new BufferedReader(new FileReader("data/poweron.txt"));
        Map<String,Set<String>> endpointTypes = new TreeMap<>();
        Map<String,Map<String,Set<String>>> valsByEndpoint = new TreeMap<>();
        Map<String,Set<String>> vals = new TreeMap<>();
        int count=0;
        List<List<Integer>> ps = new ArrayList<>();
        List<Integer> l;
        while ((l = Util.readWiresharkList(br))!=null) {

            ps.add(l);

            String t = i2x(l.get(0x1f));
            String ep = i2x(l.get(0x1e));
            endpointTypes.compute(ep,(i,s) -> { s = s == null ? new TreeSet<>() : s; s.add(t); return s;});

            for (int i = 0; i < 0x20; i++) {
                String v = i2x(l.get(i));
                String k = i2x(i);
                vals.compute(k,
                        (k_,s) -> { s=s==null?new TreeSet<>():s;s.add(v);return s; });
                valsByEndpoint.compute(ep,(ep_,m) -> {
                    m = m == null ? new TreeMap<>() : m;
                    m.compute(k,(k_,s) -> {
                        s = s == null ? new TreeSet<>() : s;
                        s.add(v);
                        return s;
                    });
                    return m;
                });
            }

            count++;
        }
        System.out.println(count);
        System.out.println(endpointTypes);
        for (String i : vals.keySet()) {
            System.out.println(i + ": " + vals.get(i));
        }
        for (String ep : valsByEndpoint.keySet()) {
            System.out.println("Endpoint vals: " + ep);
            for (String i : valsByEndpoint.get(ep).keySet()) {
                System.out.println("  " + i + ": " + valsByEndpoint.get(ep).get(i));
            }
        }

        int i = 0;
        String opid = "";
        for (List<Integer> p : ps) {
            String pid = String.format("%02x%02x",p.get(0x11),p.get(0x10));
            if (!pid.equals(opid)) { System.out.println(); }
            opid = pid;
            Integer t = p.get(0x1f);
            System.out.printf("%04x %02x[%s] %s.%02x: 05=%02x, 1a=%02x, 1c=%02x, 1d=%02x, l=%02x%02x:%04x\n",
                    i++,
                    p.get(0x1e), t==3?"I":t==0?"C":"B",pid,p.get(0x03),
                    p.get(0x05),p.get(0x1a),p.get(0x1c),p.get(0x1d),p.get(0x05),p.get(0x04),p.size()-0x20);
        }

    }
    /*
0000  00 01 20 01 01 00 00 00 00 00 00 00 00 00 00 00   .. .............
##00  == == ==          == == == == == == == == == ==
#  03: {00,01} ^^ pair id? see 10+11
#                 ^^ ^^ 04+05: length bige
0010  e9 14 d4 00 00 00 00 00 00 00 60 14 02 01 81 03   ..........`.....
##10        == == == == == == == ==    ==
#     ^^ ^^ 10+11: big-e pair id + 03, skips by 2
#  1a:{60,61};1c:{01,02};1d:{01,0d} ^^    ^^ ^^
#                 1e: {00,03,80,81,82} endpoint ^^
#   1f:{00,02,03};02=bulk,03=interrupt,00=control? ^^
# 1f by 1e: 00:00, 03:02, 80:00, 81:03, 82:02
# 00: Control  04=0,05=0,10=[0f,13,17,2d],11=[15],1f=00
# 03: OUT_BULK 11=[15,16,17,18],1a=61,1c=01,1d=0d,1f=02
# 80: Control  11=15,1f=00
# 81: IN_IRRPT 1f=03
# 82: IN_BULK  1a=61,1c=01,1d=0d,1f=02
0020  02                                                .
##20  payload

     */

    private static String i2x(Integer v) {
        return String.format("%02x",v);
    }
}
