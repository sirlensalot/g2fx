package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

public class PatchModule {

    private final Logger log;
    public static final int MAX_VARIATIONS = 10;

    private final UserModuleData userModuleData;
    private final SettingsModules settingsModuleType;
    private final List<NamedParam> params;
    private ParamValues values = new ParamValues();
    private final Map<Integer,List<LibProperty<String>>> userLabels = new TreeMap<>();
    private LibProperty<String> name;

    private final int index;
    private FieldValues morphLabelFvs;
    private List<LibProperty<String>> morphLabels;

    // constructor for user modules
    public PatchModule(FieldValues userModuleFvs) {
        this.userModuleData = new UserModuleData(userModuleFvs);
        this.index = userModuleData.getIndex();
        this.settingsModuleType = null;
        this.params = new ArrayList<>(userModuleData.getType().getParams());
        log = Util.getLogger(getClass().getName() + "." + userModuleData.getType() + "[" + index + "]");
    }


    public PatchModule(SettingsModules settingsModule) {
        this.index = settingsModule.getModIndex();
        this.settingsModuleType = settingsModule;
        this.userModuleData = null;
        this.params = settingsModule.mkParams();
        log = Util.getLogger(getClass().getName() + "." + settingsModuleType + "[" + index + "]");
    }

    public void setParamValues(List<FieldValues> varParams) {
        values = new ParamValues(varParams);
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

    private NamedParam getNamedParam(int paramIndex) {
        if (params == null) { throw new UnsupportedOperationException("getModuleLabel: no params"); }
        if (paramIndex < 0 || paramIndex >= params.size()) {
            throw new IllegalArgumentException("Invalid param index: " + paramIndex);
        }
        return params.get(paramIndex);
    }

    public void updateParam(FieldValues fvs) {
        values.updateParam(fvs);
     }
}
