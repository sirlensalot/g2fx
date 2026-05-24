package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.util.BitBuffer;

import java.util.ArrayList;
import java.util.List;

public class SzContext {

    private List<FieldValues> context = new ArrayList<>();

    private List<SzRecord> records = new ArrayList<>();

    public record SzEntry(int length, int value) {}

    public static class SzRecord {
        private final Field f;
        private final int fStart;
        private int fEnd;
        private int eStart;
        private List<SzEntry> entries = new ArrayList<>();
        public SzRecord(Field f, int start) {
            this.f = f;
            this.fStart = start;
            this.eStart = start;
        }
        public void addEntry(int value, int pos) {
            int length = pos - eStart;
            eStart = pos;
            entries.add(new SzEntry(length,value));
        }
    }

    public void startField(Field f, int fStart) {
        records.add(new SzRecord(f,fStart));
    }

    public void addEntry(int value, BitBuffer bb) {
        records.getLast().addEntry(value,bb.getBitIndex());
    }

    public void addValue(FieldValue fv) {
        context.getFirst().add(fv);
    }

    public void endField(Field f, int bitIndex) {
        records.getLast().fEnd = bitIndex;
    }

    public List<FieldValues> context() {
        return context;
    }


    public void push(FieldValues fvs) {
        context.addFirst(fvs);
    }

    public FieldValues pop() {
        return context.removeFirst();
    }

    public String dumpEntries() {
        StringBuffer sb = new StringBuffer();
        int line = 0;
        int pos = 0;
        int start = records.getFirst().fStart;
        int end = records.getLast().fEnd;
        sb.append(String.format("%x-%x, %.03f bytes\n", start/8,end/8,(end-start)/8.0));
        for (SzRecord r : records) {
            boolean multiple = r.entries.size()>1;
            if (multiple) { sb.append("[ "); }
            int i = 0;
            for (SzEntry e : r.entries) {
                sb.append(Integer.toHexString(e.value)).append(" ");
                pos += e.length;
                if (++i >= r.entries.size() && multiple) {
                    sb.append("] ");
                }
                if (pos / 0x80 > line) {
                    line++;
                    sb.append("\n");
                }
            }

        }
        return sb.toString();
    }
}
