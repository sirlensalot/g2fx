package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.model.Visual;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.util.Util;

import java.util.*;
import java.util.logging.Logger;

public class PatchArea {

    public final AreaId id;
    private final Logger log;

    private final Map<Integer,PatchModule> modules = new TreeMap<>();
    private final List<PatchCable> cables = new ArrayList<>();
    private PatchLoadData patchLoadData = new PatchLoadData();


    public record SelectedParam(int module,int param) { }
    private SelectedParam selectedParam;

    public PatchArea(Slot slot,AreaId id) {
        this.id = id;
        this.log = Util.getLogger(getClass().getName() + "." + slot + "." + id);
    }

    public PatchArea(Slot slot) {
        this.id = AreaId.Settings;
        this.log = Util.getLogger(getClass().getName() + "." + slot + "." + id);
        Arrays.stream(SettingsModules.values()).forEach(sm -> {
            PatchModule m = new PatchModule(sm);
            modules.put(m.getIndex(),m);
        });
    }


    public void addVisuals(Visual.VisualType type, List<PatchVisual> visuals) {
        for (PatchModule mod : modules.values()) {
            ModuleType mt = mod.getUserModuleData().getType();
            List<Visual> vs;
            if (type != null) {
                vs = mt.getVisuals().get(type);
            } else {
                vs = new ArrayList<>();
                vs.addAll(mt.getVisuals().get(Visual.VisualType.Meter));
                vs.addAll(mt.getVisuals().get(Visual.VisualType.LedGroup));
            }
            visuals.addAll(vs.stream().map(v -> new PatchVisual(id,mod,v)).toList());
        }
    }


    public void addModules(FieldValues modListFvs) {
        Protocol.ModuleList.Modules.subfieldsValue(modListFvs).forEach(this::addModule);
    }

    private void addModule(FieldValues fvs) {
        PatchModule m = new PatchModule(fvs);
        modules.put(m.getIndex(),m);
    }

    public PatchModule getModule(int index) {
        PatchModule m = modules.get(index);
        if (m != null) return m;
        throw new IllegalArgumentException("No such module: " + index);
    }

    public PatchModule getSettingsModule(SettingsModules m) {
        return getModule(m.getModIndex());
    }

    public void setUserModuleParams(FieldValues moduleParams) {
        Protocol.ModuleParams.ParamSet.subfieldsValue(moduleParams)
                .forEach(fvs -> getModule(
                        Protocol.ModuleParamSet.ModIndex.intValue(fvs))
                        .setParamValues(Protocol.ModuleParamSet.ModParams.subfieldsValue(fvs)));
    }

    public void addCable(FieldValues fvs) {
        cables.add(new PatchCable(fvs));
    }

    public void addCables(FieldValues cableListFvs) {
        Protocol.CableList.Cables.subfieldsValue(cableListFvs).forEach(this::addCable);
    }

    public List<PatchCable> getCables() {
        return cables;
    }

    public void setModuleLabels(FieldValues fv) {
        Protocol.ModuleLabels.ModLabels.subfieldsValue(fv).forEach(ml ->
            getModule(Protocol.ModuleLabel.ModuleIndex.intValue(ml))
                    .setUserLabels(Protocol.ModuleLabel.Labels.subfieldsValue(ml))
        );
    }

    public void setModuleNames(FieldValues fv) {
        Protocol.ModuleNames.Names.subfieldsValue(fv).forEach(mn -> {
            PatchModule m = getModule(Protocol.ModuleName.ModuleIndex.intValue(mn));
            m.setModuleName(mn);
            log.info(() -> "setModuleName: " + m.getIndex() + ", " + m.getUserModuleData().getType() + ", " + m.getName());
        });
    }

    public void setMorphLabels(FieldValues values) {
        getSettingsModule(SettingsModules.Morphs).setMorphLabels(values);
    }

    public void setPatchLoadData(FieldValues fvs) {
        this.patchLoadData = new PatchLoadData(fvs);
        log.info(() -> "setPatchLoadData: mem=" + patchLoadData.getMem() + ", cyc=" + patchLoadData.getCycles());
    }

    public PatchLoadData getPatchLoadData() {
        return patchLoadData;
    }

    public void setSelectedParam(FieldValues fvs) {
        this.selectedParam = new SelectedParam(Protocol.SelectedParam.Module.intValue(fvs),
                Protocol.SelectedParam.Param.intValue(fvs));
        log.fine("Selected param: " + selectedParam);
    }


    public void updateParam(FieldValues fvs) {
        getModule(Protocol.ParamUpdate.Module.intValue(fvs)).updateParam(fvs);
    }
}
