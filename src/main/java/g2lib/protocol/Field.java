package g2lib.protocol;

import g2lib.util.BitBuffer;

import java.util.List;

public interface Field {
    String name();

    void read(BitBuffer bb, List<FieldValues> values);

    Class<?> getFieldEnumClass();

    int ordinal();

    public enum Type {
        IntType,
        StringType,
        SubfieldType;
    }

    Type type();

    default Field guardType(Type t) {
        if (t != type()) {
            throw new IllegalArgumentException(String.format("Field type mismatch: %s: %s", t, this));
        }
        return this;
    }

    default Field guardAdd(Field f) {
        if (f.getFieldEnumClass() != getFieldEnumClass()) {
            throw new IllegalArgumentException(String.format("Field enum class mismatch: %s: %s", f, this));
        }
        // TODO ordinality is not universal so need different check in FieldValues.add
//        if (ordinal() + 1 != f.ordinal()) {
//            throw new IllegalArgumentException(String.format("Out of order: %s: %s", f, this));
//        }
        return this;
    }

}
