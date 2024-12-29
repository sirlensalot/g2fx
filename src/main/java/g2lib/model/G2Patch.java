package g2lib.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class G2Patch {
    public static final int MAX_VARIATIONS = 10;

    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};

    public final PatchArea voiceArea = new PatchArea(PatchArea.AreaName.Voice);
    public final PatchArea fxArea = new PatchArea(PatchArea.AreaName.FX);
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

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public PatchArea getArea(int location) {
        return location == 0 ? fxArea : voiceArea;
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
