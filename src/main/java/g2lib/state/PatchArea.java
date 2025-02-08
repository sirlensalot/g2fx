package g2lib.state;

import g2lib.model.SettingsModules;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;
import g2lib.util.Util;

import java.util.*;
import java.util.logging.Logger;

public class PatchArea {

    public final AreaId id;
    private final Logger log;

    private final Map<Integer,PatchModule> modules = new TreeMap<>();
    private final List<PatchCable> cables = new ArrayList<>();
    private PatchLoadData patchLoadData;

    public PatchArea(AreaId id) {
        this.id = id;
        this.log = Util.getLogger(getClass().getName() + "_" + id);
    }

    public PatchArea() {
        this.id = AreaId.Settings;
        this.log = Util.getLogger(getClass().getName() + "_" + id);
        Arrays.stream(SettingsModules.values()).forEach(sm -> {
            PatchModule m = new PatchModule(sm);
            modules.put(m.getIndex(),m);
        });
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

    public PatchModule getSettingsModule(SettingsModules m) {
        return getModuleRequired(m.ordinal());
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

    public void setModuleLabels(FieldValues fv) {
        Protocol.ModuleLabels.ModLabels.subfieldsValueRequired(fv).forEach(ml -> {
            getModuleRequired(Protocol.ModuleLabel.ModuleIndex.intValueRequired(ml))
                    .setUserLabels(Protocol.ModuleLabel.Labels.subfieldsValueRequired(ml));
        });
    }

    public void setModuleNames(FieldValues fv) {
        Protocol.ModuleNames.Names.subfieldsValueRequired(fv).forEach(mn -> {
            getModuleRequired(Protocol.ModuleName.ModuleIndex.intValueRequired(mn))
                            .setModuleName(mn);
        });
    }

    public void setMorphLabels(FieldValues values) {
        getSettingsModule(SettingsModules.MorphModes).setMorphLabels(values);
    }

    public void setPatchLoadData(FieldValues fvs) {
        this.patchLoadData = new PatchLoadData(fvs);
        log.fine(() -> "setPatchLoadData: mem=" + patchLoadData.getMem() + ", cyc=" + patchLoadData.getCycles());
    }

    public PatchLoadData getPatchLoadData() {
        return patchLoadData;
    }
}
