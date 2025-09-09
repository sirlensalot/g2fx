package org.g2fx.g2gui.controls;

import java.util.ArrayList;
import java.util.List;

public class RebindableControls<T> {

    private List<RebindableControl<T,?>> controls = new ArrayList<>();

    public RebindableControl<T,?> add(RebindableControl<T,?> c) {
        controls.add(c);
        return c;
    }

    public void remove(List<RebindableControl<T,?>> cs) {
        controls.removeAll(cs);
    }

    public void updateBinds(T target) {
        for (RebindableControl<T, ?> control : controls) {
            control.bind(target);
        }
    }
}
