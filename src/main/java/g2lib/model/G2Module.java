package g2lib.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class G2Module extends BaseModule {

    public int horiz;
    public int vert;
    public int color;
    public int uprate;
    public boolean leds;

    public String name;
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


}
