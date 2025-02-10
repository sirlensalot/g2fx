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
import java.util.function.Supplier;
import java.util.logging.Logger;

public class Device {

    private static final Logger log = Util.getLogger(Device.class);
    public static final int T_PATCH_LOAD_DATA = 0x72;
    public static final int T_TEXT_PAD = 0x6f;
    public static final int T_CURRENT_NOTE = 0x69;
    public static final int T_PATCH_NAME = 0x27;
    public static final int T_PATCH_DESCRIPTION = 0x21;
    public static final int T_SELECTED_PARAM = 0x2f;
    public static final int T_OK = 0x7f;
    public static final int V_VERSION = 0x40;
    public static final int R_CMD = 0x01;
    public static final int R_INIT = 0x80;
    public static final int T_SYNTH_SETTINGS = 0x03;
    public static final int T_PERFORMANCE_NAME = 0x29;
    public static final int T_RESERVED_1E = 0x1e;
    public static final int T_GLOBAL_KNOB_ASSIGMENTS = 0x5f;
    public static final int T_ASSIGNED_VOICES = 0x05;
    public static final int T_EXT_MASTER_CLOCK = 0x5d;

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
        String pn = name.substring(0, name.length() - T_ASSIGNED_VOICES);
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
                    , patchOrPerf ? 0 : 0x01 // pftPatch
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

    private int dispatch() throws Exception {
        int recd = 0;
        while (true) {
            long s = System.currentTimeMillis();
            UsbMessage m = usb.poll(500);
            long t = System.currentTimeMillis() - s;
            if (m == null) { return recd; }
            log.fine(() -> "dispatch: poll took " + t + " ms");
            if (dispatch(m)) {
                recd++;
            } else {
                log.warning("dispatch: unrecognized message: " + Util.dumpBufferString(m.buffer()));
            }
        }
    }

    private boolean dispatch(UsbMessage msg) {
        ByteBuffer buf = msg.getBufferx().slice(); //skip embedded first byte
        int h = Util.b2i(buf.get());
        return switch (h) {
            case R_CMD -> dispatchCmd(buf);
            case R_INIT -> dispatchSuccess(() -> "System Init");
            default -> dispatchFailure("dispatch: unrecognized response code: " + h);
        };
    }

    private boolean dispatchSuccess(Supplier<String> msg) {
        log.fine(msg);
        return true;
    }

    private boolean dispatchFailure(String msg) {
        log.warning(msg);
        return false;
    }

    private boolean dispatchCmd(ByteBuffer buf) {
        int h = Util.b2i(buf.get());
        if (h == 0x0c) {
            int v = Util.b2i(buf.get());
            if (v == V_VERSION) { // version
                return dispatchVersion(buf);
            } else if (v == perf.getVersion()) { // is this ever != 0?
                return dispatchPerfCmd(buf);
            } else {
                return dispatchFailure("dispatchCmd: unrecognized perf or sys version: " + v);
            }
        } else if (h >= 8 && h < 12) {
            return dispatchSlotCmd(Slot.fromIndex(h - 8),buf);
        } else if (h >=0 && h < 4) {
            return dispatchSlotCmd(Slot.fromIndex(h),buf);
        } else {
            return dispatchFailure("dispatchCmd: unrecognized header: " + h);
        }
    }

    private boolean dispatchPerfCmd(ByteBuffer buf) {
        int t = Util.b2i(buf.get());
        return switch (t) {
            case T_OK -> dispatchSuccess(() -> "OK");
            case T_SYNTH_SETTINGS -> setSynthSettings(buf.slice());
            case R_INIT -> dispatchSuccess(() -> "Perf Init");
            case T_PERFORMANCE_NAME -> perf.readPerformanceNameAndSettings(buf);
            case T_RESERVED_1E -> dispatchSuccess(() -> "reserved 1e");
            case T_EXT_MASTER_CLOCK -> readExtMasterClock(buf);
            case T_GLOBAL_KNOB_ASSIGMENTS -> perf.readSectionSlice(Sections.SGlobalKnobAssignments,sliceAhead(buf));
            case T_ASSIGNED_VOICES -> perf.readAssignedVoices(buf);
            default -> dispatchFailure("dispatchPerfCmd: unrecognized type: " + t);
        };
    }

