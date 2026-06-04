package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.Codes;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.usb.UsbSender;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.SafeLookup;
import org.g2fx.g2lib.util.Util;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Logger;

import static org.g2fx.g2lib.device.Device.dispatchSuccess;
import static org.g2fx.g2lib.protocol.Codes.O_LOAD_ENTRY;

public class Entries implements LibProperty.LibPropertyListener<Entries.EntriesEvent> {

    private static final Logger log = Util.getLogger(Entries.class);

    public enum EntryType {
        Patch(32),
        Performance(8);
        public static final SafeLookup<Integer, EntryType> LOOKUP =
                SafeLookup.makeEnumOrdLookup(values());
        public static final SafeLookup<String, EntryType> LC_NAME_LOOKUP =
                SafeLookup.makeLowerCaseNameLookup(values());
        private final int banks;
        EntryType(int banks) {
            this.banks = banks;
        }
        public int getBanks() {
            return banks;
        }
    }
    public record Entry(String name,int category) { }
    public record EntryBank(int bank, int entry, List<Entry> entries) { }
    public record EntriesMsg(EntryType type,List<EntryBank> banks,boolean done) { }

    public enum EntriesEventType {
        RefreshAll,
        DeleteBank,
        DeleteEntry,
        SaveEntry,
        LoadEntry
    }

    public record EntryMsg(EntryType type, int bank, int entry, Slot slot) {}

    public record EntriesEvent(
            EntriesEventType type,
            Map<EntryType,Map<Integer, Map<Integer,Entry>>> entries,
            EntryMsg msg,
            long time) {
        public EntriesEvent(EntriesEventType type,
                            Map<EntryType,Map<Integer, Map<Integer,Entry>>> entries,
                            EntryMsg msg) {
            this(type,entries,msg,System.currentTimeMillis());
        }
        public static EntriesEvent deleteBank(EntryType type, int bank) {
            return new EntriesEvent(EntriesEventType.DeleteBank,null,new EntryMsg(type,bank,-1,null));
        }
        public static EntriesEvent deleteEntry(EntryType type, int bank, int entry) {
            return new EntriesEvent(EntriesEventType.DeleteEntry,null,new EntryMsg(type,bank,entry,null));
        }
        public static EntriesEvent saveEntry(EntryType type, int bank, int entry) {
            return new EntriesEvent(EntriesEventType.SaveEntry,null,new EntryMsg(type,bank,entry,null));
        }
        public static EntriesEvent loadEntry(EntryType type, int bank, int entry, Slot slot) {
            return new EntriesEvent(EntriesEventType.LoadEntry,null,new EntryMsg(type,bank,entry,slot));
        }
        public static EntriesEvent refreshAll(Map<EntryType,Map<Integer, Map<Integer,Entry>>> entries) {
            return new EntriesEvent(EntriesEventType.RefreshAll,entries,null);
        }

    }

    private EntriesMsg entriesMsg;
    private final UsbSender usb;

    private Map<EntryType,Map<Integer, Map<Integer,Entry>>> entries;
    //TODO undo should not be supported here ...
    private LibProperty<EntriesEvent> eventProp = new LibProperty<>(EntriesEvent.refreshAll(Map.of()));

    public Entries(UsbSender usb) {
        this.usb = usb;
        resetEntries();
        eventProp.addListener(this);
    }

    public EntriesMsg getEntriesMsg() {
        return entriesMsg;
    }

    public void readEntries() throws Exception {
        resetEntries();
        readEntries(EntryType.Performance);
        readEntries(EntryType.Patch);
    }

    private void resetEntries() {
        entries = Map.of(EntryType.Performance,new HashMap<>(),EntryType.Patch,new HashMap<>());
    }

    public void fireRefreshAll() {
        //deep hashmap copy
        Map<EntryType,Map<Integer, Map<Integer,Entry>>> m = new HashMap<>();
        for (Map.Entry<EntryType, Map<Integer, Map<Integer, Entry>>> em1 : entries.entrySet()) {
            m.put(em1.getKey(), new TreeMap<>());
            for (Map.Entry<Integer, Map<Integer, Entry>> em2 : em1.getValue().entrySet()) {
                m.get(em1.getKey()).put(em2.getKey(),new TreeMap<>());
                for (Map.Entry<Integer, Entry> em3 : em2.getValue().entrySet()) {
                    m.get(em1.getKey()).get(em2.getKey()).put(em3.getKey(),em3.getValue());
                }
            }
        }
        eventProp.set(EntriesEvent.refreshAll(m));
    }

