package org.g2fx.g2lib.model;

import java.util.List;

public record NamedParam(ModParam param, String name, List<String> labels, Integer userNameParam, Integer def) {

    public NamedParam(ModParam p) {
        this(p, p.name(), List.of());
    }

    public NamedParam(ModParam param, String name, List<String> labels) { this(param,name,labels,null); }

    public static NamedParam namedParamDef
            (ModParam param, String name, List<String> labels, int def) {
        return new NamedParam(param,name,labels,null,def);
    }

    public NamedParam(ModParam param, String name, List<String> labels, Integer userNameParam) {
        this(param,name,labels,userNameParam,param.def);
    }

    public NamedParam label(String... labels) {
        return new NamedParam(this.param, this.name, List.of(labels),null,def);
    }

}
