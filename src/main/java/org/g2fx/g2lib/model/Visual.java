package org.g2fx.g2lib.model;

import java.util.List;

public record Visual(VisualType type, LedGroupType groupType, List<String> names, int index) {

    public Visual(VisualType type,List<String> names) {
        this(type,LedGroupType.None,names,0);
    }

    public Visual setIndex(int index) {
        return new Visual(type,groupType,names,index);
    }

    public enum VisualType {
        Led, // single name
        LedGroup, //multiple names
        Meter // single name
    }

    public enum LedGroupType {
        None, //not a group
        Radio, //one value at a time: mux, 8counter, sequencers (but is "Sequencer" in yaml)
        ADConv, //multiple, ADConv (2s complemenet) conversion
        BinCounter, //multiple, Bin counter conversion
        Compressor //multiple, compressor conversion, called "Sequencer" in yaml
    }

}
