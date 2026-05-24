package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

public class MorphParameters {
    private final Map<Integer,List<FieldValues>> varMorphs = new TreeMap<>();

    public record MorphParam(int morph, int range) { }

    /**
     * {@link org.g2fx.g2lib.protocol.Protocol.MorphParameters} field values
     */
    private final FieldValues fvs;

    public MorphParameters(FieldValues fvs) {
        List<FieldValues> fvss =
                new ArrayList<>(Protocol.MorphParameters.VarMorphs.subfieldsValue(fvs)); // TODO copy won't be necessary later
        for (int i = fvss.size(); i < PatchModule.MAX_VARIATIONS; i++) {
            fvss.add(mkDefaultVarMorph(i));
        }
        this.fvs = fvs.copy();
        this.fvs.update(Protocol.MorphParameters.VarMorphs.value(fvss));
        this.fvs.update(Protocol.MorphParameters.VariationCount.value(PatchModule.MAX_VARIATIONS));
        fvss.forEach(vm -> varMorphs.put(Protocol.VarMorph.Variation.intValue(vm),
                Protocol.VarMorph.VarMorphParams.subfieldsValue(vm)));
    }

    public MorphParameters() {
        this(Protocol.MorphParameters.FIELDS.values(
                Protocol.MorphParameters.VariationCount.value(0xa),
                Protocol.MorphParameters.MorphCount.value(8),
                Protocol.MorphParameters.Reserved.value(0),
                Protocol.MorphParameters.VarMorphs.value(
                        IntStream.range(0,10).mapToObj(MorphParameters::mkDefaultVarMorph).toList())));
    }

    private static FieldValues mkDefaultVarMorph(int i) {
        return Protocol.VarMorph.FIELDS.values(
                Protocol.VarMorph.Variation.value(i),
                Protocol.VarMorph.Reserved0.value(0),
                Protocol.VarMorph.Reserved1.value(0),
                Protocol.VarMorph.Reserved2.value(0),
                Protocol.VarMorph.MorphCount.value(0),
                Protocol.VarMorph.VarMorphParams.value(List.of()),
                Protocol.VarMorph.Reserved3.value(0)
        );
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

    public FieldValues getFieldValues(int variationCount) {
        if (variationCount == PatchModule.MAX_VARIATIONS) { return fvs; }
        List<FieldValues> vvs = new ArrayList<>(Protocol.MorphParameters.VarMorphs.subfieldsValue(fvs));
        vvs.removeLast();
        return fvs.copy()
                .update(Protocol.MorphParameters.VariationCount.value(variationCount))
                .update(Protocol.MorphParameters.VarMorphs.value(vvs));
    }
}
