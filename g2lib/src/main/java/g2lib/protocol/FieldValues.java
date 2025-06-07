package g2lib.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FieldValues {
    public final List<FieldValue> values;
    private final Fields fields;

    public FieldValues(int count, Fields fields) {
        this.values = new ArrayList<>(count);
        this.fields = fields;
    }

    public FieldValues add(FieldValue v) {
        if (!values.isEmpty()) {
            values.getLast().field().guardAdd(v.field());
        }
        values.add(v);
        return this;
    }

    public FieldValues addAll(FieldValue... vs) {
        for (FieldValue f : vs) {
            add(f);
        }
        return this;
    }

    public Optional<FieldValue> get(FieldEnum f) {
        int idx = f.ordinal();
        if (values.size() > idx) {
            FieldValue fv = values.get(idx);
            if (fv.field() == f.field()) {
                return Optional.of(fv);
            }
        }
        return Optional.empty();
    }

    public void update(FieldValue fv) {
        Field f = fv.field();
        int idx = f.ordinal();
        if (values.size() > idx && values.get(idx).field() == f) {
            values.set(idx, fv);
        } else {
            throw new IllegalArgumentException("update: field not found: " + f);
        }
    }

    @Override
    public String toString() {
        return values.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FieldValues && ((FieldValues) obj).values.equals(values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    public Fields getFields() {
        return fields;
    }
}
