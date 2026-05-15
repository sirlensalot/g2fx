package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.util.BitBuffer;

public abstract class AbstractField implements Field {
    public final Enum<?> enum_;

    public <T extends Enum<T>> AbstractField(Enum<T> e) {
        this.enum_ = e;
    }

    public String name() {
        return String.format("%s.%s", enum_.getDeclaringClass().getSimpleName(), enum_.name());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AbstractField && ((AbstractField) obj).enum_ == enum_;
    }

    @Override
    public int hashCode() {
        return enum_.hashCode();
    }

    public Class<?> getFieldEnumClass() {
        return enum_.getDeclaringClass();
    }

    public int ordinal() {
        return enum_.ordinal();
    }

    @Override
    public void write(FieldValue fv, BitBuffer bb, SzContext ctx) throws Exception {
        ctx.startField(fv.field(),bb.getBitIndex());
        fv.write(bb, ctx);
        ctx.endField(fv.field(),bb.getBitIndex());
    }
}
