package g2lib.state;

import g2lib.model.*;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;

public class PatchModule {

    public static final int MAX_VARIATIONS = 10;
    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};

    private final UserModuleData userModuleData;
    private final SettingsModules settingsModuleType;
    private final List<NamedParam> params;
    private List<FieldValues> values;
    private List<FieldValues> userLabels;
    private FieldValues name;

    private final int index;
    private FieldValues morphLabels;

    // constructor for user modules
    public PatchModule(FieldValues userModuleFvs) {
        this.index = Protocol.UserModule.Index.intValueRequired(userModuleFvs);
        this.userModuleData = new UserModuleData(userModuleFvs);
        this.settingsModuleType = null;
        this.params = new ArrayList<>(userModuleData.getType().getParams());
    }


    public PatchModule(SettingsModules settingsModule) {
        this.index = settingsModule.ordinal();
        this.settingsModuleType = settingsModule;
        this.userModuleData = null;
        this.params = settingsModule.mkParams();
    }

    public void setUserParamValues(FieldValues moduleParams) {
        values = new ArrayList<>(Protocol.ModuleParamSet.ModParams
                .subfieldsValueRequired(moduleParams));
    }

    public void setSettingParamValues(FieldValues patchParams) {
        values = settingsModuleType.getParamValues(patchParams);
    }

    public List<Integer> getVarValues(int variation) {
        if (settingsModuleType != null) {
            return getSettingsVarValues(variation);
        } else {
            return getUserVarValues(variation);
        }
    }

    private List<Integer> getUserVarValues(int variation) {
        return Protocol.VarParams.Params.subfieldsValueRequired(getRequiredVarValues(variation))
                .stream().map(Protocol.Data7.Datum::intValueRequired).toList();

    }

    private List<Integer> getSettingsVarValues(int variation) {
        return settingsModuleType.getVarValues(getRequiredVarValues(variation));
    }

    private List<Integer> getMorphVarValues(Protocol.MorphSettings ms, FieldValues morph) {
        List<FieldValues> dials = ms.subfieldsValueRequired(morph);
        return dials.stream().map(Protocol.Data7.Datum::intValueRequired).toList();
    }


    private FieldValues getRequiredVarValues(int variation) {
        if (variation >= values.size()) {
            throw new IllegalArgumentException("Invalid/missing variation: " + variation);
        }
        return values.get(variation);
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
        if (userModuleData != null) return userModuleData;
        throw new NullPointerException("User module data not available for settings module");
    }

    public void setUserLabels(List<FieldValues> ls) {
        this.userLabels = ls;
    }

    public void setModuleName(FieldValues mn) {
        this.name = mn;
    }

    public String getName() {
        return Protocol.ModuleName.Name.stringValueRequired(name);
    }

    public void setMorphLabels(FieldValues values) {
        this.morphLabels = values;
    }

    public String getMorphLabel(int paramIndex) {
        if (paramIndex < 0 || paramIndex > 7) {
            throw new IllegalArgumentException("Invalid param index: " + paramIndex);
        }
        if (morphLabels != null) {
            for (FieldValues f : Protocol.MorphLabels.Labels.subfieldsValueRequired(morphLabels)) {
                if (paramIndex + 8 == Protocol.MorphLabel.Entry.intValueRequired(f)) {
                    return Protocol.MorphLabel.Label.stringValueRequired(f);
                }
            }
        }
        return MORPH_LABELS[paramIndex];
    }

    public String getModuleLabel(int paramIndex) {
        if (params == null) { throw new UnsupportedOperationException("getModuleLabel: no params"); }
        if (paramIndex < 0 || paramIndex >= params.size()) {
            throw new IllegalArgumentException("Invalid param index: " + paramIndex);
        }
        if (userLabels != null) {
            for (FieldValues f : userLabels) {
                if (paramIndex == Protocol.ParamLabel.ParamIndex.intValueRequired(f)) {
                    return Protocol.ParamLabel.Label.stringValueRequired(f);
                }
            }
        }
        //TODO!!! what about ModuleType.M_Sw8_1 and other multi-label params???
        NamedParam p = params.get(paramIndex);
        return (!p.labels().isEmpty() ? p.labels().getFirst() : null);
    }
}
