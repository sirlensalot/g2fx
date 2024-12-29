package g2lib.model;

import java.util.ArrayList;
import java.util.List;

public class Morph {
    private String name;

    public record MorphSetting(int dial,int mode) {
        public MorphSetting() {
            this(0,1);
        }
    }
    private final List<MorphSetting> varMorphs = new ArrayList<>();

    public Morph(String name) {
        this.name = name;
        for (int i = 0; i < G2Patch.MAX_VARIATIONS; i++) {
            varMorphs.add(new MorphSetting());
        }
    }

    public void setVarMorph(int variation, int dial, int mode) {
        varMorphs.set(variation,new MorphSetting(dial,mode));
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
