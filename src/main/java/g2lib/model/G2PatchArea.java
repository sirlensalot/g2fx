package g2lib.model;

import g2lib.Util;

import java.util.*;

public class G2PatchArea<M extends ParamModule> {

    public static enum AreaName {
        FX,
        Voice,
        Settings
    }
    private final AreaName area;
    private final Map<Integer, M> modules = new TreeMap<>();
    private final List<G2Cable> cables = new ArrayList<>();

    public G2PatchArea(AreaName area) {
        this.area = area;
    }

    public G2PatchArea(List<M> settingsModules) {
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

    public Map<String,Object> toYamlObj() {
        return Util.withYamlMap(top -> {
            top.put("modules", Util.withYamlList(ms -> {
                for (ParamModule m : modules.values()) {
                    ms.add(m.toYamlObj());
                }
            }));
            if (area != AreaName.Settings) {
                top.put("cables", Util.withYamlList(cs -> {
                    for (G2Cable c : cables) {
                        cs.add(Util.withYamlMap(m -> {
                            m.put("source", c.fromMod().name);
                            m.put("sourceConn", c.fromPort().name());
                            m.put("dest", c.toMod().name);
                            m.put("destConn", c.toPort().name());
                        }));
                    }
                }));
            }
        });
    }

}
