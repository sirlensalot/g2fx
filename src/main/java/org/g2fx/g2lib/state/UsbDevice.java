package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.Dispatcher;
import org.g2fx.g2lib.usb.Usb;
import org.g2fx.g2lib.usb.UsbMessage;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.SafeLookup;
import org.g2fx.g2lib.util.Util;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsbDevice extends Device implements Dispatcher {
    private final Logger log = Util.getLogger(getClass());

    public static final int T_PATCH_LOAD_DATA = 0x72;
    public static final int T_TEXT_PAD = 0x6f;
    public static final int T_CURRENT_NOTE = 0x69;
    public static final int T_PATCH_NAME = 0x27;
    public static final int T_PATCH_DESCRIPTION = 0x21;
    public static final int T_SELECTED_PARAM = 0x2f;
    public static final int T_OK = 0x7f;
    public static final int T_SYNTH_SETTINGS = 0x03;
    public static final int T_PERFORMANCE_NAME = 0x29;
    public static final int T_RESERVED_1E = 0x1e;
    public static final int T_GLOBAL_KNOB_ASSIGMENTS = 0x5f;
    public static final int T_ASSIGNED_VOICES = 0x05;
    public static final int T_EXT_MASTER_CLOCK = 0x5d;
    public static final int T_ENTRY_LIST = 0x13;
    public static final int T_VOLUME_DATA = 0x3a;
    public static final int T_LED_DATA = 0x39;
    public static final int T_SET_PARAM = 0x40;

    public static final int V_VERSION = 0x40;

    public static final int R_CMD = 0x01;
    public static final int R_INIT = 0x80;

    public enum EntryType {
        Patch,
        Perf;
        public static final SafeLookup<Integer, EntryType> LOOKUP =
                SafeLookup.makeEnumOrdLookup(values());
        public static final SafeLookup<String, EntryType> LC_NAME_LOOKUP =
                SafeLookup.makeLowerCaseNameLookup(values());
    }
    public record Entry(String name,int category) { }
    public record EntryBank(int bank, int entry, List<Entry> entries) { }
    public record EntriesMsg(EntryType type,List<EntryBank> banks,boolean done) { }

    private EntriesMsg entriesMsg;

    private final Map<EntryType,Map<Integer, Map<Integer,Entry>>> entries = Map.of(
            EntryType.Patch,new TreeMap<>(),
            EntryType.Perf,new TreeMap<>()
    );

    private final Usb usb;


    public UsbDevice(Usb usb) {
        this.usb = usb;
        usb.setDispatcher(this);
    }


    public boolean online() {
        return true;
    }


    public EntriesMsg getEntriesMsg() {
        return entriesMsg;
    }


    public void dumpEntries(PrintWriter writer, EntryType type, int bank) {
        entries.get(type).forEach((bi,b) -> {
            if (bank == -1 || bi == bank) {
                writer.format("%s bank %s:\n", type, bi + 1);
                b.forEach((ei, e) ->
                        writer.format("  %02d: %s [%s]\n", ei + 1, e.name(), e.category()));
            }
        });
        writer.flush();
    }


    @Override
    public boolean dispatch(UsbMessage msg) {
        ByteBuffer buf = msg.getBufferx().slice(); //skip embedded first byte
        int h = Util.b2i(buf.get());
        return switch (h) {
            case R_CMD -> dispatchCmd(buf);
            case R_INIT -> dispatchSuccess(() -> "System Init");
            default -> dispatchFailure("dispatch: unrecognized response code: %02x",h);
        };
    }

    private boolean dispatchSuccess(Supplier<String> msg) {
        log.info(msg);
        return true;
    }

    private boolean dispatchFailure(String msg, Object... args) {
        log.warning(String.format(msg,args));
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
            return dispatchSlotCmd(Slot.fromIndex(h), buf);
        } else if (h == 4) {
            int pv = Util.b2i(buf.get());
            if (pv == perf.getVersion()) {
                return dispatchPerfCmd(buf);
            } else {
                return dispatchFailure("dispatchCmd: unrecognized perf version: " + pv);
            }
        } else {
            return dispatchFailure("dispatchCmd: unrecognized header: %02x",h);
            //2025-02-18 08:58:24.670 INFO g2lib.usb.Usb: --------------- Read Interrupt embedded, crc: 4246 4246
            //a2 01 04 00 05 01 06 01 01 42 46 00 00 00 00 00   . . . . . . . . . B F . . . . .
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
            default -> dispatchFailure("dispatchPerfCmd: unrecognized type: %02x",t);
        };
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
                buf.position(buf.position()-1);
                patch.readPatchDescription(buf);
                log.info(() -> "patch description");
                yield true;
            }
            case T_PATCH_NAME -> patch.readSectionSlice(new BitBuffer(buf.slice()), Sections.SPatchName);
            case T_CURRENT_NOTE -> patch.readSectionSlice(sliceAhead(buf), Sections.SCurrentNote);
            case T_TEXT_PAD -> patch.readSectionSlice(sliceAhead(buf), Sections.STextPad);
            case T_PATCH_LOAD_DATA -> patch.readPatchLoadData(buf);
            case T_OK -> dispatchSuccess(() -> "OK"); //TODO maybe show next byte (unknown 6...)
            case T_SELECTED_PARAM -> patch.readSelectedParam(buf);
            case T_VOLUME_DATA -> patch.readVolumeData(buf);
            case T_LED_DATA -> patch.readLedData(buf);
            case T_SET_PARAM -> patch.readParamUpdate(buf);
            default -> dispatchFailure("dispatchSlotCmd: unrecognized type: %02x",t);
        };
    }


        /*
        3a: R_VOLUME_DATA (slot cmd)
01 00 00 3a 00 00 00 00 00 00 00 00 00 00 00 07   . . . : . . . . . . . . . . . .
00 00 00 00 00 00 00 00 00 00 30 00 00 00 00 00   . . . . . . . . . . 0 . . . . .
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00   . . . . . . . . . . . . . . . .
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00   . . . . . . . . . . . . . . . .
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00   . . . . . . . . . . . . . . . .
00 00 00 00 00 6d 3d                              . . . . . m =

  R_LED_DATA              = $39; (slot)
01 00 00 39 00 00 00 00 00 15 00 00 00 00 00 b6   . . . 9 . . . . . . . . . . . .
54                                                T
         */

    /**
     * Handle 01 0c 40 36 ...
     */
    private boolean dispatchVersion(ByteBuffer buf) {
        int sc = Util.b2i(buf.get());
        return switch (sc) {
            case 0x36 -> {
                int id = Util.b2i(buf.get());
                int version = Util.b2i(buf.get());
                if (id == 4) {
                    perf.setVersion(version);
                    yield true;
                } else if (id >= 0 && id <4) { // does this also support 8-11?
                    perf.getSlot(Slot.fromIndex(id)).setVersion(version);
                    yield true;
                } else {
                    yield dispatchFailure("dispatchVersion: unrecognized id " + id);
                }
            }
            case 0x1f -> {
                perf.setVersion(buf.get());
                while ((buf.position() < buf.limit() - 2) && Util.b2i(buf.get()) == 0x36) {
                    perf.getSlot(Slot.fromIndex(buf.get())).setVersion(Util.b2i(buf.get()));
                }
                //01 0c 40 1f 00 36 00 01 36 01 01 36 02 01 36 03   . . @ . . 6 . . 6 . . 6 . . 6 .
                //01 0d 01 00 00 00 00 ea cf                        . . . . . . . . .
                yield true;
            }
            default -> dispatchFailure("dispatchVersion: unrecognized subcommand: " + sc);
        };

    }



    public void initialize() throws Exception {

        usb.sendBulk("Init", true, Util.asBytes(R_INIT));

        // perf version
        perf = new Performance();
        usb.sendSystemRequest("perf version"
                ,0x35 // Q_VERSION_CNT
                ,0x04 // perf version??
        );

        sendStartStopComm(false);

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

        //sendStartStopComm(true);

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


    public void sendStartStopComm(boolean start) throws Exception {
        usb.sendSystemRequest(start ? "Start comm" : "Stop comm"
                ,0x7d // S_START_STOP_COM
                , start ? 0x00 : 0x01
        );
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
                    bank.entries().add(new Entry(Protocol.EntryData.Name.stringValue(fvs),
                            Protocol.EntryData.Category.intValue(fvs)));
            }
        }
    }


    private void readEntries(EntryType type) throws Exception {
        entriesMsg = new EntriesMsg(type,List.of(new EntryBank(0,0,List.of())),false);
        entries.get(type).clear();
        while (!entriesMsg.done() && !entriesMsg.banks().isEmpty()) {
            EntryBank lastBank = entriesMsg.banks().getLast();
            int lastEntry = lastBank.entry() + lastBank.entries().size();
            log.info(() -> "sending entries request: " + type + ":" + lastBank.bank() + "," + lastEntry);
            entriesMsg = null;
            usb.sendSystemRequest("entries request"
                    , 0x14 // Q_LIST_NAMES
                    , type.ordinal()
                    , lastBank.bank()
                    , lastEntry
            );
            if (entriesMsg == null) {
                throw new IllegalStateException("Did not receive entries message!");
            }
            log.info(() -> "received entry data: " + entriesMsg);
            entriesMsg.banks().forEach(bank -> {
                Map<Integer, Entry> bm = entries.get(type).computeIfAbsent(bank.bank(),b -> new TreeMap<>());
                int i = bank.entry();
                for (Entry e : bank.entries()) {
                    bm.put(i++,e);
                }
            });
        }
        log.info(() -> "readEntries: received " + entries.get(type).size() + " banks");

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


    public void shutdown(boolean sendStopComms) {
        if (sendStopComms) {
            try {
                sendStartStopComm(false);
            } catch (Exception e) {
                log.log(Level.SEVERE, "could not send stop message", e);
            }
        }
        usb.shutdown();
    }

    private BitBuffer sliceAhead(ByteBuffer buf) {
        return BitBuffer.sliceAhead(buf, Util.getShort(buf));
    }


    public void loadEntry(int slotCode, int bank, int entry) throws Exception {
        log.info(String.format("loadEntry: slot=%s, bank=%s, entry=%s",slotCode,bank,entry));
        usb.sendSystemRequest("loadEntry",
                0x0a, //S_RETREIVE
                slotCode,
                bank,
                entry
        );
        //TODO initialize() if perf, readSlot() if slot
    }
}
