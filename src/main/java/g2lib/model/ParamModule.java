package g2lib.model;

import java.util.List;
import java.util.Map;

public interface ParamModule {
    int getIndex();

    void assignMidiControl(int param, int cc);

    void assignKnob(int param, boolean isLed);

    List<ParamValue> getParams(int variation);

    Map<String, Object> toYamlObj();
}
