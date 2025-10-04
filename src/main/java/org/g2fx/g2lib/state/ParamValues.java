package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.usb.UsbSlotSender;

import java.util.ArrayList;
import java.util.List;

import static org.g2fx.g2lib.state.PatchModule.MAX_VARIATIONS;
import static org.g2fx.g2lib.util.Util.mapWithIndex;

public class ParamValues {

    private final List<FieldValues> values;
    private final List<List<LibProperty<Integer>>> props;

    public ParamValues() {
        values = new ArrayList<>(MAX_VARIATIONS);
        props = new ArrayList<>(MAX_VARIATIONS);
        for (int v = 0 ; v < MAX_VARIATIONS; v++) {
            props.add(List.of());
            FieldValues fvs = Protocol.VarParams.FIELDS.init();
            fvs.add(Protocol.VarParams.Variation.value(v));
            fvs.add(Protocol.VarParams.Params.value(List.of()));
            values.add(fvs);
        }
    }
    public ParamValues(List<FieldValues> values, UsbSlotSender sender, AreaId area,int index) {
        this.values = values;
        props = mapWithIndex(values,(vfv,var) ->
                mapWithIndex(Protocol.VarParams.Params.subfieldsValue(vfv),(fvs,param) -> {
                    LibProperty<Integer> p = new LibProperty<>(new LibProperty.LibPropertyGetterSetter<>() {
                        @Override
                        public Integer get() {
                            return Protocol.Data7.Datum.intValue(fvs);
                        }
                        @Override
                        public void set(Integer newValue) {
                            fvs.update(Protocol.Data7.Datum.value(newValue));
                        }
                    });
                    p.addListener((o,n) ->
                            sender.sendSlotCommand("update-param",
                                    0x40, //S_SET_PARAM
                                    area.ordinal(), index, param, n, var
                            ));
                    return p;
                }));
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

    public List<Integer> getVarValues(int variation) {
        FieldValues vvs = getRequiredVarValues(variation);
        return getParamValues(vvs);
    }

    public static List<Integer> getParamValues(FieldValues vvs) {
        return Protocol.VarParams.Params.subfieldsValue(vvs)
                .stream().map(Protocol.Data7.Datum::intValue).toList();
    }

    private int validateVariation(int variation) {
        if (variation >= values.size()) {
            throw new IllegalArgumentException("Invalid/missing variation: " + variation);
        }
        return variation;
    }

    public List<List<Integer>> getAllVarValues() {
        return getValues().stream().map(ParamValues::getParamValues).toList();
    }

    public void updateParam(FieldValues fvs) {
        param(Protocol.ParamUpdate.Variation.intValue(fvs),
                Protocol.ParamUpdate.Param.intValue(fvs))
                .set(Protocol.ParamUpdate.Value.intValue(fvs));
    }
}
