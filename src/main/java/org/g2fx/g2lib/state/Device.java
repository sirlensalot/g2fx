package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.Dispatcher;
import org.g2fx.g2lib.usb.UsbMessage;
import org.g2fx.g2lib.usb.UsbSender;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.g2fx.g2lib.protocol.Codes.*;
import static org.g2fx.g2lib.util.BitBuffer.sliceAheadLength;

/**
 * Implements an offline device. Subclass ` has all transmission functionality,
 * but dispatching here which is offline-compatible is nice for simulation/scripting.
 */
public class Device implements Dispatcher {

    private static final Logger log = Util.getLogger(Device.class);
    private final UsbSender usb;

    private Entries entries;


    protected Performance perf;
    protected SynthSettings synthSettings = new SynthSettings();

    public Device() {
        this(new UsbSender.OfflineSender());
    }

    public Device(UsbSender usb) {
        this.usb = usb;
        perf = new Performance(usb);
        usb.setDispatcher(this);
        entries = new Entries(usb);
    }

    public Performance getPerf() {
        return perf;
    }

    public void loadPerfFile(String filePath) throws Exception {
        perf = Performance.readFromFile(filePath, usb);
        if (online()) {
            sendPerf();
        }
    }

    public UsbSender getUsb() {
        return usb;
    }

    public boolean online() {
        return usb.online();
    }


    private void sendPerf() {
        //TODO
    }


    public void initialize() throws Exception {

        usb.sendBulk("Init", true, Util.asBytes(M_INIT));

        sendStartStopComm(false); // this goes out first in poweron2

        perf = new Performance(usb);

        perf.initialize();


        entries.readEntries();

    }


    public void sendStartStopComm(boolean start) throws Exception {
        usb.sendSystemRequest(start ? "Start comm" : "Stop comm"
                , O_START_STOP_COM
                , start ? 0 : 1
        );
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

    public void loadEntry(int slotCode, int bank, int entry) throws Exception {
        log.info(String.format("loadEntry: slot=%s, bank=%s, entry=%s",slotCode,bank,entry));
        usb.sendSystemRequest("loadEntry",
                O_LOAD_ENTRY, //S_RETREIVE
                slotCode,
                bank,
                entry
        );
        //TODO initialize() if perf, readSlot() if slot
    }



    public static boolean dispatchSuccess(Supplier<String> msg) {
        log.info(msg);
        return true;
    }

    public static boolean dispatchFailure(String msg, Object... args) {
        log.warning(String.format(msg,args));
        return false;
    }

    @Override
    public boolean dispatch(UsbMessage msg) {
        try {
            ByteBuffer buf = msg.getBufferx().slice(); //skip embedded first byte
            int h = Util.b2i(buf.get());
            return switch (h) {
                case M_CMD -> dispatchCmd(buf);
                case M_INIT -> dispatchSuccess(() -> "System Init");
                default -> dispatchFailure("dispatch: unrecognized response code: %02x", h);
            };
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in dispatch of message: " + Util.dumpBufferString(msg.buffer().rewind()),e);
        }
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
        } else if (h >= S_SLOT_08 && h < S_PERF_0C) {
            return dispatchSlotCmd(Slot.fromIndex(h - 8),buf);
        } else if (h >= S_SLOT_00 && h < S_PERF_04) {
            return dispatchSlotCmd(Slot.fromIndex(h), buf);
        } else if (h == S_PERF_04) {
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
            case I_OK -> dispatchSuccess(() -> "OK");
            case I_SYNTH_SETTINGS -> setSynthSettings(buf.slice());
            case M_INIT -> dispatchSuccess(() -> "Perf Init");
            case I_PERFORMANCE_NAME -> perf.readPerformanceNameAndSettings(buf);
            case I_RESERVED_1E -> dispatchSuccess(() -> "reserved 1e");
            case I_EXT_MASTER_CLOCK -> perf.readExtMasterClock(buf);
            case I_SET_MASTER_CLOCK -> perf.setMasterClock(buf);
            case I_GLOBAL_KNOB_ASSIGMENTS -> perf.readGlobalKnobAssignments(buf);
            case I_ASSIGNED_VOICES -> perf.readAssignedVoices(buf);
            case I_ENTRY_LIST -> entries.dispatchEntryList(buf.slice());
            case I_CHANGE_SLOT -> perf.readSlotChange(buf);
            default -> dispatchFailure("dispatchPerfCmd: unrecognized type: %02x",t);
        };
    }


