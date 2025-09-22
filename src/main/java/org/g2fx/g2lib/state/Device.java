package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.Dispatcher;
import org.g2fx.g2lib.usb.UsbMessage;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.SafeLookup;
import org.g2fx.g2lib.util.Util;

import java.io.File;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Implements an offline device. Subclass `UsbDevice` has all transmission functionality,
 * but dispatching here which is offline-compatible is nice for simulation/scripting.
 */
public class Device implements Dispatcher {


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

    //09: slot change  (72) 01 04 05 09 00 31 c8 00 00 00 00 00 00 00 00
    //6a: var change   (72) 01 00 05 6a 01 b5 61 00 00 00 00 00 00 00 00
    //??: param change (b2) 01 01 05 40 01 01 00 45 01 81 ba 00 00 00 00


    private static final Logger log = Util.getLogger(Device.class);


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

    protected EntriesMsg entriesMsg;

    protected final Map<EntryType,Map<Integer, Map<Integer,Entry>>> entries = Map.of(
            EntryType.Patch,new TreeMap<>(),
            EntryType.Perf,new TreeMap<>()
    );


    protected Performance perf = new Performance();
    protected SynthSettings synthSettings = new SynthSettings();

    public Device() {}

    public Performance getPerf() {
        return perf;
    }

    public void loadPerfFile(String filePath) throws Exception {
        perf = Performance.readFromFile(filePath);
        String name = new File(filePath).getName();
        String pn = name.substring(0, name.length() - 5);
        perf.setFileName(pn);
        if (online()) {
            sendPerf();
        }
    }

    private void sendPerf() {
        //TODO
    }

    public boolean online() {
        return false;
    }


    public void sendStartStopComm(boolean start) throws Exception { }

    public void loadEntry(int slotCode, int bank, int entry) throws Exception { }


    @Override
    public boolean dispatch(UsbMessage msg) {
        try {
            ByteBuffer buf = msg.getBufferx().slice(); //skip embedded first byte
            int h = Util.b2i(buf.get());
            return switch (h) {
                case R_CMD -> dispatchCmd(buf);
                case R_INIT -> dispatchSuccess(() -> "System Init");
                default -> dispatchFailure("dispatch: unrecognized response code: %02x", h);
            };
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in dispatch of message: " + Util.dumpBufferString(msg.buffer().rewind()),e);
        }
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
        if (h == 0x0c) { // version msg in response from editor init (load?)
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
            if (pv == V_VERSION) { // version msg in response to load from keyboard, indicates a reset, plus looks like comms stop?
                return dispatchVersion(buf);
            } else if (pv == perf.getVersion()) {
                return dispatchPerfCmd(buf);
            } else {
                log.warning("Received different perf version, updating: " + pv);
                perf.setVersion(pv);
                return dispatchPerfCmd(buf);
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
            case T_GLOBAL_KNOB_ASSIGMENTS -> perf.readSectionSlice(Sections.SGlobalKnobAssignments_5f,sliceAhead(buf));
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
        byte pv = Util.expectWarn(buf, patch.getVersion(), "usb", "patch version for slot " + slot);
        patch.setVersion(Util.b2i(pv));
        int t = Util.b2i(buf.get());
        return switch (t) {
            case T_PATCH_DESCRIPTION -> {
                buf.position(buf.position()-1);
                patch.readPatchDescription(buf);
                log.info(() -> "patch description");
                yield true;
            }
            case T_PATCH_NAME -> patch.readSectionSlice(new BitBuffer(buf.slice()), Sections.SPatchName_27);
            case T_CURRENT_NOTE -> patch.readSectionSlice(sliceAhead(buf), Sections.SCurrentNote_69);
            case T_TEXT_PAD -> patch.readSectionSlice(sliceAhead(buf), Sections.STextPad_6f);
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



    // usb
    private boolean readExtMasterClock(ByteBuffer buf) {
        buf.get();
        int v = Util.getShort(buf);
        return dispatchSuccess(() -> "readExtMasterClock: " + v);
    }


    // usb
    private boolean setSynthSettings(ByteBuffer buf) {
        BitBuffer bb = new BitBuffer(buf);
        synthSettings = new SynthSettings(Protocol.SynthSettings.FIELDS.read(bb));
        return dispatchSuccess(() -> "setSynthSettings");
    }


    public boolean dispatchEntryList(ByteBuffer buf) {
        buf.position(4); //skip constant header fields
        BitBuffer bb = new BitBuffer(buf.slice());
        UsbDevice.EntryType type = UsbDevice.EntryType.LOOKUP.get(bb.get());
        List<UsbDevice.EntryBank> banks = new ArrayList<>();
        UsbDevice.EntryBank bank = null;
        while (true) {
            switch (bb.peek(8)) {
                case 0x03:
                    bb.get();
                    banks.add(bank = new UsbDevice.EntryBank(bb.get(),bb.get(),new ArrayList<>()));
                    break;
                case 0x04:
                case 0x05:
                    entriesMsg = new UsbDevice.EntriesMsg(type,banks,bb.get() == 0x04);
                    return dispatchSuccess(() -> "dispatchEntryList: terminate: " + entriesMsg.done());
                default:
                    if (bank == null) { throw new IllegalStateException("invalid message, no current bank"); }
                    FieldValues fvs = Protocol.EntryData.FIELDS.read(bb);
                    bank.entries().add(new UsbDevice.Entry(Protocol.EntryData.Name.stringValue(fvs),
                            Protocol.EntryData.Category.intValue(fvs)));
            }
        }
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




    private BitBuffer sliceAhead(ByteBuffer buf) {
        return BitBuffer.sliceAhead(buf, Util.getShort(buf));
    }






    public SynthSettings getSynthSettings() {
        return synthSettings;
    }


}
