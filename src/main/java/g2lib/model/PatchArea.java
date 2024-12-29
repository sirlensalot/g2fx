package g2lib.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PatchArea {
    public static enum AreaName {
        FX,
        Voice
    }
    private final AreaName area;
    private final Map<Integer,G2Module> modules = new TreeMap<>();
    private final List<Cable> cables = new ArrayList<>();

    public PatchArea(AreaName area) {
        this.area = area;
    }

    public void addModule(G2Module module) {
        modules.put(module.index,module);
    }

    public void addCable(Cable cable) {
        cables.add(cable);
    }

    public G2Module getModuleRequired(int ix) {
        G2Module m = modules.get(ix);
        if (m == null) {
            throw new IllegalArgumentException("Bad module index: " + ix);
        }
        return m;
    }
}
