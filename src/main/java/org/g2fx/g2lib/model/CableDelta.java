package org.g2fx.g2lib.model;

import org.g2fx.g2lib.state.PatchCable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.g2fx.g2lib.util.Util.with;

public record CableDelta<C>(Collection<C> cables,
                            boolean add,
                            Map<Integer, Boolean> uprateChanges,
                            Map<C, Integer> colorChanges) {
    public CableDelta(Collection<C> cables, boolean add) {
        this(cables, add, new HashMap<>(), new HashMap<>());
    }

    public CableDelta() {
        this(List.of(), false);
    }

    public CableDelta<C> invert(Function<C, Integer> colorAccessor) {
        return new CableDelta<>(cables, !add,
                with(new HashMap<>(uprateChanges), m -> m.replaceAll((_, v) -> !v)),
                with(new HashMap<>(colorChanges), m -> m.replaceAll((k, _) -> colorAccessor.apply(k))));
    }

    public record CableIndex(int srcModule, int srcIndex, int destModule, int destIndex) {
        public boolean match(PatchCable c) {
            return c.getSrcConn() == srcIndex &&
                    c.getSrcModule() == srcModule &&
                    c.getDestConn() == destIndex &&
                    c.getDestModule() == destModule;
        }
    }

    public <D> CableDelta<D> convert(Function<C,D> converter) {
        Map<D,Integer> ccs = new HashMap<>();
        colorChanges.forEach((k,v)->ccs.put(converter.apply(k),v));
        return new CableDelta<>(cables.stream().map(converter).toList(),add,uprateChanges,ccs);
    }
}
