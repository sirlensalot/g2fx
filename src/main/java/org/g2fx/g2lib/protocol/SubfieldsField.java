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
    private final SubfieldCounterFactory subfieldCount;

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

    public interface SubfieldCounterFactory {
        SubfieldCounter makeCounter(List<FieldValues> values);
    }

    public interface SubfieldCounter {
        boolean hasMore(List<FieldValues> values, List<FieldValues> result, int index);
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
        this(e, subfields, (SubfieldCounterFactory) values -> {
            int count = subfieldCount.getCount(values);
            return (vs, rs, i) -> i < count;
        });
    }

    public <T extends Enum<T>> SubfieldsField(Enum<T> e, Fields subfields, SubfieldCounterFactory subfieldCount) {
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
        List<FieldValues> vs = new ArrayList<>();
        readSubfields(bb, values, vs);
        values.getFirst().add(new SubfieldsValue(this, vs));
    }

    private void readSubfields(BitBuffer bb, List<FieldValues> values, List<FieldValues> result) {
        SubfieldCounter counter = subfieldCount.makeCounter(values);
        for (int i = 0; counter.hasMore(values,result,i); i++) {
            try {
                result.add(subfields.read(bb, values));
            } catch (RuntimeException e) {
                log.log(Level.SEVERE,String.format(
                        "Subfields read failure at subfield %d, partial result:\n%s\n",i,
                        String.join("\n",result.stream().map(Object::toString).toList())
                ),e);
                throw e;
            }
        }
    }


    public void write(BitBuffer bb, List<FieldValues> value) throws Exception {
        for (FieldValues fvs : value) {
            fvs.write(bb);
        }
    }


    @Override
    public Type type() {
        return Type.SubfieldType;
    }
}
