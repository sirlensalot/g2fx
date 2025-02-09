package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;
import g2lib.protocol.Sections;
import g2lib.repl.Repl;
import g2lib.usb.Usb;
import g2lib.usb.UsbMessage;
import g2lib.util.BitBuffer;
import g2lib.util.Util;

import java.io.File;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class Device {

    private static final Logger log = Util.getLogger(Device.class);

    private final Usb usb;

    private Performance perf;
    private SynthSettings synthSettings;

    public Device(Usb usb) {
        this.usb = usb;
    }

    public Device() {
        this.usb = null;
    }

    public Repl.Path loadPerfFile(String filePath) throws Exception {
        perf = Performance.readFromFile(filePath);
        if (online()) {
            sendPerf();
        }
        String name = new File(filePath).getName();
        String pn = name.substring(0, name.length() - 5);
        perf.setFileName(pn);
        return getPath();
    }


    private void sendPerf() throws Exception {
        //TODO
    }

    public boolean online() {
        return usb != null;
    }

    public Map<Integer, Map<Integer, String>> readEntryList(int entryCount, boolean patchOrPerf) throws Exception {
        Map<Integer, Map<Integer,String>> entries = new TreeMap<>();
        int bank = 0;
        int item = 0;
        entries.put(bank,new TreeMap<>());
        for (int i = 0; i < entryCount; i++) {

            UsbMessage beMsg = expectSystemMsg(0,"entry list",
                    0x13  // R_LIST_NAMES
                    , 0x14 // Q_LIST_NAMES
                    , patchOrPerf ? 0 : 1 // pftPatch
                    , bank // bank
                    , item // item
            ).get();
            if (!beMsg.extended()) { log.info("Entry list empty: " + i); continue; }
            ByteBuffer buf = beMsg.buffer();
            buf.position(4);
            BitBuffer bb = new BitBuffer(buf.slice());
            FieldValues fvs = Protocol.BankEntries.FIELDS.read(bb);
            //log.info(fvs.toString());
            Map<Integer, String> m = entries.get(bank);
            List<FieldValues> es = Protocol.BankEntries.Entries.subfieldsValue(fvs).orElse(new ArrayList<>());
            for (FieldValues e : es) {
                //log.info(e.toString());
                int bc = Protocol.BankEntry.BankChange.intValue(e).orElse(0);
                if (bc != 0) {
                    bank = (bc & 0xff00) >> 8;
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

    }

    public static void dumpEntries(PrintWriter writer, boolean patchOrPerf, Map<Integer, Map<Integer, String>> entries, Integer bank) {
        for (int b : entries.keySet()) {
            if (bank == null || b == bank) {
                writer.print(patchOrPerf ? "Patch" : "Perf");
                writer.println(" Bank " + b + ":");
                Map<Integer, String> es = entries.get(b);
                for (int p : es.keySet()) {
                    writer.format("  %02d: %s\n", p, es.get(p));
                }
            }
        }
    }

    private Future<UsbMessage> expectSystemMsg(int pvOr40, String msg, int type, int... cdata) {
        Future<UsbMessage> f = usb.expect(msg, m -> m.headx(0x01, 0x0c,pvOr40,type));
        usb.sendSystemRequest(msg,cdata);
        return f;
    }

    public void initialize() throws Exception {

        Future<UsbMessage> f = usb.expect("Init", msg1 -> msg1.head(0x80));
        usb.sendBulk("Init", Util.asBytes(0x80));
        f.get();

        // perf version
        Future <UsbMessage> future = usb.expect("perf version",
                msg -> msg.headx(0x01, 0x0c, 0x40, 0x36, 0x04));
        usb.sendSystemRequest("perf version"
                ,0x35 // Q_VERSION_CNT
                ,0x04 // perf version??
        );
        perf = new Performance(future.get().buffer().get());

        future = usb.expect("Stop Comm", m -> m.headx(0x01));
        usb.sendSystemRequest("Stop Comm"
                ,0x7d // S_START_STOP_COM
                ,0x01 // stop
        );
        future.get();

        //synth settings
        //extended: 01 0c 00 03 -- synth settings [03]
        future = usb.expect("Synth settings",
                m -> m.head(0x01, 0x0c, 0x00, 0x03));
        usb.sendSystemRequest("Synth settings"
                ,0x02 // Q_SYNTH_SETTINGS
        );
        setSynthSettings(future.get());

        //unknown 1/slot init
        future = usb.expect("unknown 1", m -> m.head(0x01,0x0c,0x00,0x80));
        usb.sendSystemRequest("unknown 1"
                ,0x81 // M_UNKNOWN_1
        );
        future.get();

        //perf settings
        //extended: 01 0c 00 29 -- perf settings [29 "perf name"]
        //  then chunks in TG2FilePerformance.Read
        future = usb.expect("perf settings",
                m -> m.head(0x01, 0x0c, 0x00, 0x29));
        usb.sendPerfRequest(perf.getVersion(),"perf settings"
                ,0x10 // Q_PERF_SETTINGS
        );
        perf.readFromMessage(future.get().buffer().rewind().slice());

        //unknown 2
        //embedded: 72 01 0c 00 1e -- "unknown 2" [1e]
        future = usb.expect("reserved 2", m->m.headx(0x01,0x0c,0x00,0x1e));
        usb.sendPerfRequest(perf.getVersion(),"unknown 2"
                ,0x59 // M_UNKNOWN_2
        );
        future.get();



        // master clock
        //  TODO master clock can be R_EXT_MASTER_CLOCK = 0x5d or S_SET_MASTER_CLOCK = 0x3f
        // really need to move past blocking and start streaming
        // ext master clock:
        // 92 01 0c 00 5d 01 00 78 37 90 00 00 00 00 00 00
        expectSystemMsg(perf.getVersion(),"master clock",
                0x5d, //R_EXT_MASTER_CLOCK
                0x3b //Q_MASTER_CLOCK
        ).get();

        // SendGetGlobalKnobsMessage
        //01 0c 00 5f 00 11 00 78 00 00 00 00 00 00 00 00   . . . _ . . . x . . . . . . . .
        //00 00 00 00 00 00 00 85 0a                        . . . . . . . . .
        future = usb.expect("global knobs", m->m.headx(0x01,0x0c,0x00,0x5f));
        usb.sendPerfRequest(perf.getVersion(),"global knobs"
                ,0x5e //Q_GLOBAL_KNOBS
        );
        perf.readSectionMessage(future.get().buffer(),Sections.SGlobalKnobAssignments);

        for (Slot slot : Slot.values()) {
            readSlot(slot);
        }


        //assigned voices
        //a2 01 0c 00 05 01 06 01 01 d1 eb 00 00 00 00 00   . . . . . . . . . . . . . . . .
        perf.readAssignedVoices(expectSystemMsg(perf.getVersion(),"assigned voices",
                0x05, //R_ASSIGNED_VOICES
                0x04 //Q_ASSIGNED_VOICES
        ).get());


    }


    private Future<UsbMessage> expectPerfMsg(int pv, String msg, int type, int... cdata) {
        Future<UsbMessage> f = usb.expect(msg, m -> m.headx(0x01, 0x0c,pv,type));
        usb.sendPerfRequest(pv,msg,cdata);
        return f;
    }

    private void readSlot(final Slot slot) throws Exception {
        final int slot8 = slot.ordinal() + 8;
        //embedded: 82 01 0c 40 36 01 -- slot version
        Future<UsbMessage> future = usb.expect("slot version " + slot,
                m -> m.headx(0x01, 0x0c, 0x40, 0x36, slot.ordinal()));
        usb.sendSystemRequest("slot version " + slot
                ,0x35 // Q_VERSION_CNT
                , slot.ordinal() // slot index
        );
        int pv = future.get().buffer().get();

        //extended: 01 09 00 21 -- patch description
        future = usb.expect("slot patch " + slot,
                m -> m.head(0x01, slot8, pv, 0x21));
        usb.sendSlotRequest(slot,pv,"slot patch" + slot,
                0x3c // Q_PATCH
        );
        Patch patch = Patch.readFromMessage(future.get().buffer().rewind());

        //extended or embedded: 01 09 00 27 -- patch name, slot 1
        future = usb.expect("slot name " + slot,
                m -> m.headx(0x01, slot8, pv, 0x27));
        usb.sendSlotRequest(slot,pv,"slot name" + slot,
                0x28 // Q_PATCH_NAME
        );
        patch.readSectionSlice(new BitBuffer(future.get().buffer()),
                Sections.SPatchName);

        //extended: 01 09 00 69
        future = usb.expect("slot note " + slot,
                m -> m.head(0x01, slot8, pv, 0x69));
        usb.sendSlotRequest(slot,pv,"slot note" + slot,
                0x68 // Q_CURRENT_NOTE
        );
        patch.readSectionMessage(Sections.SCurrentNote, future.get());


        //extended: 01 09 00 6f -- textpad, slot 1
        future = usb.expect("slot text " + slot,
                m -> m.headx(0x01, slot8, pv, 0x6f));
        usb.sendSlotRequest(slot,pv,"slot text " + slot,
                0x6e //Q_PATCH_TEXT
        );
        patch.readSectionMessage(Sections.STextPad, future.get());

        patch.readPatchLoadDataMsg(expectSlotMsg(slot, pv, "patch load VA",
                0x72, // R_RESOURCES_USED
                0x71, // Q_RESOURCES_USED
                AreaId.Voice.ordinal() // LOCATION_VA
        ).get());

        patch.readPatchLoadDataMsg(expectSlotMsg(slot, pv, "patch load FX",
                0x72, // R_RESOURCES_USED
                0x71, // Q_RESOURCES_USED
                AreaId.Fx.ordinal() // LOCATION_VA
        ).get());

        expectSlotMsg(slot, pv, "unknown 6",
                0x7f, // R_OK
                0x70 // M_UNKNOWN_6
        ).get();


        patch.readSelectedParam(expectSlotMsg(slot, pv, "selected param",
                0x2f, // S_SEL_PARAM
                0x2e // Q_SELECTED_PARAM
        ).get());

        perf.setPatch(slot,patch);

    }

    private Future<UsbMessage> expectSlotMsg(Slot slot, int pv, String msg, int type, int... cdata) {
        Future<UsbMessage> f = usb.expect(msg, m -> m.headx(0x01,slot.ordinal()+8,pv,type));
        usb.sendSlotRequest(slot,pv,msg,cdata);
        return f;
    }


    private void setSynthSettings(UsbMessage msg) {
        BitBuffer bb = new BitBuffer(msg.buffer().slice());
        synthSettings = new SynthSettings(Protocol.SynthSettings.FIELDS.read(bb));
    }

    public SynthSettings getSynthSettings() {
        return synthSettings;
    }

    public void shutdown() {
        if (!online()) { return; }
        usb.shutdown();
    }


    public Repl.SlotPatch getSlotPatch (Slot s) {
        return new Repl.SlotPatch(s,perf.getPerfSettings().getSlotSettings(s).getPatchName());
    }

    private int getVariation() {
        return perf.getSelectedPatch().getPatchSettings().getVariation();
    }

    private Performance assertPerf() {
        if (perf == null) { throw new IllegalStateException("No current performance"); }
        return perf;
    }

    public Repl.Path getPath() {
        return new Repl.Path(online() ? "online" : "offline",
            assertPerf().getName(), getSlotPatch(assertPerf().getSelectedSlot()),getVariation(),AreaId.Voice,null,null);
    }
}
