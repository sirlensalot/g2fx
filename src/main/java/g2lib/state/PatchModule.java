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

    private final int index;

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
}
