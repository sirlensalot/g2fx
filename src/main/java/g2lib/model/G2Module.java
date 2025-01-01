package g2lib.model;

import g2lib.Util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class G2Module extends BaseModule {

    public int horiz;
    public int vert;
    public int color;
    public int uprate;
    public boolean leds;


    public final ModuleType moduleType;


    private final List<ParamValue> modes;



    public G2Module(ModuleType type,int index) {
        super(index,type.params.size());
        this.moduleType = type;
        this.name = type.shortName;
        modes = type.modes.stream().map(ParamValue::new).toList();
        for (int i = 0; i < G2Patch.MAX_VARIATIONS; i++) {
            varParams.add(type.params.stream().map(ParamValue::new).toList());
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

    public ModuleType getModuleType() {
        return moduleType;
    }

    public Connector getInPort(int ix) {
        if (ix < moduleType.inPorts.size()) {
            return moduleType.inPorts.get(ix);
        }
        throw new IllegalArgumentException("Invalid in port: " + ix + ", module=" + name);
    }

    public Connector getOutPort(int ix) {
        if (ix < moduleType.outPorts.size()) {
            return moduleType.outPorts.get(ix);
        }
        throw new IllegalArgumentException("Invalid out port: " + ix + ", module=" + name);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Map<String, Object> toYamlObj() {
        Map<String, Object> top = super.toYamlObj();
        top.put("modes", Util.withYamlMap(m -> {
            for (ParamValue mode : modes) {
                m.put(mode.getParam().name(),mode.getValue());
            }
        }));
        return top;
    }
}
