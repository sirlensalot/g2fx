package g2lib.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Area {
    public static enum AreaName {
        FX,
        Voice
    }
    private final AreaName area;
    private final Map<Integer,G2Module> modules = new TreeMap<>();
    private final List<Cable> cables = new ArrayList<>();

    public Area(AreaName area) {
        this.area = area;
    }

    public void addModule(G2Module module) {
        modules.put(module.index,module);
    }

    public void addCable(Cable cable) {
        cables.add(cable);
    }
}
