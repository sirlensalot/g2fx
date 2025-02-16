package g2lib.state;

import g2lib.model.NamedParam;
import g2lib.model.SettingsModules;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;
import g2lib.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PatchModule {

    private final Logger log;
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
        this.index = Protocol.UserModule.Index.intValue(userModuleFvs);
        this.userModuleData = new UserModuleData(userModuleFvs);
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
        values = varParams;
    }

    public List<Integer> getVarValues(int variation) {
        return Protocol.VarParams.Params.subfieldsValue(getRequiredVarValues(variation))
                .stream().map(Protocol.Data7.Datum::intValue).toList();
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
        if (isUserModule()) return userModuleData;
        throw new NullPointerException("User module data not available for settings module");
    }

    private boolean isUserModule() {
        return userModuleData != null;
    }

    public void setUserLabels(List<FieldValues> ls) {
        this.userLabels = ls;
    }

    public void setModuleName(FieldValues mn) {
        this.name = mn;
    }

    public String getName() {
        return Protocol.ModuleName.Name.stringValue(name);
    }

    public void setMorphLabels(FieldValues values) {
        this.morphLabels = values;
    }

    public String getMorphLabel(int paramIndex) {
        if (paramIndex < 0 || paramIndex > 7) {
            throw new IllegalArgumentException("Invalid param index: " + paramIndex);
        }
        if (morphLabels != null) {
            for (FieldValues f : Protocol.MorphLabels.Labels.subfieldsValue(morphLabels)) {
                if (paramIndex + 8 == Protocol.MorphLabel.Entry.intValue(f)) {
                    return Protocol.MorphLabel.Label.stringValue(f);
                }
            }
        }
        return MORPH_LABELS[paramIndex];
    }

    public String getModuleLabel(int paramIndex) {
        NamedParam p = getNamedParam(paramIndex);
        if (userLabels != null) {
            for (FieldValues f : userLabels) {
                if (paramIndex == Protocol.ParamLabel.ParamIndex.intValue(f)) {
                    return Protocol.ParamLabel.Label.stringValue(f);
                }
            }
        }
        //TODO!!! what about ModuleType.M_Sw8_1 and other multi-label params???
        return (!p.labels().isEmpty() ? p.labels().getFirst() : null);
    }

    private NamedParam getNamedParam(int paramIndex) {
        if (params == null) { throw new UnsupportedOperationException("getModuleLabel: no params"); }
        if (paramIndex < 0 || paramIndex >= params.size()) {
            throw new IllegalArgumentException("Invalid param index: " + paramIndex);
        }
        NamedParam p = params.get(paramIndex);
        return p;
    }

    public void updateParam(FieldValues fvs) {
        int variation = Protocol.ParamUpdate.Variation.intValue(fvs);
        int value = Protocol.ParamUpdate.Value.intValue(fvs);
        int param = Protocol.ParamUpdate.Param.intValue(fvs);
        FieldValues vvs = getRequiredVarValues(variation);
        FieldValues v = Protocol.VarParams.Params.subfieldsValue(vvs).get(param);
        int old = Protocol.Data7.Datum.intValue(v);
        if (old != value) { //updates can happen with same value, ignore
            v.update(Protocol.Data7.Datum.value(value));
            log.fine(() -> String.format("updateParam: var=%s, param=%s[%s], old=%s, value=%s",
                    variation, getNamedParam(param).name(), param, old, value));
        }
     }
}
