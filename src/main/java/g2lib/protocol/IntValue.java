package g2lib.protocol;

import g2lib.util.BitBuffer;

public record IntValue(SizedField field, int value) implements FieldValue {

    @Override
    public String toString() {
        return field.name() + ": " + value;
    }

    public static int intValue(FieldValue f) {
        if (f instanceof IntValue) {
            return ((IntValue) f).value();
        }
        throw new UnsupportedOperationException("Not IntValue: " + f);
    }

    @Override
    public void write(BitBuffer bb) throws Exception {
        bb.put(field.size,value);
    }
}
