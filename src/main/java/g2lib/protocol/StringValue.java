package g2lib.protocol;

import g2lib.util.BitBuffer;

public record StringValue(StringField field, String value) implements FieldValue {

    @Override
    public String toString() {
        return field.name() + ": " + value;
    }

    public static String stringValue(FieldValue f) {
        if (f instanceof StringValue) {
            return ((StringValue) f).value();
        }
        throw new UnsupportedOperationException("Not StringValue: " + f);
    }

    @Override
    public void write(BitBuffer bb) throws Exception {
        field.write(bb,value);
    }
}
