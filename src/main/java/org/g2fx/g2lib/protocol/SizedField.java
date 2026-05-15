package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.util.BitBuffer;

public class SizedField extends AbstractField implements Field {
    public final int size;

    public <T extends Enum<T>> SizedField(Enum<T> e, int size) {
        super(e);
        this.size = size;
    }


    @Override
    public String toString() {
        return String.format("%s: %d",
                name(), size);
    }

    @Override
    public void read(BitBuffer bb, SzContext values) {
        int value = readValue(bb);
        values.addEntry(value,bb);
        values.addValue(new IntValue(this, value));
    }

    protected int readValue(BitBuffer bb) {
        return bb.get(size);
    }

    @Override
    public Type type() {
        return Type.IntType;
    }
}
