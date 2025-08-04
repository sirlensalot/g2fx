package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;

import java.util.List;
import java.util.logging.Logger;

public class StringField extends AbstractField implements Field {

    private final Logger log = Util.getLogger(StringField.class);

    /**
     * Length sentinel for read string to end of buffer.
     */
    public static final int READ_TO_EOF = -1;
    /**
     * Length sentinel for read string to 0 terminator.
     */
    public static final int READ_TO_TERMINATOR = 0;
    /**
     * Length limit, if not set to sentinel vals above.
     */
    private final int length;
    /**
     * Support terminators before hitting length limit.
     * Otherwise, expect fixed-length buffers padded with 0s.
     */
    private final boolean lengthWithTerm;

    public <T extends Enum<T>> StringField(Enum<T> e) {
        super(e);
        this.length = READ_TO_TERMINATOR;
        this.lengthWithTerm = false;
    }
    public <T extends Enum<T>> StringField(Enum<T> e, int length) {
        super(e);
        this.length = length;
        this.lengthWithTerm = false;
    }
    public <T extends Enum<T>> StringField(Enum<T> e, int length, boolean lengthWithTerm) {
        super(e);
        if (length <= 0) { throw new IllegalArgumentException("Invalid length " + length); }
        this.length = length;
        this.lengthWithTerm = lengthWithTerm;
    }
    @Override
    public String toString() {
        return String.format("%s: (String)",
                name());
    }

    @Override
    public void read(BitBuffer bb, List<FieldValues> values) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (bb.getBitsRemaining() >= 8) { // for READ_TO_EOF
            if (length > 0 && ++i > length) { break; } // enforce length limit
            int c = bb.get(8);
            if (c != 0) {
                sb.append(Character.valueOf((char) c));
            } else {
                if (lengthWithTerm || length == READ_TO_TERMINATOR) { break; } // support terminators
                //otherwise, continue for padded fixed length.
            }
        }
        values.getFirst().add(new StringValue(this, sb.toString()));
    }

    @Override
    public Type type() {
        return Type.StringType;
    }

    public void write(BitBuffer bb, String value) {
        int i = 0;
        //write all of buf
        for (char c : value.toCharArray()) {
            if (length > 0 && ++i > length) {
                log.warning(String.format("%s: truncating string for length %d: %s",this,length,value));
                break;
            }
            bb.put(8,c & 0xff);
        }
        //pad zeros if indicated
        if (length > 0 && !lengthWithTerm) {
            while (++i <= length) {
                bb.put(8, 0);
            }
            return;
        }
        //write terminator unless string goes to EOF
        if (length != READ_TO_EOF) {
            bb.put(8, 0);
        }
    }
}