    private BitBuffer sliceAhead(ByteBuffer buf) {
        return BitBuffer.sliceAhead(buf, Util.getShort(buf));
    }

    private boolean dispatchSlotCmd(Slot slot, ByteBuffer buf) {
        Patch patch = perf.getSlot(slot);
        Util.expectWarn(buf,patch.getVersion(),"usb","patch version");
        int t = Util.b2i(buf.get());
        return switch (t) {
            case T_PATCH_DESCRIPTION -> {
                buf.position(buf.position()- 0x01);
                patch.readPatchDescription(buf);
                log.fine(() -> "patch description");
                yield true;
            }
            case T_PATCH_NAME -> patch.readSectionSlice(sliceAhead(buf), Sections.SPatchName);
            case T_CURRENT_NOTE -> patch.readSectionSlice(sliceAhead(buf), Sections.SCurrentNote);
            case T_TEXT_PAD -> patch.readSectionSlice(sliceAhead(buf), Sections.STextPad);
            case T_PATCH_LOAD_DATA -> patch.readPatchLoadData(buf);
            case T_OK -> dispatchSuccess(() -> "OK"); //TODO maybe show next byte (unknown 6...)
            case T_SELECTED_PARAM -> patch.readSelectedParam(buf);
            default -> dispatchFailure("dispatchSlotCmd: unrecognized type: " + t);
        };
    }

    private boolean dispatchVersion(ByteBuffer buf) {
        Util.expectWarn(buf,0x36,"usb","Version lsb");
        int id = Util.b2i(buf.get());
        int version = Util.b2i(buf.get());
        if (id == 4) {
            perf.setVersion(version);
            return true;
        } else if (id >= 0 && id <4) { // does this also support 8-11?
            perf.getSlot(Slot.fromIndex(id)).setVersion(version);
            return true;
        } else {
            return dispatchFailure("dispatchVersion: unrecognized id " + id);
        }
    }

    private void dispatch1() throws Exception {
        int r = dispatch();
        if (r == 1) { return; }
        log.warning("dispatch1: receive count="+r);
    }

    public void initialize() throws Exception {

        usb.sendBulk("Init", Util.asBytes(R_INIT));
        dispatch1();

        // perf version
        usb.sendSystemRequest("perf version"
                ,0x35 // Q_VERSION_CNT
                ,0x04 // perf version??
        );
        perf = new Performance();
        dispatch1();

        usb.sendSystemRequest("Stop Comm"
                ,0x7d // S_START_STOP_COM
                , 0x01 // stop
        );
        dispatch1();

        //synth settings
        usb.sendSystemRequest("Synth settings"
                ,0x02 // Q_SYNTH_SETTINGS
        );
        dispatch1();

        usb.sendSystemRequest("unknown 1"
                ,0x81 // M_UNKNOWN_1
        );
        dispatch1();

        usb.sendPerfRequest(perf.getVersion(),"perf settings"
                ,0x10 // Q_PERF_SETTINGS
        );
        dispatch1();

        usb.sendPerfRequest(perf.getVersion(),"unknown 2"
                ,0x59 // M_UNKNOWN_2
        );
        dispatch1();



        // master clock
        //  TODO master clock can be R_EXT_MASTER_CLOCK = 0x5d or S_SET_MASTER_CLOCK = 0x3f
        // ext master clock:
        // 92 01 0c 00 5d 01 00 78 37 90 00 00 00 00 00 00
        usb.sendSystemRequest("master clock",
                0x3b //Q_MASTER_CLOCK
        );
        dispatch1();

        usb.sendPerfRequest(perf.getVersion(),"global knobs"
                ,0x5e //Q_GLOBAL_KNOBS
        );
        dispatch1();

        for (Slot slot : Slot.values()) {
            readSlot(slot);
        }


        usb.sendSystemRequest("assigned voices",
                0x04 //Q_ASSIGNED_VOICES
                );
        dispatch1();


    }



