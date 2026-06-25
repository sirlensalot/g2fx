package org.g2fx.g2lib.state;

import com.google.common.collect.Streams;
import org.g2fx.g2gui.controls.Cables;
import org.g2fx.g2gui.module.ModuleDelta;
import org.g2fx.g2lib.model.*;
import org.g2fx.g2lib.protocol.Codes;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.UsbSlotSender;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.g2fx.g2lib.model.CableDelta.CableIndex;
import static org.g2fx.g2lib.model.Connector.ConnDir.In;
import static org.g2fx.g2lib.model.Connector.ConnDir.Out;
import static org.g2fx.g2lib.protocol.Codes.O_RESOURCES_USED;
import static org.g2fx.g2lib.protocol.Sections.writeSection;
import static org.g2fx.g2lib.state.PatchModule.MAX_VARIATIONS;
import static org.g2fx.g2lib.util.Util.forEach;

public class PatchArea {

    public final AreaId id;
    private final UsbSlotSender sender;
    private final Runnable updateVisuals;
    private final Logger log;

    private final Map<Integer,PatchModule> modules = new TreeMap<>();
    private final List<PatchCable> cables = new ArrayList<>();
    private PatchLoadData patchLoadData = new PatchLoadData();

    private final LibProperty<ModuleDelta> dummyModuleAddProp =
            new LibProperty<>(new ModuleDelta());
    private final LibProperty<CableDelta<Cables.Cable>> dummyCableDeltaProp =
            new LibProperty<>(new CableDelta<>());

    public record SelectedParam(int module,int param) { }
    private SelectedParam selectedParam;

    /**
     * User module area constructor.
     */
    public PatchArea(Slot slot, AreaId id, UsbSlotSender sender, Runnable updateVisuals) {
        this.id = id;
        this.sender = sender;
        this.updateVisuals = updateVisuals;
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
        updateVisuals=()->{};
    }

    public void updateVisuals() {
        updateVisuals.run();
    }

    public LibProperty<ModuleDelta> getDummyModuleAddProp() {
        return dummyModuleAddProp;
    }

