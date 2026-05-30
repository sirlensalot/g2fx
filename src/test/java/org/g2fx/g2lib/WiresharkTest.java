package org.g2fx.g2lib;

import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WiresharkTest {

    @BeforeAll
    public static void beforeAll() {
        Util.configureLogging();
    }

    @Test
    void playStartup() throws Exception {
        List<MessageRecorder.RecordedUsbMessage> ms = DeviceTest.parseCapture("data/capture/poweron2.pcapng",
                MessageRecorder.INBOUND);
        Device d = new Device();
        for (MessageRecorder.RecordedUsbMessage m : ms) {
            assertTrue(d.dispatch(m.msg()));
        }
        //d.getPerf().dumpYaml("data/startup.yaml");
    }
    @Test
    void testStartup() throws Exception{
        ByteBuffer bb = Util.readFile("data/capture/poweron2.pcapng");
        Map<String,Set<String>> endpointTypes = new TreeMap<>();
        Map<String,Map<String,Set<String>>> valsByEndpoint = new TreeMap<>();
        Map<String,Set<String>> vals = new TreeMap<>();
        int count=0;
        List<Util.UsbPacket> ps = Util.readPcapNg(bb);
        for (Util.UsbPacket p : ps) {

            String t = i2x(p.data().get(0x1f));
            String ep = i2x(p.data().get(0x1e));
            endpointTypes.compute(ep,(i,s) -> { s = s == null ? new TreeSet<>() : s; s.add(t); return s;});

            for (int i = 0; i < 0x20; i++) {
                String v = i2x(p.data().get(i));
                String k = i2x((byte) i);
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
        for (Util.UsbPacket p : ps) {
            String pid = String.format("%02x%02x",p.data().get(0x11),p.data().get(0x10));
            if (!pid.equals(opid)) { System.out.println(); }
            opid = pid;
            byte t = p.data().get(0x1f);
            int len = p.data().limit() - 0x20;
            System.out.printf("%04x %02x[%s] %s.%02x: 05=%02x, 1a=%02x, 1c=%02x, 1d=%02x, l=%02x%02x:%04x\n",
                    i++,
                    p.data().get(0x1e), t==3?"I":t==0?"C":"B",pid,p.data().get(0x03),
                    p.data().get(0x05),p.data().get(0x1a),p.data().get(0x1c),p.data().get(0x1d),
                    p.data().get(0x05),p.data().get(0x04), len);
            if (len > 0) {
                System.out.println(Util.dumpBufferString(p.data().slice(0x20,len)));
            }
        }

    }
    /*
0000  00 01 20 01 01 00 00 00 00 00 00 00 00 00 00 00   .. .............
#     ^^ ^^ 00-01: darwin header version (0x0100)
#           ^^ 02: darwin header length (32)
#              ^^ 03: Request type: COMPLETE (1)
#                                   SUBMIT (0)
#                 ^^ ^^ 04-05: I/O Length (0x0001)
# 06-09:status,success  ^^ ^^ ^^ ^^ (0x00000000)
#  10: Isochronous tfr # frames (0) ^^
#                          0a-0f: ???  ^^ ^^ ^^ ^^ ^^
0010  e9 14 d4 00 00 00 00 00 00 00 60 14 02 01 81 03   ..........`.....
#     ^^ ^^ ^^ ^^ ^^ ^^ ^^ ^^ 10-17: I/O ID
#   18-1b: Device location ID ^^ ^^ ^^ ^^
#    1c: Device Speed: High (2), Full (1) ^^
#          1d: Device index (1) {0x01,0x0d}  ^^
#                 1e: {00,03,80,81,82} endpoint ^^
#    1f:{00,02,03};02=bulk,03=interrupt,00=control ^^
0020  02                                                .
#     20-...:"I/O" payload

     */

    private static String i2x(byte v) {
        return String.format("%02x",v);
    }
}
