package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.util.BitBuffer;

import java.util.List;

public record SubfieldsValue(SubfieldsField field, List<FieldValues> value) implements FieldValue {

    @Override
    public String toString() {
        return field.name() + ": " + value;
    }
    public static List<FieldValues> subfieldsValue(FieldValue f) {
        if (f instanceof SubfieldsValue) {
            return ((SubfieldsValue) f).value();
        }
        throw new UnsupportedOperationException("Not SubfieldsValue: " + f);
    }

    @Override
    public void write(BitBuffer bb) throws Exception {
        field.write(bb,value);
    }
}
