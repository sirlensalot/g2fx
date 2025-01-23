package g2lib.state;

import g2lib.model.SettingsModules;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

import java.util.*;

public class PatchArea {

    public final AreaId id;

    private final Map<Integer,PatchModule> modules = new TreeMap<>();
    private final List<PatchCable> cables = new ArrayList<>();

    public PatchArea(AreaId id) {
        this.id = id;
    }

    public void addModules(FieldValues modListFvs) {
        Protocol.ModuleList.Modules.subfieldsValueRequired(modListFvs).forEach(this::addModule);
    }

    public PatchArea() {
        this.id = AreaId.Settings;
        Arrays.stream(SettingsModules.values()).forEach(PatchModule::new);
    }

    public void addModule(FieldValues fvs) {
        PatchModule m = new PatchModule(fvs);
        modules.put(m.getIndex(),m);
    }

    public PatchModule getModule(int index) {
        return modules.get(index);
    }

    public void addCable(FieldValues fvs) {
        cables.add(new PatchCable(fvs));
    }

    public void addCables(FieldValues cableListFvs) {
        Protocol.CableList.Cables.subfieldsValueRequired(cableListFvs).forEach(this::addCable);
    }

    public List<PatchCable> getCables() {
        return cables;
    }
}
