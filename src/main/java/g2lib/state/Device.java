package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;
import g2lib.protocol.Sections;
import g2lib.repl.Repl;
import g2lib.usb.Dispatcher;
import g2lib.usb.Usb;
import g2lib.usb.UsbMessage;
import g2lib.util.BitBuffer;
import g2lib.util.SafeLookup;
import g2lib.util.Util;

import java.io.File;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class Device implements Dispatcher {

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
    public static final int T_ENTRY_LIST = 0x13;

    public enum EntryType {
        Patch,
        Perf;
        public static final SafeLookup<Integer,EntryType> LOOKUP = SafeLookup.makeEnumOrdLookup(values());
    }
    public record Entry(String name,int category) { }
    public record EntryBank(int bank, int entry, List<Entry> entries) { }
    public record EntriesMsg(EntryType type,List<EntryBank> banks,boolean done) { }

    private EntriesMsg entriesMsg;

    private final Usb usb;
    private final Thread thread;

    private Performance perf;
    private SynthSettings synthSettings;

    private final Map<EntryType,Map<Integer,Map<Integer,Entry>>> entries = Map.of(
        EntryType.Patch,new TreeMap<>(),
        EntryType.Perf,new TreeMap<>()
    );

    public Device(Usb usb) {
        this.usb = usb;
        usb.setDispatcher(this);
        this.thread = Thread.currentThread();
    }

    public Device() {
        this.usb = null;
        this.thread = Thread.currentThread();
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

    public EntriesMsg getEntriesMsg() {
        return entriesMsg;
    }

    private void sendPerf() throws Exception {
        //TODO
    }

    public boolean online() {
        return usb != null;
    }


    public void dumpEntries(PrintWriter writer, EntryType type) {
        entries.get(type).forEach((bi,b) -> {
            writer.format("%s bank %s:\n",type,bi+1);
            b.forEach((ei,e) -> {
                writer.format("  %02d: %s [%s]\n", ei+1, e.name(), e.category());
            });
        });
        writer.flush();
    }


    private int poll() throws Exception {
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

    @Override
    public boolean dispatch(UsbMessage msg) {
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

    /**
     * Handle 01 ...
     */
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

    /**
     * Handle 01 0c [perfVersion (00)] ...
     */
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
            case T_ENTRY_LIST -> dispatchEntryList(buf.slice());
            default -> dispatchFailure("dispatchPerfCmd: unrecognized type: " + t);
        };
    }



    public boolean dispatchEntryList(ByteBuffer buf) {
        buf.position(4); //skip constant header fields
        BitBuffer bb = new BitBuffer(buf.slice());
        EntryType type = EntryType.LOOKUP.get(bb.get());
        List<EntryBank> banks = new ArrayList<>();
        EntryBank bank = null;
        while (true) {
            switch (bb.peek(8)) {
                case 0x03:
                    bb.get();
                    banks.add(bank = new EntryBank(bb.get(),bb.get(),new ArrayList<>()));
                    break;
                case 0x04:
                case 0x05:
                    entriesMsg = new EntriesMsg(type,banks,bb.get() == 0x04);
                    return dispatchSuccess(() -> "dispatchEntryList: terminate: " + entriesMsg.done());
                default:
                    if (bank == null) { throw new IllegalStateException("invalid message, no current bank"); }
                    FieldValues fvs = Protocol.EntryData.FIELDS.read(bb);
                    bank.entries().add(new Entry(Protocol.EntryData.Name.stringValueRequired(fvs),
                            Protocol.EntryData.Category.intValueRequired(fvs)));
            }
        }
    }


    private BitBuffer sliceAhead(ByteBuffer buf) {
        return BitBuffer.sliceAhead(buf, Util.getShort(buf));
    }

    /**
     * Handle 01 [slot] [slot version] ...
     */
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
            case T_PATCH_NAME -> patch.readSectionSlice(new BitBuffer(buf.slice()), Sections.SPatchName);
            case T_CURRENT_NOTE -> patch.readSectionSlice(sliceAhead(buf), Sections.SCurrentNote);
            case T_TEXT_PAD -> patch.readSectionSlice(sliceAhead(buf), Sections.STextPad);
            case T_PATCH_LOAD_DATA -> patch.readPatchLoadData(buf);
            case T_OK -> dispatchSuccess(() -> "OK"); //TODO maybe show next byte (unknown 6...)
            case T_SELECTED_PARAM -> patch.readSelectedParam(buf);
            default -> dispatchFailure("dispatchSlotCmd: unrecognized type: " + t);
        };
    }

    /**
     * Handle 01 0c 40 36 ...
     */
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

    public void initialize() throws Exception {

        usb.sendBulk("Init", true, Util.asBytes(R_INIT));

        // perf version
        perf = new Performance();
        usb.sendSystemRequest("perf version"
                ,0x35 // Q_VERSION_CNT
                ,0x04 // perf version??
        );

        usb.sendSystemRequest("Stop Comm"
                ,0x7d // S_START_STOP_COM
                , 0x01 // stop
        );

        //synth settings
        usb.sendSystemRequest("Synth settings"
                ,0x02 // Q_SYNTH_SETTINGS
        );

        usb.sendSystemRequest("unknown 1"
                ,0x81 // M_UNKNOWN_1
        );

        usb.sendPerfRequest(perf.getVersion(),"perf settings"
                ,0x10 // Q_PERF_SETTINGS
        );

        usb.sendPerfRequest(perf.getVersion(),"unknown 2"
                ,0x59 // M_UNKNOWN_2
        );



        // master clock
        //  TODO master clock can be R_EXT_MASTER_CLOCK = 0x5d or S_SET_MASTER_CLOCK = 0x3f
        // ext master clock:
        // 92 01 0c 00 5d 01 00 78 37 90 00 00 00 00 00 00
        usb.sendSystemRequest("master clock",
                0x3b //Q_MASTER_CLOCK
        );

        usb.sendPerfRequest(perf.getVersion(),"global knobs",
                0x5e //Q_GLOBAL_KNOBS
        );

        for (Slot slot : Slot.values()) {
            readSlot(slot);
        }

        usb.sendSystemRequest("assigned voices",
                0x04 //Q_ASSIGNED_VOICES
                );

        readEntries(EntryType.Patch);
        readEntries(EntryType.Perf);
    }

    private void readEntries(EntryType type) throws Exception {
        entriesMsg = new EntriesMsg(type,List.of(new EntryBank(0,0,List.of())),false);
        entries.get(type).clear();
        while (!entriesMsg.done() && !entriesMsg.banks().isEmpty()) {
            EntryBank lastBank = entriesMsg.banks().getLast();
            int lastEntry = lastBank.entry() + lastBank.entries().size();
            log.fine(() -> "sending entry request: " + type + ":" + lastBank.bank() + "," + lastEntry);
            entriesMsg = null;
            usb.sendSystemRequest("patch entry"
                    , 0x14 // Q_LIST_NAMES
                    , type.ordinal()
                    , lastBank.bank()
                    , lastEntry
            );
            if (entriesMsg == null) {
                throw new IllegalStateException("Did not receive entries message!");
            }
            log.fine(() -> "received entry data: " + entriesMsg);
            entriesMsg.banks().forEach(bank -> {
                Map<Integer, Entry> bm = entries.get(type).computeIfAbsent(bank.bank(),b -> new TreeMap<>());
                int i = bank.entry();
                for (Entry e : bank.entries()) {
                    bm.put(i++,e);
                }
            });
        }
        log.fine(() -> "readEntries: received " + entries.get(type).size() + " banks");

    }



    private void readSlot(final Slot slot) throws Exception {

        usb.sendSystemRequest("slot version " + slot
                ,0x35 // Q_VERSION_CNT
                , slot.ordinal() // slot index
        );
        Patch patch = perf.getSlot(slot);
        int pv = patch.getVersion();

        usb.sendSlotRequest(slot,pv,"slot patch" + slot,
                0x3c // Q_PATCH
        );

        usb.sendSlotRequest(slot,pv,"slot name" + slot,
                0x28 // Q_PATCH_NAME
        );

        usb.sendSlotRequest(slot,pv,"slot note" + slot,
                0x68 // Q_CURRENT_NOTE
        );

        usb.sendSlotRequest(slot,pv,"slot text " + slot,
                0x6e //Q_PATCH_TEXT
        );

        usb.sendSlotRequest(slot,pv,"patch load VA",
                0x71, // Q_RESOURCES_USED
                AreaId.Voice.ordinal() // LOCATION_VA
        );

        usb.sendSlotRequest(slot,pv,"patch load FX",
                0x71, // Q_RESOURCES_USED
                AreaId.Fx.ordinal() // LOCATION_VA
        );

        usb.sendSlotRequest(slot,pv,"unknown 6",
                0x70 // M_UNKNOWN_6
        );

        usb.sendSlotRequest(slot,pv,"selected param",
                0x2e // Q_SELECTED_PARAM
        );

        perf.setPatch(slot,patch);

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
