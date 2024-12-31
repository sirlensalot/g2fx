package g2lib.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class G2Patch {
    public static final int MAX_VARIATIONS = 10;

    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};

    public final PatchArea<G2Module> voiceArea = new PatchArea<>(PatchArea.AreaName.Voice);
    public final PatchArea<G2Module> fxArea = new PatchArea<>(PatchArea.AreaName.FX);
    public final PatchArea<BaseModule> settingsArea = initPatchSettings();

    private final List<Morph> morphs = Arrays.stream(MORPH_LABELS).map(Morph::new).toList();
    private final List<PatchSettings> settings = new ArrayList<>();
    private String name;
    private String textPad;

    public int voices;
    public int height;
    //TODO colors
    public int monoPoly; // TODO default
    public int variation;
    public int category;

    public G2Patch(String name) {
        this.name = name;
        for (int i = 0; i < MAX_VARIATIONS; i++) {
            settings.add(new PatchSettings());
        }
    }

    private static PatchArea<BaseModule> initPatchSettings() {
        return new PatchArea<BaseModule>(List.of(
                new BaseModule(0,ModParam.GainVolume, ModParam.GainActiveMuted),
                new BaseModule(1,ModParam.Glide, ModParam.GlideSpeed),
                new BaseModule(2,ModParam.BendEnable, ModParam.BendSemi),
                new BaseModule(3,ModParam.Vibrato, ModParam.VibCents, ModParam.VibRate),
                new BaseModule(4,ModParam.ArpEnable, ModParam.ArpTime, ModParam.ArpDir, ModParam.ArpOctaves),
                new BaseModule(5,ModParam.MiscOctShift, ModParam.MiscSustain)
                ));
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public PatchArea<G2Module> getUserArea(int location) {
        return switch (location) {
            case 0 -> fxArea;
            case 1 -> voiceArea;
            default -> throw new IllegalArgumentException("Invalid location: " + location);
        };
    }

    public PatchArea<? extends ParamModule> getArea(int location) {
        return switch (location) {
            case 0 -> fxArea;
            case 1 -> voiceArea;
            case 2 -> settingsArea;
            default -> throw new IllegalArgumentException("Invalid location: " + location);
        };
    }

    public Morph getMorph(int ix) {
        if (ix >= 8) { throw new IllegalArgumentException("Invalid morph index: " + ix); }
        return morphs.get(ix);
    }

    public PatchSettings getSettings(int variation) {
        return settings.get(variation);
    }

    public void setSettings(int variation, PatchSettings pss) {
        settings.set(variation,pss);
    }
}
