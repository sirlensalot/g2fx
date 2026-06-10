package org.g2fx.g2lib.state;

import com.google.common.collect.Streams;
import org.g2fx.g2gui.module.ModuleDelta;
import org.g2fx.g2lib.model.LibProperty;
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

    private final LibProperty<ModuleDelta> dummyModuleAddProp = new LibProperty<>(new ModuleDelta());

    public record SelectedParam(int module,int param) { }
    private SelectedParam selectedParam;

    /**
     * User module area constructor.
     */
    public PatchArea(Slot slot, AreaId id, UsbSlotSender sender) {
        this.id = id;
        this.sender = sender;
        this.log = Util.getLogger(getClass(),slot,id);
    }

    /**
     * Settings area constructor
     */
    public PatchArea(Slot slot, UsbSlotSender sender) {
        this.sender = sender;
        this.id = AreaId.Settings;
        this.log = Util.getLogger(getClass(),slot,id);
        Arrays.stream(SettingsModules.values()).forEach(sm -> {
            PatchModule m = new PatchModule(sm,sender,id);
            modules.put(m.getIndex(),m);
        });
    }

    public LibProperty<ModuleDelta> getDummyModuleAddProp() {
        return dummyModuleAddProp;
    }

    public void addVisuals(Visual.VisualType type, List<PatchVisual> visuals) {
        for (PatchModule mod : modules.values()) {
            visuals.addAll(type == Visual.VisualType.Led ? mod.getLeds() : mod.getMetersAndGroups());
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

    public void initSettingsParams() {
        for (PatchModule m : modules.values()) {
            m.setDefaultParamValues();
        }

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

    public void initMorphLabels() {
        setMorphLabels(Protocol.MorphLabels.FIELDS.values(
                Protocol.MorphLabels.LabelCount.value(1),
                Protocol.MorphLabels.Entry.value(1),
                Protocol.MorphLabels.Length.value(0x50),
                Protocol.MorphLabels.Labels.value(
                        Streams.mapWithIndex(
                                Arrays.stream(SettingsModules.MORPH_LABELS),
                                (s,i) -> Protocol.MorphLabel.FIELDS.values(
                                        Protocol.MorphLabel.Index.value(1),
                                        Protocol.MorphLabel.Length.value(8),
                                        Protocol.MorphLabel.Entry.value(8+(int)i),
                                        Protocol.MorphLabel.Label.value(s))).toList())
                ));
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

    public SelectedParam getSelectedParam() {
        return selectedParam;
    }

    public void updateParam(FieldValues fvs) {
        getModule(Protocol.ParamUpdate.Module.intValue(fvs)).updateParam(fvs);
    }

    public List<PatchModule> createModules(ModuleDelta md) throws Exception {
        return createModules(md,0);
    }
    public List<PatchModule> createModules(ModuleDelta md,int reservedHack) throws Exception {
        List<PatchModule> pms = new ArrayList<>();
        for (ModuleDelta.UserModuleRecord mr : md.records()) {
            PatchModule pm = addModule(mr.moduleData());
            if (mr.paramValues() != null) { pm.setParamValues(mr.paramValues()); }
            pm.setModuleName(Protocol.ModuleName.FIELDS.init().addAll(
                    Protocol.ModuleName.ModuleIndex.value(-1), // unused
                    Protocol.ModuleName.Name.value(mr.name())));
            if (mr.moduleLabels() != null) {
                pm.setUserLabels(Protocol.ModuleLabel.Labels.subfieldsValue(mr.moduleLabels()));
            }
            pms.add(pm);
        }

        //assemble message
        ByteBuffer buf = ByteBuffer.allocateDirect(0xffff);
        BitBuffer bb = BitBuffer.fromSlice(buf);
        for (PatchModule pm : pms) {
            UserModuleData umd = pm.getUserModuleData();
            Protocol.ModuleAdd.FIELDS.init().addAll(
                    Protocol.ModuleAdd.ModuleAdd_30.value(0x30), //S_MODULE_ADD
                    Protocol.ModuleAdd.ModuleTypeIx.value(umd.getType().ix),
                    Protocol.ModuleAdd.Location.value(id.ordinal()),
                    Protocol.ModuleAdd.Index.value(umd.getIndex()),
                    Protocol.ModuleAdd.Column.value(umd.column().get()),
                    Protocol.ModuleAdd.Row.value(umd.row().get()),
                    Protocol.ModuleAdd.Reserved_0.value(0),
                    Protocol.ModuleAdd.Uprate.value(umd.uprate().get()),
                    Protocol.ModuleAdd.Leds.value(umd.getType().isLed),
                    Protocol.ModuleAdd.Modes.value(pm.getUserModuleData().getModes().stream().map(v ->
                            Protocol.Data8.FIELDS.values(Protocol.Data8.Datum.value(v.get()))).toList()),
                    Protocol.ModuleAdd.Name.value(pm.name().get())
            ).write(bb);
        }
        buf.position(bb.limit());
        writeSection(buf,id == AreaId.Fx ? Sections.SCableList0_52 : Sections.SCableList1_52,
                Protocol.CableList.FIELDS.init().addAll(
                        Protocol.CableList.Reserved.value(0),
                        Protocol.CableList.CableCount.value(md.cables().size()),
                        Protocol.CableList.Cables.value(md.cables())
                ));
        List<PatchModule> ppms = pms.stream().filter(pm -> pm.getValues() != null).toList();
        writeSection(buf,id == AreaId.Fx ? Sections.SModuleParams0_4d : Sections.SModuleParams1_4d,
                Protocol.ModuleParams.FIELDS.init().addAll(
                        Protocol.ModuleParams.SetCount.value(ppms.size()),
                        Protocol.ModuleParams.VariationCount.value(MAX_VARIATIONS),
                        Protocol.ModuleParams.ParamSet.value(
                                ppms.stream().map(pm -> Protocol.ModuleParamSet.FIELDS.init().addAll(
                                    Protocol.ModuleParamSet.ModIndex.value(pm.getIndex()),
                                    Protocol.ModuleParamSet.ParamCount.value(pm.getUserModuleData().getType().getParams().size()),
                                    Protocol.ModuleParamSet.ModParams.value(pm.getValues().getValues()))).toList())));

        List<FieldValues> modLabels = pms.stream().map(PatchModule::getModuleLabelsValues).filter(Objects::nonNull).toList();
        writeSection(buf,id == AreaId.Fx ? Sections.SModuleLabels0_5b : Sections.SModuleLabels1_5b,
                Protocol.ModuleLabels.FIELDS.init().addAll(
                        Protocol.ModuleLabels.ModuleCount.value(modLabels.size()),
                        Protocol.ModuleLabels.ModLabels.value(modLabels)));
        writeSection(buf,id == AreaId.Fx ? Sections.SModuleNames0_5a : Sections.SModuleNames1_5a,
                Protocol.ModuleNames.FIELDS.init().addAll(
                        Protocol.ModuleNames.Reserved.value(reservedHack),
                        Protocol.ModuleNames.NameCount.value(pms.size()),
                        Protocol.ModuleNames.Names.value(pms.stream().map(m -> Protocol.ModuleName.FIELDS.values(
                                Protocol.ModuleName.ModuleIndex.value(m.getIndex()),
                                Protocol.ModuleName.Name.value(m.name().get()))).toList())));
        buf.limit(buf.position());
        sender.sendSlotRequest("add-modules",Util.getBytes(buf.rewind()));
        Patch.sendUnk6Request(sender);
        return pms;
    }

    public FieldValues getModuleListValues() {
        return Protocol.ModuleList.FIELDS.values(
                Protocol.ModuleList.ModuleCount.value(modules.size()),
                Protocol.ModuleList.Modules.value(modules.values().stream().map(pm ->
                        pm.getUserModuleData().getValues()).toList())
                        );
    }

    public FieldValues getCableListValues() {
        return Protocol.CableList.FIELDS.values(
                Protocol.CableList.Reserved.value(0), // Always 0?
                Protocol.CableList.CableCount.value(cables.size()),
                Protocol.CableList.Cables.value(cables.stream().map(PatchCable::getFieldValues).toList())
        );
    }

    public FieldValues getParamsValues(int variationCount) {
        List<FieldValues> fvss = new ArrayList<>();
        for (PatchModule m : modules.values()) {
            FieldValues pvs = m.getParamsValues(variationCount);
            if (pvs != null) { fvss.add(pvs); }
        }
        int vc = fvss.isEmpty() ? 0 : variationCount; // sigh this is sometimes, in files, 9 and sometimes not!
        return Protocol.ModuleParams.FIELDS.values(
                Protocol.ModuleParams.SetCount.value(fvss.size()),
                Protocol.ModuleParams.VariationCount.value(vc),
                Protocol.ModuleParams.ParamSet.value(fvss));
    }

    public FieldValues getMorphLabelValues() {
        return getSettingsModule(SettingsModules.Morphs).getMorphLabelValues();
    }

    public FieldValues getModuleLabelValues() {
        List<FieldValues> fvss = new ArrayList<>();
        for (PatchModule m : modules.values()) {
            FieldValues vs = m.getModuleLabelsValues();
            if (vs != null) { fvss.add(vs); }
        }
        return Protocol.ModuleLabels.FIELDS.values(
                Protocol.ModuleLabels.ModuleCount.value(fvss.size()),
                Protocol.ModuleLabels.ModLabels.value(fvss));
    }

    public FieldValues getModuleNameValues() {
        return Protocol.ModuleNames.FIELDS.values(
                Protocol.ModuleNames.Reserved.value(0), // legacy init: A:1,1 B:21,8 C:1,0 D:0,0
                Protocol.ModuleNames.NameCount.value(modules.size()),
                Protocol.ModuleNames.Names.value(
                        modules.values().stream().map(m ->
                                        Protocol.ModuleName.FIELDS.values(
                                                Protocol.ModuleName.ModuleIndex.value(m.getIndex()),
                                                Protocol.ModuleName.Name.value(m.name().get())
                                        )
                                ).toList()
                )
        );
    }
}
