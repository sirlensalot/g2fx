package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.*;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.usb.UsbSlotSender;
import org.g2fx.g2lib.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class PatchModule {

    private final Logger log;
    public static final int MAX_VARIATIONS = 10;
    public static final int FILE_VARIATIONS = 9;

    private final UserModuleData userModuleData; // user
    private final UsbSlotSender sender;
    private final AreaId area;
    private final SettingsModules settingsModuleType; // settings
    private ParamValues values;
    private final Map<Integer,List<LibProperty<String>>> userLabels = new TreeMap<>(); // user
    private LibProperty<String> name;

    private final int index;
    private FieldValues morphLabelFvs; // settings
    private List<LibProperty<String>> morphLabels; // settings
    private final List<PatchVisual> leds; // user, derived
    private final List<PatchVisual> metersAndGroups; // user, derivced

    /**
     * User module constructor
     */
    public PatchModule(FieldValues userModuleFvs, UsbSlotSender sender, AreaId area) {
        this.userModuleData = new UserModuleData(userModuleFvs,sender,area);
        this.sender = sender;
        this.area = area;
        this.index = userModuleData.getIndex();
        this.settingsModuleType = null;
        log = Util.getLogger(getClass(),userModuleData.getType(),index);
        leds = initVisuals(Visual.VisualType.Led);
        metersAndGroups = initVisuals(null);
    }

    /**
     * Settings module constructor.
     */
    public PatchModule(SettingsModules settingsModule, UsbSlotSender sender, AreaId area) {
        this.index = settingsModule.getModIndex();
        this.settingsModuleType = settingsModule;
        this.sender = sender;
        this.area = area;
        this.userModuleData = null;
        log = Util.getLogger(getClass(), settingsModuleType,index);
        leds = metersAndGroups = new ArrayList<>();
    }

    public AreaId getArea() {
        return area;
    }



    public void setParamValues(List<FieldValues> varParams) {
        // add extra variations with defaults
        // TODO this copy will be unnecessary once file-writing migrates to new method
        List<FieldValues> vps = new ArrayList<>(varParams);
        for (int i = varParams.size(); i < MAX_VARIATIONS; i++) {
            vps.add(ParamValues.mkDefaultParams(getDefaultParamValues(),i));
        }
        values = new ParamValues(vps,sender,area,index);
    }

    public List<Integer> getVarValues(int variation) {
        return values.getVarValues(variation);
    }

    public List<List<Integer>> getAllVarValues() {
        return values.getAllVarValues();
    }

    public LibProperty<Integer> getSettingsValueProperty(ModParam param, int variation) {
        int i = settingsModuleType.getModParams().indexOf(param);
        if (i == -1) { throw new IllegalArgumentException(
                "Invalid mod param " + param + " for settings " + settingsModuleType); }
        return getParamValueProperty(variation, i);
    }

    public LibProperty<Integer> getParamValueProperty(int variation, int index) {
        return values.param(variation,index);
    }




    public int getIndex() {
        return index;
    }

    /**
     * For Fx/Voice, get user module data.
     * @return data if present
     * @throws NullPointerException if settings module
     */
    public UserModuleData getUserModuleData() {
        if (isUserModule()) return userModuleData;
        throw new NullPointerException("User module data not available for settings module");
    }

    private boolean isUserModule() {
        return userModuleData != null;
    }

    public void setUserLabels(List<FieldValues> uls) {
        uls.forEach(fvs -> userLabels.put(Protocol.ParamLabels.ParamIndex.intValue(fvs),
                Protocol.ParamLabels.Labels.subfieldsValue(fvs).stream().map(ls ->
                        LibProperty.stringFieldProperty(ls, Protocol.ParamLabel.Label)).toList()));
    }

    public void setModuleName(FieldValues mn) {
        this.name = LibProperty.stringFieldProperty(mn,Protocol.ModuleName.Name);
    }

    public LibProperty<String> name() {
        return name;
    }

    public void setMorphLabels(FieldValues values) {
        this.morphLabelFvs = values;
        this.morphLabels = Protocol.MorphLabels.Labels.subfieldsValue(morphLabelFvs).stream().map(fvs ->
                LibProperty.stringFieldProperty(fvs, Protocol.MorphLabel.Label)).toList();
    }

    public LibProperty<String> getMorphLabel(int index) {
        if (index < 0 || index > 7) {
            throw new IllegalArgumentException("Invalid param index: " + index);
        }
        return morphLabels.get(index);
    }

    public List<LibProperty<String>> getModuleLabels(int paramIndex) {
        return userLabels.get(paramIndex);
    }

    public void updateParam(FieldValues fvs) {
        values.updateParam(fvs);
    }

    public void setDefaultParamValues() {
        setParamValues(ParamValues.mkDefaultVarParams(getDefaultParamValues()));
        log.info(() -> "setDefaultParamsValues: " + values.getValues());
    }

    private List<Integer> getDefaultParamValues() {
        return area == AreaId.Settings ?
                settingsModuleType.getModParams().stream().map(mp -> mp.def).toList() :
                userModuleData.getDefaultParamValues();
    }

    public ParamValues getValues() {
        return values;
    }

    public FieldValues getParamsValues(int variationCount) {
        if (values == null) { return null; } // for param-less modules
        int pc = area == AreaId.Settings ?
                settingsModuleType.getModParams().size() :
                userModuleData.getType().getParams().size();

        List<FieldValues> vvs = new ArrayList<>(values.getValues());
        while (vvs.size() > variationCount) {
            vvs.removeLast();
        }
        return Protocol.ModuleParamSet.FIELDS.values(
                Protocol.ModuleParamSet.ModIndex.value(index),
                Protocol.ModuleParamSet.ParamCount.value(pc),
                Protocol.ModuleParamSet.ModParams.value(vvs)
        );
    }

    public FieldValues getMorphLabelValues() {
        return morphLabelFvs;
    }

    public FieldValues getModuleLabelsValues() {
        if (userLabels.isEmpty()) { return null; }
        AtomicInteger labelCount = new AtomicInteger(0);
        List<FieldValues> modLabels = userLabels.entrySet().stream().map(e -> {
            int lc = e.getValue().size();
            labelCount.addAndGet(lc);
            int plen = lc * 8 - (lc - 1); // TODO this works for 1, 2 values, try more
            return Protocol.ParamLabels.FIELDS.values(
                        Protocol.ParamLabels.IsString.value(1),
                        Protocol.ParamLabels.ParamLen.value(plen),
                        Protocol.ParamLabels.ParamIndex.value(e.getKey()),
                        Protocol.ParamLabels.Labels.value(e.getValue().stream().map(s ->
                                Protocol.ParamLabel.FIELDS.values(
                                Protocol.ParamLabel.Label.value(s.get()))).toList()));
        }).toList();
        return Protocol.ModuleLabel.FIELDS.values(
                Protocol.ModuleLabel.ModuleIndex.value(index),
                Protocol.ModuleLabel.ModLabelLen.value(userLabels.size() * 3 + labelCount.get() * 7),
                Protocol.ModuleLabel.Labels.value(modLabels)
        );
    }

    private List<PatchVisual> initVisuals(Visual.VisualType type) {
        ModuleType mt = userModuleData.getType();
        List<Visual> vs;
        if (type != null) {
            vs = mt.getVisuals().get(type);
        } else {
            vs = new ArrayList<>();
            vs.addAll(mt.getVisuals().get(Visual.VisualType.Meter));
            vs.addAll(mt.getVisuals().get(Visual.VisualType.LedGroup));
        }
        return vs.stream().map(v -> new PatchVisual(area, userModuleData.getType() + ":" + userModuleData.getIndex(), v)).toList();
    }

    public List<PatchVisual> getLeds() {
        return leds;
    }

    public List<PatchVisual> getMetersAndGroups() {
        return metersAndGroups;
    }
}