    public void readEntries(EntryType type) throws Exception {
        entriesMsg = new EntriesMsg(type,List.of(new EntryBank(0,0,List.of())),false);
        entries.get(type).clear();
        while (!entriesMsg.done() && !entriesMsg.banks().isEmpty()) {
            EntryBank lastBank = entriesMsg.banks().getLast();
            int lastEntry = lastBank.entry() + lastBank.entries().size();
            log.info(() -> "sending entries request: " + type + ":" + lastBank.bank() + "," + lastEntry);
            entriesMsg = null;
            usb.sendSystemRequest("entries request"
                    , Codes.O_LIST_NAMES // Q_LIST_NAMES
                    , type.ordinal()
                    , lastBank.bank()
                    , lastEntry
            );
            if (entriesMsg == null) {
                throw new IllegalStateException("Did not receive entries message!");
            }
            log.info(() -> "received entry data: " + entriesMsg);
        }
        log.info(() -> "readEntries: received " + entries.get(type).size() + " banks");

    }

    public void processEntriesMsg(boolean isStoreResponse) {
        log.info("processEntriesMsg: " + entriesMsg);
        entriesMsg.banks().forEach(bank -> {
            Map<Integer, Entry> bm = entries.get(entriesMsg.type).computeIfAbsent(bank.bank(), b -> new TreeMap<>());
            int i = bank.entry();
            for (Entry e : bank.entries()) {
                bm.put(i++,e);
            }
        });
        if (entriesMsg.done || isStoreResponse) { // <-- what about large banks, multiple post-store messages?
            fireRefreshAll();
        }
    }


    public boolean dispatchEntryList(ByteBuffer buf) {
        buf.position(3); //xx xx 16
        boolean isStoreResponse = buf.get() == 0;
        BitBuffer bb = new BitBuffer(buf.slice());
        EntryType type = EntryType.LOOKUP.get(bb.get());
        List<EntryBank> banks = new ArrayList<>();
        log.info(() -> "dispatchEntryList: " + type + ": " + Util.dumpBufferString(buf));
        EntryBank bank = null;
        while (true) {
            switch (bb.peek(8)) {
                case 0x01:
                    bb.get();
                    banks.add(bank = new EntryBank(bank.bank,bb.get(),new ArrayList<>()));
                    log.info("dispatchEntryList: index jump: " + bank);
                    break;
                case 0x02:
                    bb.get();
                    banks.add(bank = new EntryBank(bank.bank,bank.entry+bank.entries.size()+1,new ArrayList<>()));
                    log.info("dispatchEntryList: empty entry: " + bank);
                    break;
                case 0x03:
                    bb.get();
                    banks.add(bank = new EntryBank(bb.get(),bb.get(),new ArrayList<>()));
                    log.info("dispatchEntryList: new bank: " + bank);
                    break;
                case 0x04:
                case 0x05:
                    entriesMsg = new EntriesMsg(type,banks,bb.get() == 0x04);
                    processEntriesMsg(isStoreResponse);
                    return dispatchSuccess(() -> "dispatchEntryList: terminate: " + entriesMsg.done());
                default:
                    if (bank == null) { throw new IllegalStateException("invalid message, no current bank"); }
                    FieldValues fvs = Protocol.EntryData.FIELDS.read(bb);
                    bank.entries().add(new Entry(Protocol.EntryData.Name.stringValue(fvs),
                            Protocol.EntryData.Category.intValue(fvs)));
            }
        }
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

    public LibProperty<EntriesEvent> getEventProp() {
        return eventProp;
    }

    @Override
    public void propertyChanged(EntriesEvent o, EntriesEvent e) throws Exception {
        switch (e.type) {
            case LoadEntry -> loadEntry(e);
            case SaveEntry -> saveEntry(e);
        }
    }

    private void saveEntry(EntriesEvent e) {
    }

    private void loadEntry(EntriesEvent e) throws Exception {
        loadEntry(e.msg.type==EntryType.Performance ? Codes.S_PERF_04 : e.msg.slot.ordinal(),
            e.msg.bank,e.msg.entry);
    }


    public void loadEntry(int slotCode, int bank, int entry) throws Exception {
        log.info(String.format("loadEntry: slot=%s, bank=%s, entry=%s",slotCode,bank,entry));
        usb.sendSystemRequest("loadEntry",
                O_LOAD_ENTRY,
                slotCode,
                bank,
                entry
        );

    }
}
