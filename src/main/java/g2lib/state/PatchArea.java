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

    public PatchArea() {
        this.id = AreaId.Settings;
        Arrays.stream(SettingsModules.values()).forEach(PatchModule::new);
    }

    public void addModules(FieldValues modListFvs) {
        Protocol.ModuleList.Modules.subfieldsValueRequired(modListFvs).forEach(this::addModule);
    }

    public void addModule(FieldValues fvs) {
        PatchModule m = new PatchModule(fvs);
        modules.put(m.getIndex(),m);
    }

    public PatchModule getModule(int index) {
        return modules.get(index);
    }

    public PatchModule getModuleRequired(int index) {
        PatchModule m = getModule(index);
        if (m != null) return m;
        throw new IllegalArgumentException("No such module: " + index);
    }

    public void setUserModuleParams(FieldValues moduleParams) {
        Protocol.ModuleParams.ParamSet.subfieldsValueRequired(moduleParams)
                .forEach(fvs -> getModuleRequired(
                        Protocol.ModuleParamSet.ModIndex.intValueRequired(fvs))
                        .setUserParamValues(fvs));
    }

    public void setSettingsModuleParams(FieldValues patchParams) {
        modules.values().forEach(m -> m.setSettingParamValues(patchParams));
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
