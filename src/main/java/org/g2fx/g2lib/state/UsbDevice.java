package org.g2fx.g2lib.state;

import org.g2fx.g2lib.usb.Dispatcher;
import org.g2fx.g2lib.usb.Usb;
import org.g2fx.g2lib.util.Util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Device subclass around Usb instance
 */
public class UsbDevice extends Device implements Dispatcher {

    private final Logger log = Util.getLogger(getClass());
    private final Usb usb;

    public UsbDevice(Usb usb) {
        this.usb = usb;
        usb.setDispatcher(this);
    }

    /**
     * WARNING this is only exposed for scripting, otherwise should not be directly accessed.
     */
    public Usb getUsb() {
        return usb;
    }

    public boolean online() {
        return true;
    }




    public void initialize() throws Exception {

        usb.sendBulk("Init", true, Util.asBytes(R_INIT));

        // perf version
        perf = new Performance(usb);
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
                0x0a, //S_RETREIVE
                slotCode,
                bank,
                entry
        );
        //TODO initialize() if perf, readSlot() if slot
    }
}
