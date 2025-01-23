package g2lib.model;

import g2lib.Util;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.util.*;

public class G2Patch {
    public static final int MAX_VARIATIONS = 10;

    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};

    public final G2PatchArea<G2Module> voiceArea = new G2PatchArea<>(G2PatchArea.AreaName.Voice);
    public final G2PatchArea<G2Module> fxArea = new G2PatchArea<>(G2PatchArea.AreaName.FX);
    public final G2PatchArea<BaseModule> settingsArea = initPatchSettings();

    private String name;
    private String textPad;

    public int voices;
    public int height;
    //TODO colors
    public int monoPoly; // TODO default
    public int variation;
    public int category;

    // cable visibility colors
    public boolean cvRed = true;
    public boolean cvBlue = true;
    public boolean cvYellow = true;
    public boolean cvOrange = true;
    public boolean cvGreen = true;
    public boolean cvPurple = true;
    public boolean cvWhite = true;

    public String textArea;


    public G2Patch(String name) {
        this.name = name;
    }

    private static G2PatchArea<BaseModule> initPatchSettings() {
        return new G2PatchArea<BaseModule>(List.of(
                new BaseModule(SettingsModules.MorphDials), // morph dials
                new BaseModule(SettingsModules.MorphModes), // morph modes
                new BaseModule(SettingsModules.Gain,
                        ModParam.GainVolume, ModParam.GainActiveMuted),
                new BaseModule(SettingsModules.Glide,
                        ModParam.Glide, ModParam.GlideSpeed),
                new BaseModule(SettingsModules.Bend,
                        ModParam.BendEnable, ModParam.BendSemi),
                new BaseModule(SettingsModules.Vibrato,
                        ModParam.Vibrato, ModParam.VibCents, ModParam.VibRate),
                new BaseModule(SettingsModules.Arpeggiator,
                        ModParam.ArpEnable, ModParam.ArpTime, ModParam.ArpDir, ModParam.ArpOctaves),
                new BaseModule(SettingsModules.Misc,
                        ModParam.MiscOctShift, ModParam.MiscSustain)
                ));
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public G2PatchArea<G2Module> getUserArea(int location) {
        return switch (location) {
            case 0 -> fxArea;
            case 1 -> voiceArea;
            default -> throw new IllegalArgumentException("Invalid location: " + location);
        };
    }

    public BaseModule getSettingsModule(SettingsModules m) {
        return settingsArea.getModuleRequired(m.ordinal());
    }

    public G2PatchArea<? extends ParamModule> getArea(int location) {
        return switch (location) {
            case 0 -> fxArea;
            case 1 -> voiceArea;
            case 2 -> settingsArea;
            default -> throw new IllegalArgumentException("Invalid location: " + location);
        };
    }

    public void writeYaml(String file) throws Exception {
        FileWriter fw = new FileWriter(file);
        new Yaml().dump(Util.withYamlMap(top -> {
                    top.put("name",name);
                    top.put("voices",voices);
                    top.put("height",height);
                    //TODO colors
                    top.put("monoPoly",monoPoly);
                    top.put("variation",variation);
                    top.put("category",category);
                    top.put("voice",voiceArea.toYamlObj());
                    top.put("fx",fxArea.toYamlObj());
                    top.put("settings",settingsArea.toYamlObj());
                })
        ,fw);
        fw.close();
    }

    public void setTextPad(String textPad) {
        this.textPad = textPad;
    }
}
