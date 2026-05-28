package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
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

import static org.g2fx.g2lib.state.Device.dispatchSuccess;

public class Entries {

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

    public record EntryMsg(EntryType type, int bank, int entry) {}

    public record EntriesEvent(
            EntriesEventType type,
            Map<EntryType,Map<Integer, Map<Integer,Entry>>> entries,
            EntryMsg msg) {
        public static EntriesEvent deleteBank(EntryType type, int bank) {
            return new EntriesEvent(EntriesEventType.DeleteBank,null,new EntryMsg(type,bank,-1));
        }
        public static EntriesEvent deleteEntry(EntryType type, int bank, int entry) {
            return new EntriesEvent(EntriesEventType.DeleteEntry,null,new EntryMsg(type,bank,entry));
        }
        public static EntriesEvent saveEntry(EntryType type, int bank, int entry) {
            return new EntriesEvent(EntriesEventType.SaveEntry,null,new EntryMsg(type,bank,entry));
        }
        public static EntriesEvent loadEntry(EntryType type, int bank, int entry) {
            return new EntriesEvent(EntriesEventType.LoadEntry,null,new EntryMsg(type,bank,entry));
        }
        public static EntriesEvent refreshAll(Map<EntryType,Map<Integer, Map<Integer,Entry>>> entries) {
            return new EntriesEvent(EntriesEventType.RefreshAll,entries,null);
        }

    }

    private EntriesMsg entriesMsg;
    private final UsbSender usb;

    private Map<EntryType,Map<Integer, Map<Integer,Entry>>> entries;
    private LibProperty<EntriesEvent> eventProp = new LibProperty<>(EntriesEvent.refreshAll(Map.of()));

    public Entries(UsbSender usb) {
        this.usb = usb;
    }

    public EntriesMsg getEntriesMsg() {
        return entriesMsg;
    }

    public void readEntries() throws Exception {
        entries = Map.of(EntryType.Performance,new HashMap<>(),EntryType.Patch,new HashMap<>());
        readEntries(EntryType.Performance);
        readEntries(EntryType.Patch);
        eventProp.set(EntriesEvent.refreshAll(entries));
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
}
