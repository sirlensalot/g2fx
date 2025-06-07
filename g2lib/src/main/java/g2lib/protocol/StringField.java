package g2lib.protocol;

import g2lib.util.BitBuffer;
import g2lib.util.Util;

import java.util.List;
import java.util.logging.Logger;

public class StringField extends AbstractField implements Field {

    public static final int NO_TERMINATION = -1;
    private final Logger log = Util.getLogger(StringField.class);
    private final int length;
    private final boolean lengthWithTerm;

    public <T extends Enum<T>> StringField(Enum<T> e) {
        super(e);
        this.length = 0;
        this.lengthWithTerm = false;
    }
    public <T extends Enum<T>> StringField(Enum<T> e, int length) {
        super(e);
        this.length = length;
        this.lengthWithTerm = false;
    }
    public <T extends Enum<T>> StringField(Enum<T> e, int length, boolean lengthWithTerm) {
        super(e);
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
        while (bb.getBitsRemaining() >= 8) {
            if (length > 0 && i++ > length) { break; }
            int c = bb.get(8);
            if (c != 0) {
                sb.append(Character.valueOf((char) c));
            } else {
                if (lengthWithTerm || length <= 0) { break; }
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
        for (char c : value.toCharArray()) {
            if (length > 0 && i++ > length) {
                log.warning(String.format("%s: truncating string for length %d: %s",this,length,value));
                break;
            }
            bb.put(8,c & 0xff);
        }
        if (length > 0) {
            while (i++ <= length) {
                bb.put(8, 0);
            }
            return;
        }
        if (length != NO_TERMINATION) {
            bb.put(8, 0);
        }
    }
}
