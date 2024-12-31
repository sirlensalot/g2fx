package g2lib.model;

import java.util.*;

public class BaseModule implements ParamModule {

    private final int index;
    private final int paramCount;
    protected String name;

    protected final List<List<ParamValue>> varParams =
            new ArrayList<>(G2Patch.MAX_VARIATIONS);

    private final Map<Integer,Integer> midiControls = new TreeMap<>();

    private final Map<Integer,Boolean> knobs = new TreeMap<>();

    private final Map<Integer,String> customLabels = new TreeMap<>();

    public BaseModule(int index, int paramCount) {
        this.index = index;
        this.paramCount = paramCount;
    }

    public BaseModule(SettingsModules mod, ModParam... params) {
        this.index = mod.ordinal();
        this.name = mod.name();
        switch (mod) {
            case MorphDials:
            case MorphModes:
                paramCount = 8;
                List<String> morphs = List.of(G2Patch.MORPH_LABELS);
                for (int i = 0; i < G2Patch.MAX_VARIATIONS; i++) {
                    varParams.add(morphs.stream().map(m -> new ParamValue(new NamedParam(
                            mod == SettingsModules.MorphModes ? ModParam.MorphMode : ModParam.MorphDial,
                            m,
                            mod == SettingsModules.MorphModes ? List.of("Knob", m) : List.of()))).toList());
                }
                break;
            default:
                this.paramCount = params.length;
                List<ModParam> ps = List.of(params);
                for (int i = 0; i < G2Patch.MAX_VARIATIONS; i++) {
                    varParams.add(ps.stream().map(p ->
                            new ParamValue(new NamedParam(p, p.name(), List.of()))).toList());
                }
                break;
        }
    }

    public void setParamLabel(int ix, String s) {
        customLabels.put(ix,s);
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

    public void setParams(int variation,List<Integer> values) {
        List<ParamValue> ps = getParams(variation);
        for (int i = 0; i < ps.size(); i++) {
            getParam(ps, i).setValue(values.get(i));
        }
    }

    protected ParamValue getParam(List<ParamValue> ps, int i) {
        return ps.get(validateParam(i));
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

    @Override
    public Map<String, Object> toYamlObj() {
        Map<String, Object> top = new LinkedHashMap<>();
        top.put("name",name);
        List<Map<String,Object>> vps = new ArrayList<>();
        top.put("params",vps);
        for (List<ParamValue> pvs : varParams) {
            Map<String, Object> m = new TreeMap<>();
            vps.add(m);
            for (ParamValue pv : pvs) {
                m.put(pv.getParam().name(),pv.getValue());
            }
        }
        return top;
    }
}
