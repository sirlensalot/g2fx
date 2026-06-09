package org.g2fx.g2lib.device;

import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.state.*;
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
 * Model a G2 USB device, dispatch inbound USB messages, act as facade
 * for bridging to lib properties.
 */
public class Device implements Dispatcher {

    private static final Logger log = Util.getLogger(Device.class);
    private final UsbSender usb;
    private final LifecycleListener<Performance> perfLoadListener;
    private final LifecycleListener<Patch> patchLoadListener;

    private final Entries entries;


    private Performance perf;
    private final SynthSettings synthSettings = new SynthSettings();

    public Device(UsbSender usb,
                  LifecycleListener<Performance> perfLoadListener,
                  LifecycleListener<Patch> patchLoadListener) {
        this.usb = usb;
        this.perfLoadListener = perfLoadListener;
        this.patchLoadListener = patchLoadListener;
        usb.setDispatcher(this);
        entries = new Entries(usb);
    }

    public Device setPerf(Performance perf) {
        this.perf = perf;
        return this;
    }


    public UsbSender getUsb() {
        return usb;
    }

    public boolean online() {
        return usb.online();
    }



    public void initialize() throws Exception {

        usb.sendBulk("Init", true, Util.asBytes(M_INIT));

        // this goes out first in poweron2
        usb.sendStartStopComm(false);

        entries.readEntries();

    }


    public void shutdown(boolean sendStopComms) {
        if (sendStopComms) {
            try {
                usb.sendStartStopComm(false);
            } catch (Exception e) {
                log.log(Level.SEVERE, "could not send stop message", e);
            }
        }
        usb.shutdown();
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
        if (h == S_PERF_0C) {
            return dispatchPerfCmd(buf);
        } else if (h >= S_SLOT_08 && h < S_PERF_0C) {
            return dispatchSlotCmd(Slot.fromIndex(h - 8),buf);
        } else if (h == S_PERF_04) {
            return dispatchPerfCmd(buf);
        } else if (h >= S_SLOT_00 && h < S_PERF_04) {
            return dispatchSlotCmd(Slot.fromIndex(h), buf);
        } else {
            return dispatchFailure("dispatchCmd: unrecognized header: %02x",h);
        }
    }

    /**
     * Handle 01 0c [perfVersion (00)] ...
     */
    private boolean dispatchPerfCmd(ByteBuffer buf) {
        int v = Util.b2i(buf.get());
        if (v == V_VERSION) {
            return dispatchPerfVersion(buf);
        }
        if (v != perf.getVersion()) {
            return dispatchFailure("dispatchPerfCmd: received perf version " + v +
                    " but currently " + perf.getVersion());
        }
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
        int v = Util.b2i(buf.get());
        if (v == V_VERSION) {
            int t = Util.b2i(buf.get());
            if (t == I_VERSION_UPDATE) {
                return readVersionUpdate(buf);
            }
            return dispatchFailure("dispatchSlotCmd: slot " + slot +
                    ", unrecognized version subcommand: " + t);
        }
        if (v != patch.getVersion()) {
            return dispatchFailure("dispatchSlotCmd: slot " + slot +
                    ", received slot version " + v + " but currently " + perf.getVersion());
        }
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


    /**
     * Handle 40 ...
     */
    private boolean dispatchPerfVersion(ByteBuffer buf) {
        int t = Util.b2i(buf.get());
        return switch (t) {
            case I_VERSION_UPDATE -> readVersionUpdate(buf);
            case I_VERSION_LOAD_PERF -> dispatchLoadPerf(buf);
            case I_VERSION_LOAD_PATCH -> dispatchLoadPatch(buf);
            default -> dispatchFailure("dispatchPerfVersion: unrecognized subcommand: " + t);
        };
    }

    private boolean readVersionUpdate(ByteBuffer buf) {
        do {
            byte s = buf.get();
            byte v = buf.get();
            if (s == S_PERF_04) {
                perf.setVersion(v);
            } else {
                perf.getSlot(Slot.fromIndex(s)).setVersion(v);
            }
        } while (Util.b2i(buf.get()) == I_VERSION_UPDATE);
        return true;
    }

    private boolean dispatchLoadPatch(ByteBuffer buf) {
        //load from usb is always post-init so perf not null
        Slot slot = Slot.fromIndex(buf.get());
        LifecycleListener.notifyLifecycleDispose(patchLoadListener,perf.getSlot(slot));
        int version = buf.get();
        try {
            perf.loadPatchFromDevice(slot,version);
        } catch (Exception e) {
            log.log(Level.SEVERE,"Failure loading patch from device",e);
            return dispatchFailure("Load patch from device failed");
        }
        LifecycleListener.notifyLifecycleInit(patchLoadListener,perf.getSlot(slot));
        return true;
    }

    private boolean dispatchLoadPerf(ByteBuffer buf) {
        //load from usb is always post-init so perf not null
        LifecycleListener.notifyLifecycleDispose(perfLoadListener,perf);
        perf = new Performance(usb);
        perf.setVersion(buf.get());
        while ((buf.position() < buf.limit() - 2) && Util.b2i(buf.get()) == 0x36) {
            perf.getSlot(Slot.fromIndex(buf.get())).setVersion(Util.b2i(buf.get()));
        }
        try {
            perf.loadFromDevice();
        } catch (Exception e) {
            log.log(Level.SEVERE,"Failure loading from device",e);
            return dispatchFailure("Load from device failed");
        }
        LifecycleListener.notifyLifecycleInit(perfLoadListener,perf);
        return true;
    }


    // usb
    private boolean setSynthSettings(ByteBuffer buf) {
        BitBuffer bb = new BitBuffer(buf);
        synthSettings.update(Protocol.SynthSettings.FIELDS.read(bb));
        return dispatchSuccess(() -> "setSynthSettings");
    }


    public Entries getEntries() {
        return entries;
    }

    public SynthSettings getSynthSettings() {
        return synthSettings;
    }


}
