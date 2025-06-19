package org.g2fx.g2lib.model;

import java.util.List;

public record NamedParam(ModParam param, String name, List<String> labels) {

    public NamedParam(ModParam p) {
        this(p, p.name(), List.of());
    }

    public NamedParam label(String... labels) {
        return new NamedParam(this.param, this.name, List.of(labels));
    }

}
