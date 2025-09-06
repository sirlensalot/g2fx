package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.Visual;

public class PatchVisual {

    private final AreaId area;
    private final PatchModule module;
    private final Visual visual;
    private final LibProperty<Integer> value = new LibProperty<>(0);

    public PatchVisual(AreaId area, PatchModule module, Visual visual) {
        this.area = area;
        this.module = module;
        this.visual = visual;
    }

    public boolean update(int value) {
        if (this.value.get() != value) {
            this.value.set(value);
            return true;
        }
        return false;
    }

    public LibProperty<Integer> value() { return value; }

    @Override
    public String toString() {
        int sz = visual.names().size();
        String ns = sz == 1 ? visual.names().getFirst() : visual.names().toString();
        return area + "." + module.name().get() + "[" + module.getIndex() + "]." + ns + "=" + value.get();
    }

    public PatchModule getModule() {
        return module;
    }

    public AreaId getArea() {
        return area;
    }

    public Visual getVisual() {
        return visual;
    }

}