    public LibProperty<CableDelta<Cables.Cable>> getDummyCableDeltaProp() {
        return dummyCableDeltaProp;
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

    public void setSelectedParam(int module, int param) {
        this.selectedParam = new SelectedParam(module, param);
        try {
            sender.sendSlotCommand("selected param",
                    Codes.O_SELECT_PARAM,
                    0,
                    id.ordinal(),
                    module,
                    param);
        } catch (Exception e) {
            log.log(Level.SEVERE,"Failed to send select param message",e);
        }
    }


    public SelectedParam getSelectedParam() {
        return selectedParam;
    }

    public void updateParam(FieldValues fvs) {
        getModule(Protocol.ParamUpdate.Module.intValue(fvs)).updateParam(fvs);
    }

    public record CreateResult(List<PatchModule> modules, List<PatchCable> cables) {}

    public CreateResult createModules(ModuleDelta md) throws Exception {
        List<PatchModule> pms = new ArrayList<>();
        for (ModuleDelta.UserModuleRecord mr : md.modules()) {
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
        List<PatchCable> newCables = new ArrayList<>();
        for (FieldValues cfvs : md.cables()) {
            PatchCable c = new PatchCable(cfvs);
            cables.add(c);
            newCables.add(c);
        }

        //assemble message
        BitBuffer bb = new BitBuffer();
        for (PatchModule pm : pms) {
            UserModuleData umd = pm.getUserModuleData();
            Protocol.ModuleAdd.FIELDS.init().addAll(
                    Protocol.ModuleAdd.ModuleAdd_30.value(Codes.O_ADD_MODULE), //S_MODULE_ADD
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
        ByteBuffer buf = bb.getBuffer();
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
                        Protocol.ModuleNames.Reserved.value(0),
                        Protocol.ModuleNames.NameCount.value(pms.size()),
                        Protocol.ModuleNames.Names.value(pms.stream().map(m -> Protocol.ModuleName.FIELDS.values(
                                Protocol.ModuleName.ModuleIndex.value(m.getIndex()),
                                Protocol.ModuleName.Name.value(m.name().get()))).toList())));
        buf.limit(buf.position());
        sender.sendSlotRequest("add-modules",buf);
        return new CreateResult(pms,newCables);
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


    public ModuleDelta mkCopyModuleDelta(List<Integer> idxs) {
        List<ModuleDelta.UserModuleRecord> umrs = idxs.stream().map(i ->
                new ModuleDelta.UserModuleRecord(getModule(i))).toList();
        List<FieldValues> newCables = new ArrayList<>();
        for (PatchCable cable : cables) {
            if (idxs.contains(cable.getSrcModule()) && idxs.contains(cable.getDestModule())) {
                newCables.add(cable.getFieldValues().copy());
            }
        }
        return new ModuleDelta(umrs,newCables,true);
    }

    public ModuleDelta mkDeleteModuleDelta(List<Integer> idxs) {
        List<ModuleDelta.UserModuleRecord> umrs = idxs.stream().map(i ->
                new ModuleDelta.UserModuleRecord(getModule(i))).toList();
        List<FieldValues> cutCables = new ArrayList<>();
        for (PatchCable cable : cables) {
            if (idxs.contains(cable.getSrcModule()) || idxs.contains(cable.getDestModule())) {
                cutCables.add(ModuleDelta.invertCable(cable.getFieldValues()));
            }
        }
        return new ModuleDelta(umrs,cutCables,false);
    }



    public void deleteModules(ModuleDelta md) throws Exception {
        md.modules().forEach(mr -> modules.remove(mr.getIndex()));
        md.cables().forEach(mdc -> cables.removeIf(c -> mdc.equals(c.getFieldValues())));
        BitBuffer bb = new BitBuffer();
        forEach(md.cables(), mdc -> {
            Connector.ConnDir destConnType = Protocol.Cable.Direction.booleanIntValue(mdc) ? Out : In;
            Protocol.DeleteCable.FIELDS.values(
                    Protocol.DeleteCable.DeleteCable_51.value(Codes.O_DELETE_CABLE),
                    Protocol.DeleteCable.Reserved.value(0), // Unknown
                    Protocol.DeleteCable.Location.value(id.ordinal()),
                    Protocol.DeleteCable.SrcModule.value(Protocol.Cable.SrcModule.intValue(mdc)),
                    Protocol.DeleteCable.SrcConnType.value(Connector.ConnDir.In.ordinal()),
                    Protocol.DeleteCable.SrcConn.value(Protocol.Cable.SrcConn.intValue(mdc)),
                    Protocol.DeleteCable.DestModule.value(Protocol.Cable.DestModule.intValue(mdc)),
                    Protocol.DeleteCable.DestConnType.value(destConnType.ordinal()),
                    Protocol.DeleteCable.DestConn.value(Protocol.Cable.DestConn.intValue(mdc))
            ).write(bb);
        });
        forEach(md.modules(),mr -> {
            bb.put(8,Codes.O_DELETE_MODULE);
            bb.put(8,id.ordinal());
            bb.put(8,mr.getIndex());
        });
        sender.sendSlotRequest("deleteModules",bb.toBuffer());
        sendAreaResourcesRequest();
    }

    public void sendAreaResourcesRequest() throws Exception {
        sender.sendSlotRequest("patch load " + id, O_RESOURCES_USED, id.ordinal());
    }

    public void execCableDelta(CableDelta<CableIndex> d) throws Exception {
        if (d.add()) {
            execAddCable(d);
        } else {
            execDeleteCable(d);
        }
    }

    private void execDeleteCable(CableDelta<CableIndex> d) throws Exception {
        // collect removes
        List<PatchCable> remove = new ArrayList<>();
        // hypothesis that dynamic modules going unconnected drives load request seen in regression 11
        Map<Integer,Long> moduleCableCount = new HashMap<>();
        // walk cables to search for removes and color changes. deletes are small so quadratic ok ...
        cables.forEach(c -> {
            // compute cable-conn count by module
            moduleCableCount.compute(c.getSrcModule(),(_,cc) -> cc == null ? 1 : cc+1);
            moduleCableCount.compute(c.getDestModule(),(_,cc) -> cc == null ? 1 : cc+1);
            // match with remove cables
            d.cables().forEach(ci -> { if (ci.match(c)) {
                remove.add(c);
                //decrement if dynamic
                if (ci.srcConn().bandwidth()== Connector.Bandwidth.Dynamic) {
                    moduleCableCount.computeIfPresent(c.getSrcModule(), (_, cc) -> --cc);
                }
                if (ci.destConn().bandwidth()== Connector.Bandwidth.Dynamic) {
                    moduleCableCount.computeIfPresent(c.getDestModule(), (_, cc) -> --cc);
                }
            }});

            d.colorChanges().forEach((ci,cc)->{
                if (ci.match(c)) { c.setColor(cc); }
            });
        });
        // module counts decremented to 0 triggers request load
        AtomicReference<Boolean> requestLoad = new AtomicReference<>(false);
        moduleCableCount.values().forEach(cc -> {if (cc==0) requestLoad.set(true);});

        updateModuleUprates(d);

        cables.removeAll(remove);
        BitBuffer bb = new BitBuffer();
        // write deletes
        forEach(remove, c->{
            Connector.ConnDir destConnType = c.getDirection() ? Out : In;
            Protocol.DeleteCable.FIELDS.values(
                    Protocol.DeleteCable.DeleteCable_51.value(Codes.O_DELETE_CABLE),
                    Protocol.DeleteCable.Reserved.value(1), // for 011 test, shd prob be 0?
                    Protocol.DeleteCable.Location.value(id.ordinal()),
                    Protocol.DeleteCable.SrcModule.value(c.getDestModule()),
                    Protocol.DeleteCable.SrcConnType.value(In.ordinal()),
                    Protocol.DeleteCable.SrcConn.value(c.getDestConn()),
                    Protocol.DeleteCable.DestModule.value(c.getSrcModule()),
                    Protocol.DeleteCable.DestConnType.value(destConnType.ordinal()),
                    Protocol.DeleteCable.DestConn.value(c.getSrcConn())
            ).write(bb);
        });
        writeUprates(d, bb);
        sender.sendSlotRequest("delete cable",bb.toBuffer());
        if (requestLoad.get()) {
            sendAreaResourcesRequest();
        }
    }

    private void writeUprates(CableDelta<CableIndex> d, BitBuffer bb) throws Exception {
        forEach(d.uprateChanges(),(m, u) -> {
            bb.put(8,Codes.O_SET_UPRATE);
            bb.put(8,id.ordinal());
            bb.put(8,m);
            bb.put(8,u ? 1 : 0);
        });
    }

    private void updateModuleUprates(CableDelta<CableIndex> d) {
        d.uprateChanges().forEach((m, u)->
                getModule(m).getUserModuleData().uprate().set(u));
    }

    private void execAddCable(CableDelta<CableIndex> d) throws Exception {
        // hypothesis is that dynamic module becoming connected triggers load request per regression 11
        Set<Integer> modulesWithoutCables = new HashSet<>(modules.keySet());
        // do color changes
        cables.forEach(c -> {
            modulesWithoutCables.remove(c.getSrcModule());
            modulesWithoutCables.remove(c.getDestModule());
            d.colorChanges().forEach((ci,cc)-> {
                if (ci.match(c)) c.setColor(cc);
            });
        });

        AtomicReference<Boolean> requestLoad = new AtomicReference<>(false);
        cables.addAll(d.cables().stream().map(ci -> {
            // look for cable-less module add with dynamic connector
            requestLoad.set(requestLoad.get() ||
                    (modulesWithoutCables.contains(ci.srcModule()) && ci.srcConn().bandwidth()== Connector.Bandwidth.Dynamic) ||
                    (modulesWithoutCables.contains(ci.destModule()) && ci.destConn().bandwidth()== Connector.Bandwidth.Dynamic));
            // build add cable
            return new PatchCable(Protocol.Cable.FIELDS.values(
                    Protocol.Cable.Color.value(ci.color()),
                    Protocol.Cable.SrcModule.value(ci.srcModule()),
                    Protocol.Cable.SrcConn.value(ci.srcConn().index()),
                    Protocol.Cable.Direction.value(Out.ordinal()),
                    Protocol.Cable.DestModule.value(ci.destModule()),
                    Protocol.Cable.DestConn.value(ci.destConn().index())
            ));
        }).toList());
        updateModuleUprates(d);
        BitBuffer bb = new BitBuffer();
        forEach(d.cables(),c-> Protocol.AddCable.FIELDS.values(
                Protocol.AddCable.AddCable_50.value(0x50),
                Protocol.AddCable.Reserved.value(1),
                Protocol.AddCable.Location.value(id.ordinal()),
                Protocol.AddCable.Color.value(c.color()),
                Protocol.AddCable.SrcModule.value(c.srcModule()),
                Protocol.AddCable.SrcConnType.value(c.srcConn().dir().ordinal()),
                Protocol.AddCable.SrcConn.value(c.srcConn().index()),
                Protocol.AddCable.DestModule.value(c.destModule()),
                Protocol.AddCable.DestConnType.value(c.destConn().dir().ordinal()),
                Protocol.AddCable.DestConn.value(c.destConn().index())
                ).write(bb));
        writeUprates(d,bb);
        sender.sendSlotRequest("add cable",bb.toBuffer());
        if (requestLoad.get()) {
            sendAreaResourcesRequest();
        }
    }

}
