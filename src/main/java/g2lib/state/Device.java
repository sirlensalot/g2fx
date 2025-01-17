package g2lib.state;

import g2lib.BitBuffer;
import g2lib.protocol.Protocol;
import g2lib.Util;
import g2lib.protocol.FieldValues;
import g2lib.usb.Usb;
import g2lib.usb.UsbMessage;
import g2lib.usb.UsbReadThread;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Logger;

public class Device {

    private static final Logger log = Util.getLogger(Device.class);

    private final Usb usb;

    private final UsbReadThread readThread;

    private Performance perf;
    private FieldValues synthSettings;

    public Device(Usb usb, UsbReadThread readThread) {
        this.usb = usb;
        this.readThread = readThread;
    }

    public static Map<Integer, Map<Integer, String>> readEntryList(Usb usb, UsbReadThread readThread, int entryCount, boolean patchOrPerf) throws InterruptedException {
        Map<Integer, Map<Integer,String>> entries = new TreeMap<>();
        int bank = 0;
        int item = 0;
        entries.put(bank,new TreeMap<>());
        for (int i = 0; i < entryCount; i++) {
            usb.sendSystemCmd("patch list message: " + i
                    , 0x14 // Q_LIST_NAMES
                    , patchOrPerf ? 0 : 1 // pftPatch
                    , bank // bank
                    , item // item
            );
            UsbMessage beMsg = readThread.expect("patch list message: " + i, m ->
                            (!m.extended()) || m.head(0x01,0x0c,0x00,0x13));
            if (!beMsg.extended()) { log.info("Entry list empty: " + i); continue; }
            ByteBuffer buf = beMsg.buffer();
            buf.position(4);
            BitBuffer bb = new BitBuffer(buf.slice());
            FieldValues fvs = Protocol.BankEntries.FIELDS.read(bb);
            log.info(fvs.toString());
            Map<Integer, String> m = entries.get(bank);
            List<FieldValues> es = Protocol.BankEntries.Entries.subfieldsValue(fvs).orElse(new ArrayList<>());
            for (FieldValues e : es) {
                //log.info(e.toString());
                int bc = Protocol.BankEntry.BankChange.intValue(e).orElse(0);
                if (bc != 0) {
                    bank = (bc & 0xff00) >> 8;
                    item = bc & 0xff;
                    entries.put(bank, m = new TreeMap<>());
                    item = 0;
                }
                String n = Protocol.BankEntry.Name.stringValue(e).orElse("_error");
                m.put(item++, n);
            }

            Optional<Integer> term = Protocol.BankEntries.Terminator.intValue(fvs);
            if (term.isPresent() && term.get()==0x04) {
                break;
            }
        }
        //dumpEntries(patchOrPerf, entries);
        return entries;
        /*
        01 0c 00 13 74 01 16 01 00 03 0a 00 49 6e 70 75   . . . . t . . . . . . . I n p u
74 49 6e 74 65 72 70 72 65 74 65 72 00 64 72 75   t I n t e r p r e t e r . d r u
6d 65 66 66 65 63 74 73 00 00 45 66 66 65 63 74   m e f f e c t s . . E f f e c t
7a 00 00 45 66 66 65 63 74 7a 4c 46 53 52 00 00   z . . E f f e c t z L F S R . .
03 0b 00 50 65 64 61 6c 45 66 66 65 63 74 73 00   . . . P e d a l E f f e c t s .
00 04 61 5a
         */
    }

    public static void dumpEntries(boolean patchOrPerf, Map<Integer, Map<Integer, String>> entries, Integer bank) {
        for (int b : entries.keySet()) {
            if (bank == null || b == bank) {
                System.out.print(patchOrPerf ? "Patch" : "Perf");
                System.out.println(" Bank " + b + ":");
                Map<Integer, String> es = entries.get(b);
                for (int p : es.keySet()) {
                    System.out.format("  %02d: %s\n", p, es.get(p));
                }
            }
        }
    }


    public void initialize() throws Exception {
        readThread.start();

        // init message
        usb.sendBulk("Init", Util.asBytes(0x80)); // CMD_INIT
        readThread.expect("Init response", msg -> msg.head(0x80));


        // perf version
        usb.sendSystemCmd("perf version"
                ,0x35 // Q_VERSION_CNT
                ,0x04 // perf version??
        );
        UsbMessage perfInitMsg = readThread.expect("perf version",
                msg -> msg.head(0x82, 0x01, 0x0c, 0x40, 0x36, 0x04));
        perf = new Performance(perfInitMsg.buffer().get());

        usb.sendSystemCmd("Stop Comm"
                ,0x7d // S_START_STOP_COM
                ,0x01 // stop
        );
        readThread.expect("Stop Comm",m -> m.head(0x62,0x01));

        //synth settings
        usb.sendSystemCmd("Synth settings"
                ,0x02 // Q_SYNTH_SETTINGS
        );
        //extended: 01 0c 00 03 -- synth settings [03]
        setSynthSettings(readThread.expect("Synth settings", m -> m.head(0x01, 0x0c, 0x00, 0x03)));


    }


    private void setSynthSettings(UsbMessage msg) {
        BitBuffer bb = new BitBuffer(msg.buffer().slice());
        synthSettings = Protocol.SynthSettings.FIELDS.read(bb);
    }
}
