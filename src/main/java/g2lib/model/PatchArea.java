package g2lib.model;

import java.util.*;

public class PatchArea<M extends ParamModule> {
    public static enum AreaName {
        FX,
        Voice,
        Settings
    }
    private final AreaName area;
    private final Map<Integer, M> modules = new TreeMap<>();
    private final List<G2Cable> cables = new ArrayList<>();

    public PatchArea(AreaName area) {
        this.area = area;
    }

    public PatchArea(List<M> settingsModules) {
        this.area = AreaName.Settings;
        settingsModules.forEach(this::addModule);
    }

    public void addModule(M module) {
        modules.put(module.getIndex(),module);
    }

    public void addCable(G2Cable cable) {
        cables.add(cable);
    }

    public M getModuleRequired(int ix) {
        M m = modules.get(ix);
        if (m == null) {
            throw new IllegalArgumentException("Bad module index: " + ix);
        }
        return m;
    }
}
