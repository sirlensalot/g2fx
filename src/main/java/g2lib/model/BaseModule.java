package g2lib.model;

import java.util.*;

public class BaseModule implements ParamModule {

    private final int index;
    private final int paramCount;

    protected final List<List<ParamValue>> varParams =
            new ArrayList<>(G2Patch.MAX_VARIATIONS);

    private final Map<Integer,Integer> midiControls = new TreeMap<>();

    private final Map<Integer,Boolean> knobs = new TreeMap<>();

    public BaseModule(int index, int paramCount) {
        this.index = index;
        this.paramCount = paramCount;
    }

    public BaseModule(int index, ModParam... params) {
        this.index = index;
        this.paramCount = params.length;
        List<ModParam> ps = List.of(params);
        for (int i = 0; i < G2Patch.MAX_VARIATIONS; i++) {
            varParams.add(ps.stream().map(p ->
                    new ParamValue(new NamedParam(p,p.name(),List.of()))).toList());
        }
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void assignMidiControl(int param, int cc) {
        midiControls.put(validateParam(param),cc);
    }

    @Override
    public void assignKnob(int param, boolean isLed) {
        knobs.put(validateParam(param),isLed);
    }

    @Override
    public List<ParamValue> getParams(int variation) {
        if (variation >= varParams.size()) {
            throw new IllegalArgumentException("Invalid variation: " + variation);
        }
        return varParams.get(variation);
    }

    protected int validateParam(int param) {
        if (param >= paramCount) {
            throw new IllegalArgumentException("Invalid param index: " + param);
        }
        return param;
    }

}
