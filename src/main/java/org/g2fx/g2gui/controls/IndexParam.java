package org.g2fx.g2gui.controls;

import org.g2fx.g2lib.model.NamedParam;

public record IndexParam(NamedParam param, int index, String ctx) {

    @Override
    public String toString() {
        return ctx + ":" + index + ":" + param.param().name();
    }
}