    private void readSlot(final Slot slot) throws Exception {
        final int slot8 = slot.ordinal() + 8;
        //embedded: 82 01 0c 40 36 01 -- slot version
        Future<UsbMessage> future; // = usb.expect("slot version " + slot,
                //m -> m.headx(0x01, 0x0c, V_VERSION, 0x36, slot.ordinal()));
        usb.sendSystemRequest("slot version " + slot
                ,0x35 // Q_VERSION_CNT
                , slot.ordinal() // slot index
        );
        //int pv = future.get().buffer().get();
        dispatch1();
        Patch patch = perf.getSlot(slot);
        int pv = patch.getVersion();

        //extended: 01 09 00 21 -- patch description
        //future = usb.expect("slot patch " + slot,
        //        m -> m.head(0x01, slot8, pv, T_PATCH_DESCRIPTION));
        usb.sendSlotRequest(slot,pv,"slot patch" + slot,
                0x3c // Q_PATCH
        );
        dispatch1();
        //patch = Patch.readFromMessage(slot,future.get().buffer().rewind());

        //extended or embedded: 01 09 00 27 -- patch name, slot 1
        future = usb.expect("slot name " + slot,
                m -> m.headx(0x01, slot8, pv, T_PATCH_NAME));
        usb.sendSlotRequest(slot,pv,"slot name" + slot,
                0x28 // Q_PATCH_NAME
        );
        patch.readSectionSlice(new BitBuffer(future.get().buffer()),
                Sections.SPatchName);

        //extended: 01 09 00 69
        future = usb.expect("slot note " + slot,
                m -> m.head(0x01, slot8, pv, T_CURRENT_NOTE));
        usb.sendSlotRequest(slot,pv,"slot note" + slot,
                0x68 // Q_CURRENT_NOTE
        );
        patch.readSectionMessage(Sections.SCurrentNote, future.get());


        //extended: 01 09 00 6f -- textpad, slot 1
        future = usb.expect("slot text " + slot,
                m -> m.headx(0x01, slot8, pv, T_TEXT_PAD));
        usb.sendSlotRequest(slot,pv,"slot text " + slot,
                0x6e //Q_PATCH_TEXT
        );
        patch.readSectionMessage(Sections.STextPad, future.get());

        patch.readPatchLoadDataMsg(expectSlotMsg(slot, pv, "patch load VA",
                T_PATCH_LOAD_DATA, // R_RESOURCES_USED
                0x71, // Q_RESOURCES_USED
                AreaId.Voice.ordinal() // LOCATION_VA
        ).get());

        patch.readPatchLoadDataMsg(expectSlotMsg(slot, pv, "patch load FX",
                T_PATCH_LOAD_DATA, // R_RESOURCES_USED
                0x71, // Q_RESOURCES_USED
                AreaId.Fx.ordinal() // LOCATION_VA
        ).get());

        expectSlotMsg(slot, pv, "unknown 6",
                T_OK, // R_OK
                0x70 // M_UNKNOWN_6
        ).get();


        patch.readSelectedParam(expectSlotMsg(slot, pv, "selected param",
                T_SELECTED_PARAM, // S_SEL_PARAM
                0x2e // Q_SELECTED_PARAM
        ).get());

        perf.setPatch(slot,patch);

    }

    private Future<UsbMessage> expectSlotMsg(Slot slot, int pv, String msg, int type, int... cdata) {
        Future<UsbMessage> f = usb.expect(msg, m -> m.headx(0x01,slot.ordinal()+8,pv,type));
        usb.sendSlotRequest(slot,pv,msg,cdata);
        return f;
    }


    private boolean readExtMasterClock(ByteBuffer buf) {
        buf.get();
        int v = Util.getShort(buf);
        return dispatchSuccess(() -> "readExtMasterClock: " + v);
    }


    private boolean setSynthSettings(ByteBuffer buf) {
        BitBuffer bb = new BitBuffer(buf);
        synthSettings = new SynthSettings(Protocol.SynthSettings.FIELDS.read(bb));
        return dispatchSuccess(() -> "setSynthSettings");
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
