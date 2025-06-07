package g2lib.protocol;

import g2lib.util.BitBuffer;

public interface FieldValue {
    Field field();
    void write(BitBuffer bb) throws Exception;
}
