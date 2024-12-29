package g2lib.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class G2Module {
    public final int index;
    public int horiz;
    public int vert;
    public int color;
    public int uprate;
    public boolean leds;

    public String name;
    public final ModuleType moduleType;

    class ParamValue {
        private final NamedParam param;
        private final Map<Integer,Integer> morphs = new TreeMap<>();
        private int value;
        ParamValue(NamedParam param) {
            this.param = param;
            this.value = param.param().def;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    private final List<ParamValue> modes;

    private final List<List<ParamValue>> varParams;

    private final Map<Integer,Integer> midiControls = new TreeMap<>();

    public record Knob(int index,boolean isLed) { }

    private final Map<Integer,Knob> knobs = new TreeMap<>();

    public G2Module(ModuleType type,int index) {
        this.moduleType = type;
        this.index = index;
        this.name = type.shortName;
        modes = type.modes.stream().map(ParamValue::new).toList();
        varParams = new ArrayList<>(G2Patch.MAX_VARIATIONS);
        for (int i = 0; i < G2Patch.MAX_VARIATIONS; i++) {
            varParams.add(type.params.stream().map(ParamValue::new).toList());
        }
    }

    public void setParams(int variation,List<Integer> values) {
        List<ParamValue> ps = getParams(variation);
        for (int i = 0; i < ps.size(); i++) {
            getParam(ps, i).setValue(values.get(i));
        }
    }

    public void setMorph(int variation,int paramIndex,int morph,int range) {
        getParam(getParams(variation),paramIndex).morphs.put(morph,range);
    }

    public void setMode(int ix,int value) {
        if (ix > modes.size()) {
            throw new IllegalArgumentException("Invalid mode: " + ix);
        }
        modes.get(ix).setValue(value);
    }

    public void assignMidiControl(int param,int cc) {
        midiControls.put(validateParam(param),cc);
    }

    public void assignKnob(int ix, int param, boolean isLed) {
        knobs.put(validateParam(param),new Knob(ix,isLed));
    }

    private ParamValue getParam(List<ParamValue> ps, int i) {
        return ps.get(validateParam(i));
    }

    private int validateParam(int param) {
        if (param >= moduleType.params.size()) {
            throw new IllegalArgumentException("Invalid param index: " + param);
        }
        return param;
    }

    private List<ParamValue> getParams(int variation) {
        if (variation >= varParams.size()) {
            throw new IllegalArgumentException("Invalid variation: " + variation);
        }
        return varParams.get(variation);
    }


}