    /**
     * Handle 01 [slot] [slot version] ...
     */
    private boolean dispatchSlotCmd(Slot slot, ByteBuffer buf) {
        Patch patch = perf.getSlot(slot);
        patch.setVersion(Util.b2i(buf.get()));
        int t = Util.b2i(buf.get());
        return switch (t) {
            case I_PATCH_DESCRIPTION -> {
                buf.position(buf.position()-1);
                patch.readPatchDescription(buf);
                log.info(() -> "patch description");
                yield true;
            }
            case I_PATCH_NAME -> patch.readSectionSlice(new BitBuffer(buf.slice()), Sections.SPatchName_27);
            case I_CURRENT_NOTE -> patch.readSectionSlice(sliceAheadLength(buf), Sections.SCurrentNote_69);
            case I_TEXT_PAD -> patch.readSectionSlice(sliceAheadLength(buf), Sections.STextPad_6f);
            case I_PATCH_LOAD_DATA -> patch.readPatchLoadData(buf);
            case I_OK -> dispatchSuccess(() -> "OK"); //TODO maybe show next byte (unknown 6...)
            case I_SELECTED_PARAM -> patch.readSelectedParam(buf);
            case I_VOLUME_DATA -> patch.getVisuals().readVolumeData(buf);
            case I_LED_DATA -> patch.getVisuals().readLedData(buf);
            case I_SET_PARAM -> patch.readParamUpdate(buf);
            case I_CHANGE_VARIATION -> patch.readVarChange(buf);
            case I_PARAMS -> patch.readParams(buf);
            case I_PARAM_LABELS -> patch.readParamLabels(buf);
            default -> dispatchFailure("dispatchSlotCmd: unrecognized type: %02x",t);
        };
    }


        /*
        3a: O_VOLUME_DATA (slot cmd)
01 00 00 3a 00 00 00 00 00 00 00 00 00 00 00 07   . . . : . . . . . . . . . . . .
00 00 00 00 00 00 00 00 00 00 30 00 00 00 00 00   . . . . . . . . . . 0 . . . . .
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00   . . . . . . . . . . . . . . . .
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00   . . . . . . . . . . . . . . . .
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00   . . . . . . . . . . . . . . . .
00 00 00 00 00 6d 3d                              . . . . . m =

  O_LED_DATA              = $39; (slot)
01 00 00 39 00 00 00 00 00 15 00 00 00 00 00 b6   . . . 9 . . . . . . . . . . . .
54                                                T
         */

    /**
     * Handle 01 0c 40 ...
     */
    private boolean dispatchVersion(ByteBuffer buf) {
        return switch (Util.b2i(buf.get(buf.position()))) {
            case I_VERSION1 -> {
                buf.get();
                do {
                    byte s = buf.get();
                    byte v = buf.get();
                    if (s == S_PERF_04) {
                        perf.setVersion(v);
                    } else {
                        perf.getSlot(Slot.fromIndex(s)).setVersion(v);
                    }
                } while (Util.b2i(buf.get()) == I_VERSION1);
                yield true;
            }
            case I_VERSION2 -> {
                buf.get();
                perf.setVersion(buf.get());
                while ((buf.position() < buf.limit() - 2) && Util.b2i(buf.get()) == 0x36) {
                    perf.getSlot(Slot.fromIndex(buf.get())).setVersion(Util.b2i(buf.get()));
                }
                //01 0c 40 1f 00 36 00 01 36 01 01 36 02 01 36 03   . . @ . . 6 . . 6 . . 6 . . 6 .
                //01 0d 01 00 00 00 00 ea cf                        . . . . . . . . .
                yield true;
            }
            default -> dispatchFailure("dispatchVersion: unrecognized subcommand: " + buf.get(buf.position()));
        };

    }





    // usb
    private boolean setSynthSettings(ByteBuffer buf) {
        BitBuffer bb = new BitBuffer(buf);
        synthSettings = new SynthSettings(Protocol.SynthSettings.FIELDS.read(bb));
        return dispatchSuccess(() -> "setSynthSettings");
    }


    public Entries getEntries() {
        return entries;
    }

    public SynthSettings getSynthSettings() {
        return synthSettings;
    }


}
