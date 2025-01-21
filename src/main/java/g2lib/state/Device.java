package g2lib.state;

import g2lib.BitBuffer;
import g2lib.protocol.Protocol;
import g2lib.Util;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Sections;
import g2lib.usb.Usb;
import g2lib.usb.UsbMessage;
import g2lib.usb.UsbReadThread;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Future;
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
            UsbMessage beMsg = readThread.expectBlocking("patch list message: " + i, m ->
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

    private Future<UsbMessage> sendSubscribe(String name, Runnable cmd, UsbReadThread.MsgP filter) {
        Future<UsbMessage> f = readThread.expect(name, filter);
        cmd.run();
        return f;
    }


    public void initialize() throws Exception {

        // usb.sendBulk("Init", Util.asBytes(0x80)); // CMD_INIT
        // // init message
        // readThread.expectBlocking("Init response", msg -> msg.head(0x80));

        sendSubscribe("Init",
                () -> usb.sendBulk("Init", Util.asBytes(0x80)),
                msg -> msg.head(0x80)
        ).get();

        // perf version
        Future <UsbMessage> future = readThread.expect("perf version",
                msg -> msg.headx(0x01, 0x0c, 0x40, 0x36, 0x04));
        usb.sendSystemCmd("perf version"
                ,0x35 // Q_VERSION_CNT
                ,0x04 // perf version??
        );
        perf = new Performance(future.get().buffer().get());

        future = readThread.expect("Stop Comm", m -> m.headx(0x01));
        usb.sendSystemCmd("Stop Comm"
                ,0x7d // S_START_STOP_COM
                ,0x01 // stop
        );
        future.get();

        //synth settings
        //extended: 01 0c 00 03 -- synth settings [03]
        future = readThread.expect("Synth settings",
                m -> m.head(0x01, 0x0c, 0x00, 0x03));
        usb.sendSystemCmd("Synth settings"
                ,0x02 // Q_SYNTH_SETTINGS
        );
        setSynthSettings(future.get());

        //unknown 1/slot init
        future = readThread.expect("slot init", m -> m.head(0x01,0x0c,0x00,0x80));
        usb.sendSystemCmd("unknown 1"
                ,0x81 // M_UNKNOWN_1
        );
        future.get();

        //perf settings
        //extended: 01 0c 00 29 -- perf settings [29 "perf name"]
        //  then chunks in TG2FilePerformance.Read
        future = readThread.expect("perf settings",
                m -> m.head(0x01, 0x0c, 0x00, 0x29));
        usb.sendSystemCmd("perf settings"
                ,0x10 // Q_PERF_SETTINGS
        );
        perf.readFromMessage(future.get().buffer().rewind().slice());

        //unknown 2
        //embedded: 72 01 0c 00 1e -- "unknown 2" [1e]
        future = readThread.expect("reserved 2", m->m.headx(0x01,0x0c,0x00,0x1e));
        usb.sendSystemCmd("unknown 2"
                ,0x59 // M_UNKNOWN_2
        );
        future.get();



        for (int slot = 0; slot < 4; slot++) {
            readSlot(slot);
        }


    }

    private void readSlot(final int slot) throws Exception {
        final int slot8 = slot + 8;
        //embedded: 82 01 0c 40 36 01 -- slot version
        Future<UsbMessage> future = readThread.expect("slot version " + slot,
                m -> m.headx(0x01, 0x0c, 0x40, 0x36, slot));
        usb.sendSystemCmd("slot version " + slot
                ,0x35 // Q_VERSION_CNT
                , slot // slot index
        );
        byte fv = future.get().buffer().get();

        //extended: 01 09 00 21 -- patch description
        future = readThread.expect("slot patch " + slot,
                m -> m.head(0x01, slot8, 0x00, 0x21));
        usb.sendSlotRequest(slot,0,"slot patch" + slot,
                0x3c // Q_PATCH
        );
        Patch patch = Patch.readFromMessage(future.get().buffer().rewind());

        //extended or embedded: 01 09 00 27 -- patch name, slot 1
        future = readThread.expect("slot name " + slot,
                m -> m.headx(0x01, slot8, 0x00, 0x27));
        usb.sendSlotRequest(slot,0,"slot name" + slot,
                0x28 // Q_PATCH_NAME
        );
        patch.readSectionSlice(new BitBuffer(future.get().buffer()),
                Sections.SPatchName);

        //extended: 01 09 00 69
        future = readThread.expect("slot note " + slot,
                m -> m.head(0x01, slot8, 0x00, 0x69));
        usb.sendSlotRequest(slot,0,"slot note" + slot,
                0x68 // Q_CURRENT_NOTE
        );
        patch.readSectionMessage(future.get(), Sections.SCurrentNote);


        //extended: 01 09 00 6f -- textpad, slot 1
        future = readThread.expect("slot text " + slot,
                m -> m.headx(0x01, slot8, 0x00, 0x6f));
        usb.sendSlotRequest(slot,0,"slot text " + slot,
                0x6e //Q_PATCH_TEXT
        );
        patch.readSectionMessage(future.get(), Sections.STextPad);


        perf.setPatch(slot,patch);

    }


    private void setSynthSettings(UsbMessage msg) {
        BitBuffer bb = new BitBuffer(msg.buffer().slice());
        synthSettings = Protocol.SynthSettings.FIELDS.read(bb);
    }
}
