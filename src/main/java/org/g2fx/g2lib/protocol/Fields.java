package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Fields {

    private static final Logger log = Util.getLogger(Fields.class);
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

    public String getName() {
        return name;
    }

    public FieldValues read(BitBuffer bb) {
        SzContext c = new SzContext();
        FieldValues fvs = read(bb, c);
        log.info(() -> fields.getFirst().getFieldEnumClass().getSimpleName() + " [READ]: " + c.dumpEntries());
        return fvs;
    }

    public FieldValues read(BitBuffer bb, SzContext context) {
        int fvsStart = bb.getBitIndex();
        FieldValues l = init();
        context.push(l);
        fields.forEach(f -> {
            int fStart = bb.getBitIndex();
            context.startField(f,fStart);
            try {
                f.read(bb, context);
            } catch (Exception e) {
                int pos = bb.getBitIndex();
                String fvsBuf = dumpBufContext(bb, fvsStart, pos);
                String fBuf = dumpBufContext(bb, fStart, pos);
                log.log(Level.SEVERE,String.format("""
                        Field %s read failure at buffer pos %s(%d)
                        Row context: %s
                        Field context: %s
                        Fields:
                        %s
                        """,
                        f.name(),pos,pos/8,fvsBuf,fBuf,l),e);
                throw new IllegalStateException(
                        "readFailed, field=" + f + ", context=" + context
                        ,e);
            }
            context.endField(f,bb.getBitIndex());
        });
        return context.pop();
    }

    private static String dumpBufContext(BitBuffer bb, int start, int pos) {
        int bl = bb.getBitLength();
        if (start >= bl) {
            return "EOF";
        }
        ByteBuffer b = bb.setBitIndex(start).shiftedSlice();
        return String.format("BitBuffer length: %d (%d bytes)\n%s\n",(bl-start),b.limit(),Util.dumpBufferString(b));
    }


    public FieldValues init() {
        return new FieldValues(fields.size(),this);
    }

    public FieldValues values(FieldValue... vs) {
        return init().addAll(vs);
    }

    public void write(BitBuffer bb, List<FieldValue> values, SzContext ctx) throws Exception {
        for (FieldValue fv : values) {
            fv.field().write(fv,bb,ctx);
        }
    }

    public void logWrite(SzContext ctx) {
        log.info(() -> fields.getFirst().getFieldEnumClass().getSimpleName() + " [WRITE]: " + ctx.dumpEntries());
    }
}
