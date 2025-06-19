package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MorphParameters {
    private final Map<Integer,List<FieldValues>> varMorphs = new TreeMap<>();

    public record MorphParam(int morph, int range) { }

    public MorphParameters(FieldValues fvs) {
        Protocol.MorphParameters.VarMorphs.subfieldsValue(fvs).forEach(vm -> {
            varMorphs.put(Protocol.VarMorph.Variation.intValue(vm),
                    Protocol.VarMorph.VarMorphParams.subfieldsValue(vm));
        });
    }

    public MorphParam getMorphParam(int variation, AreaId area, int module, int param) {
        List<FieldValues> vm = varMorphs.get(variation);
        if (vm == null) { throw new IllegalArgumentException("Invalid variation: " + variation); }
        for (FieldValues kp : vm) {
            if (area.ordinal() == Protocol.VarMorphParam.Location.intValue(kp) &&
                    module == Protocol.VarMorphParam.ModuleIndex.intValue(kp) &&
                    param == Protocol.VarMorphParam.ParamIndex.intValue(kp)) {
                return new MorphParam(Protocol.VarMorphParam.Morph.intValue(kp),
                        Protocol.VarMorphParam.Range.intValue(kp));
            }
        }
        return null;
    }
}
