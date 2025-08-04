package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SubfieldsField extends AbstractField implements Field {

    private static final Logger log = Util.getLogger(SubfieldsField.class);

    protected final Fields subfields;
    private final SubfieldCount subfieldCount;

    public interface SubfieldCount {
        int getCount(List<FieldValues> values);
    }

    public static class ConstantSubfieldCount implements SubfieldCount {
        private final int count;
        public ConstantSubfieldCount(int count) {
            this.count = count;
        }
        @Override
        public int getCount(List<FieldValues> values) {
            return count;
        }
    }

    public static SubfieldCount constant(int count) {
        return new ConstantSubfieldCount(count);
    }

    public record FieldCount(FieldEnum f) implements SubfieldCount {
        @Override
        public int getCount(List<FieldValues> values) {
            for (FieldValues fv : values) {
                Optional<FieldValue> v = fv.get(f);
                if (v.isPresent()) { return IntValue.intValue(v.get()); }
            }
            throw new NoSuchElementException(f.field().name());
        }
    }

    public static SubfieldCount fieldCount(FieldEnum e) {
        return new FieldCount(e);
    }

    public <T extends Enum<T>> SubfieldsField(Enum<T> e, Fields subfields, FieldEnum subfieldIxField) {
        this(e,subfields,fieldCount(subfieldIxField));
    }

    public <T extends Enum<T>> SubfieldsField(Enum<T> e, Fields subfields, int size) {
        this(e,subfields,constant(size));
    }

    public <T extends Enum<T>> SubfieldsField(Enum<T> e, Fields subfields, SubfieldCount subfieldCount) {
        super(e);
        this.subfields = subfields;
        this.subfieldCount = subfieldCount;
    }

    @Override
    public String toString() {
        return String.format("%s: %s",
                name(), subfields);
    }

    @Override
    public void read(BitBuffer bb, List<FieldValues> values) {
        int count = subfieldCount.getCount(values);
        List<FieldValues> vs = new ArrayList<>(count);
        readSubfields(bb, values, count, vs);
        values.getFirst().add(new SubfieldsValue(this, vs));
    }

    protected void readSubfields(BitBuffer bb, List<FieldValues> values, int count, List<FieldValues> result) {
        for (int i = 0; i < count; i++) {
            try {
                result.add(subfields.read(bb, values));
            } catch (RuntimeException e) {
                log.log(Level.SEVERE,String.format(
                        "Subfields read failure at subfield %d of %d, partial result:\n%s\n",i,count,
                        String.join("\n",result.stream().map(Object::toString).toList())
                ),e);
                throw e;
            }
        }
    }


    public void write(BitBuffer bb, List<FieldValues> value) throws Exception {
        for (FieldValues fvs : value) {
            for (FieldValue fv : fvs.values) {
                fv.write(bb);
            }
        }
    }


    @Override
    public Type type() {
        return Type.SubfieldType;
    }
}
