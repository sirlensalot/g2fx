package g2lib.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class G2Patch {
    public static final int MAX_VARIATIONS = 10;

    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};

    private final Area voiceArea = new Area(Area.AreaName.Voice);
    private final Area fxArea = new Area(Area.AreaName.FX);
    private final List<Morph> morphs = Arrays.stream(MORPH_LABELS).map(Morph::new).toList();
    private final List<PatchSettings> settings = new ArrayList<>();
    private String name;
    private String textPad;

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

    public Area getArea(int location) {
        return location == 0 ? fxArea : voiceArea;
    }

    public Morph getMorph(int ix) {
        if (ix >= 8) { throw new IllegalArgumentException("Invalid morph index: " + ix); }
        return morphs.get(ix);
    }

    public PatchSettings getSettings(int variation) {
        return settings.get(variation);
    }
}
