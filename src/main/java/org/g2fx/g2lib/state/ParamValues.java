package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.List;

public class ParamValues {

    private final List<FieldValues> values;
    private final List<List<LibProperty<Integer>>> props;

    public ParamValues(List<FieldValues> values) {
        this.values = values;
        props = values.stream().map(vfv ->
                Protocol.VarParams.Params.subfieldsValue(vfv).stream().map(fvs ->
                        new LibProperty<Integer>(new LibProperty.LibPropertyGetterSetter<>() {
                            @Override
                            public Integer get() {
                                return Protocol.Data7.Datum.intValue(fvs);
                            }

                            @Override
                            public void set(Integer newValue) {
                                fvs.update(Protocol.Data7.Datum.value(newValue));
                            }
                        })).toList()).toList();
    }

    public LibProperty<Integer> param(int variation,int idx) {
        List<LibProperty<Integer>> ps = props.get(validateVariation(variation));
        if (idx >= ps.size()) { throw new IllegalArgumentException("Invalid param index: " + idx); }
        return ps.get(idx);
    }

    public List<FieldValues> getValues() {
        return values;
    }

    public FieldValues getRequiredVarValues(int variation) {
        return values.get(validateVariation(variation));
    }

    private int validateVariation(int variation) {
        if (variation >= values.size()) {
            throw new IllegalArgumentException("Invalid/missing variation: " + variation);
        }
        return variation;
    }
}
