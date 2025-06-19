package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.util.BitBuffer;

public interface FieldValue {
    Field field();
    void write(BitBuffer bb) throws Exception;
}
