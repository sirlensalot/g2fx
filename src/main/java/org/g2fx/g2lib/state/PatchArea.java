package org.g2fx.g2lib.state;

import org.g2fx.g2gui.panel.AreaPane;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.model.Visual;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.UsbSlotSender;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Logger;

import static org.g2fx.g2lib.protocol.Sections.writeSection;
import static org.g2fx.g2lib.state.PatchModule.MAX_VARIATIONS;

public class PatchArea {

    public final AreaId id;
    private final UsbSlotSender sender;
    private final Logger log;

    private final Map<Integer,PatchModule> modules = new TreeMap<>();
    private final List<PatchCable> cables = new ArrayList<>();
    private PatchLoadData patchLoadData = new PatchLoadData();

    private final LibProperty<Set<AreaPane.ModuleAdd>> dummyModuleAddProp = new LibProperty<>(Set.of());

    public record SelectedParam(int module,int param) { }
    private SelectedParam selectedParam;

    public PatchArea(Slot slot, AreaId id, UsbSlotSender sender) {
        this.id = id;
        this.sender = sender;
        this.log = Util.getLogger(getClass(),slot,id);
    }

    public PatchArea(Slot slot, UsbSlotSender sender) {
        this.sender = sender;
        this.id = AreaId.Settings;
        this.log = Util.getLogger(getClass(),slot,id);
        Arrays.stream(SettingsModules.values()).forEach(sm -> {
            PatchModule m = new PatchModule(sm,sender,id);
            modules.put(m.getIndex(),m);
        });
    }

    public LibProperty<Set<AreaPane.ModuleAdd>> getDummyModuleAddProp() {
        return dummyModuleAddProp;
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

    private PatchModule addModule(FieldValues fvs) {
        PatchModule m = new PatchModule(fvs,sender,id);
        modules.put(m.getIndex(),m);
        return m;
    }

    public PatchModule getModule(int index) {
        PatchModule m = modules.get(index);
        if (m != null) return m;
        throw new IllegalArgumentException("No such module: " + index);
    }

    public Collection<PatchModule> getModules() {
        return modules.values();
    }

    public PatchModule getSettingsModule(SettingsModules m) {
        return getModule(m.getModIndex());
    }

    public void setModuleParamValues(FieldValues moduleParams) {
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
            log.info(() -> "setModuleName: " + m.getIndex() + ", " + m.getUserModuleData().getType() + ", " + m.name().get());
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

    public PatchModule createModule(AreaPane.ModuleAdd ma) throws Exception {
        PatchModule pm = addModule(Protocol.UserModule.FIELDS.init().addAll(
                Protocol.UserModule.Id.value(ma.type().ix),
                Protocol.UserModule.Index.value(ma.index()),
                Protocol.UserModule.Column.value(ma.coords().column()),
                Protocol.UserModule.Row.value(ma.coords().row()),
                Protocol.UserModule.Color.value(ma.color()),
                Protocol.UserModule.Uprate.value(0),
                Protocol.UserModule.Leds.value(ma.type().isLed),
                Protocol.UserModule.Reserved.value(0), // TODO implement defaults
                Protocol.UserModule.ModeCount.value(ma.type().modes.size()),
                Protocol.UserModule.Modes.value(ma.type().modes.stream().map(np ->
                        Protocol.ModuleModes.FIELDS.init().add(Protocol.ModuleModes.Data.value(np.param().def)
                        )).toList())
        ));
        List<FieldValues> paramValuesFvs = ParamValues.mkDefaultParams(ma.type());
        pm.setParamValues(paramValuesFvs);
        FieldValues moduleNamesFvs = Protocol.ModuleName.FIELDS.init().addAll(
                Protocol.ModuleName.ModuleIndex.value(ma.index()),
                Protocol.ModuleName.Name.value(ma.name()));
        pm.setModuleName(moduleNamesFvs);

        ByteBuffer buf = ByteBuffer.allocateDirect(0xffff);
        BitBuffer bb = new BitBuffer(0xffff);
        Protocol.ModuleAdd.FIELDS.init().addAll(
                Protocol.ModuleAdd.ModuleAdd_30.value(0x30), //S_MODULE_ADD
                Protocol.ModuleAdd.ModuleTypeIx.value(ma.type().ix),
                Protocol.ModuleAdd.Location.value(id.ordinal()),
                Protocol.ModuleAdd.Index.value(ma.index()),
                Protocol.ModuleAdd.Column.value(ma.coords().column()),
                Protocol.ModuleAdd.Row.value(ma.coords().row()),
                Protocol.ModuleAdd.Reserved_0.value(0),
                Protocol.ModuleAdd.Uprate.value(0),
                Protocol.ModuleAdd.Leds.value(ma.type().isLed),
                Protocol.ModuleAdd.Modes.value(List.of()),
                Protocol.ModuleAdd.Name.value(ma.name())
        ).write(bb);
        bb.dumpToBuffer(buf);
        writeSection(buf,id == AreaId.Fx ? Sections.SCableList0_52 : Sections.SCableList1_52,
                Protocol.CableList.FIELDS.init().addAll(
                        Protocol.CableList.Reserved.value(0),
                        Protocol.CableList.CableCount.value(0),
                        Protocol.CableList.Cables.value(List.of())
                ));
        writeSection(buf,id == AreaId.Fx ? Sections.SModuleParams0_4d : Sections.SModuleParams1_4d,
                Protocol.ModuleParams.FIELDS.init().addAll(
                        Protocol.ModuleParams.SetCount.value(1),
                        Protocol.ModuleParams.VariationCount.value(MAX_VARIATIONS),
                        Protocol.ModuleParams.ParamSet.value(List.of(Protocol.ModuleParamSet.FIELDS.init().addAll(
                                Protocol.ModuleParamSet.ModIndex.value(ma.index()),
                                Protocol.ModuleParamSet.ParamCount.value(ma.type().getParams().size()),
                                Protocol.ModuleParamSet.ModParams.value(paramValuesFvs)
                        )))
                ));
        writeSection(buf,id == AreaId.Fx ? Sections.SModuleLabels0_5b : Sections.SModuleLabels1_5b,
                Protocol.ModuleLabels.FIELDS.init().addAll(
                        Protocol.ModuleLabels.ModuleCount.value(0),
                        Protocol.ModuleLabels.ModLabels.value(List.of())
                ));
        writeSection(buf,id == AreaId.Fx ? Sections.SModuleNames0_5a : Sections.SModuleNames1_5a,
                moduleNamesFvs);
        buf.limit(buf.position());
        sender.sendSlotRequest("add-module",Util.getBytes(buf.rewind()));

        return pm;
    }
}
