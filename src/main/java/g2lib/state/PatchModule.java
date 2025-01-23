package g2lib.state;

import g2lib.model.*;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class PatchModule {

    public static final int MAX_VARIATIONS = 10;
    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};

    private final UserModuleData userModuleData;
    private final SettingsModules settingsModuleType;
    private final List<NamedParam> params;

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
        this.params = mkParams(settingsModule);
    }

    private List<NamedParam> mkParams(SettingsModules settingsModule) {
        return switch (settingsModule) {
            case MorphDials -> mkMorphParams(ModParam.MorphDial, m -> List.of());
            case MorphModes -> mkMorphParams(ModParam.MorphMode, m -> List.of("Knob", m));
            case Gain -> mkParams(ModParam.GainVolume, ModParam.GainActiveMuted);
            case Glide -> mkParams(ModParam.Glide, ModParam.GlideSpeed);
            case Bend -> mkParams(ModParam.BendEnable, ModParam.BendSemi);
            case Vibrato -> mkParams(ModParam.Vibrato, ModParam.VibCents, ModParam.VibRate);
            case Arpeggiator -> mkParams(ModParam.ArpEnable, ModParam.ArpTime, ModParam.ArpDir, ModParam.ArpOctaves);
            case Misc -> mkParams(ModParam.MiscOctShift, ModParam.MiscSustain);
        };
    }

    private List<NamedParam> mkMorphParams(ModParam modParam, Function<String,List<String>> labelF) {
        return Arrays.stream(MORPH_LABELS).map(m -> new NamedParam(modParam, m, labelF.apply(m))).toList();
    }

    private List<NamedParam> mkParams(ModParam... params) {
        return Arrays.stream(params).map(NamedParam::new).toList();
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
