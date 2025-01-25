package g2lib.model;

import java.util.Map;
import java.util.TreeMap;

public class ParamValue {
    private final NamedParam param;
    public final Map<Integer, Integer> morphs = new TreeMap<>();
    private int value;

    public ParamValue(NamedParam param) {
        this.param = param;
        this.value = param.param().def;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public NamedParam getParam() {
        return param;
    }

    public int getValue() {
        return value;
    }
}
