package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MorphParameters {
    private final Map<Integer,List<FieldValues>> varMorphs = new TreeMap<>();

    public record MorphParam(int morph, int range) { }

    public MorphParameters(FieldValues fvs) {
        Protocol.MorphParameters.VarMorphs.subfieldsValueRequired(fvs).forEach(vm -> {
            varMorphs.put(Protocol.VarMorph.Variation.intValueRequired(vm),
                    Protocol.VarMorph.VarMorphParams.subfieldsValueRequired(vm));
        });
    }

    public MorphParam getMorphParam(int variation, AreaId area, int module, int param) {
        List<FieldValues> vm = varMorphs.get(variation);
        if (vm == null) { throw new IllegalArgumentException("Invalid variation: " + variation); }
        for (FieldValues kp : vm) {
            if (area.ordinal() == Protocol.VarMorphParam.Location.intValueRequired(kp) &&
                    module == Protocol.VarMorphParam.ModuleIndex.intValueRequired(kp) &&
                    param == Protocol.VarMorphParam.ParamIndex.intValueRequired(kp)) {
                return new MorphParam(Protocol.VarMorphParam.Morph.intValueRequired(kp),
                        Protocol.VarMorphParam.Range.intValueRequired(kp));
            }
        }
        return null;
    }
}
