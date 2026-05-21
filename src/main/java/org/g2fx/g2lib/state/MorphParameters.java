package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

public class MorphParameters {
    private final Map<Integer,List<FieldValues>> varMorphs = new TreeMap<>();

    public record MorphParam(int morph, int range) { }

    private final FieldValues fvs;

    public MorphParameters(FieldValues fvs) {
        this.fvs = fvs;
        Protocol.MorphParameters.VarMorphs.subfieldsValue(fvs).forEach(vm -> {
            varMorphs.put(Protocol.VarMorph.Variation.intValue(vm),
                    Protocol.VarMorph.VarMorphParams.subfieldsValue(vm));
        });
    }

    public MorphParameters() {
        this(Protocol.MorphParameters.FIELDS.values(
                Protocol.MorphParameters.VariationCount.value(0xa),
                Protocol.MorphParameters.MorphCount.value(8),
                Protocol.MorphParameters.Reserved.value(0),
                Protocol.MorphParameters.VarMorphs.value(
                        IntStream.range(0,9).mapToObj(i ->
                                Protocol.VarMorph.FIELDS.values(
                                        Protocol.VarMorph.Variation.value(i),
                                        Protocol.VarMorph.Reserved0.value(0),
                                        Protocol.VarMorph.Reserved1.value(0),
                                        Protocol.VarMorph.Reserved2.value(0),
                                        Protocol.VarMorph.MorphCount.value(0),
                                        Protocol.VarMorph.VarMorphParams.value(List.of()),
                                        Protocol.VarMorph.Reserved3.value(0)
                                )).toList())));
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

    public Map<Integer, List<FieldValues>> getVarMorphs() {
        return varMorphs;
    }

    public FieldValues getFieldValues() {
        return fvs;
    }
}
