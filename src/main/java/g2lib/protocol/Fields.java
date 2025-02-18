package g2lib.protocol;

import g2lib.util.BitBuffer;

import java.util.ArrayList;
import java.util.List;

public class Fields {
    private final List<Field> fields;
    private final String name;

    public Fields(FieldEnum[] fieldEnums) {
        fields = new ArrayList<>();
        for (FieldEnum fieldEnum : fieldEnums) {
            fields.add(fieldEnum.field());
        }
        this.name = fieldEnums[0].getClass().getSimpleName();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name).append(":\n");
        fields.forEach(f -> sb.append(String.format("  %s\n", f)));
        return sb.toString();
    }

    public FieldValues read(BitBuffer bb) {
        return read(bb, new ArrayList<>());
    }

    public FieldValues read(BitBuffer bb, List<FieldValues> context) {
        FieldValues l = init();
        context.addFirst(l);
        fields.forEach(f -> {
            try {
                f.read(bb, context);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "readFailed, field=" + f + ", context=" + context
                        ,e);
            }
        });
        return context.removeFirst();
    }

    public FieldValues init() {
        return new FieldValues(fields.size(),this);
    }

    public FieldValues values(FieldValue... vs) {
        return init().addAll(vs);
    }
}
