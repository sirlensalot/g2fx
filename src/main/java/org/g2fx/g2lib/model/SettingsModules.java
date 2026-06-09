package org.g2fx.g2lib.model;

import org.g2fx.g2gui.controls.IndexParam;
import org.g2fx.g2lib.util.SafeLookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum SettingsModules {
    Morphs { // 1
        @Override
        public List<ModParam> getModParams() {
            ArrayList<ModParam> ps = new ArrayList<>(Collections.nCopies(8, ModParam.MorphDial));
            ps.addAll(Collections.nCopies(8,ModParam.MorphMode));
            return ps;
        }

    },
    Gain { // 2
        public List<ModParam> getModParams() {
            return List.of(ModParam.GainVolume, ModParam.GainActiveMuted);
        }
    },
    Glide { // 3
        public List<ModParam> getModParams() {
            return List.of(ModParam.GlideControl, ModParam.GlideSpeed);
        }
    },
    Bend { // 4
        public List<ModParam> getModParams() {
            return List.of(ModParam.BendEnable, ModParam.BendSemi);
        }
    },
    Vibrato { // 5
        public List<ModParam> getModParams() {
            return List.of(ModParam.VibratoControl, ModParam.VibCents, ModParam.VibRate);
        }
    },
    Arpeggiator { // 6
        public List<ModParam> getModParams() {
            return List.of(ModParam.ArpEnable, ModParam.ArpTime, ModParam.ArpDir, ModParam.ArpOctaves);
        }
    },
    Misc { // 7
        public List<ModParam> getModParams() {
            return List.of(ModParam.MiscOctShift, ModParam.MiscSustain);
        }
    };

    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};
    public static final String MORPH_GW1="G.Wh 1";

    public static final SafeLookup<Integer,SettingsModules> IX_LOOKUP =
            SafeLookup.makeLookup(values(), SettingsModules::getModIndex);

    public abstract List<ModParam> getModParams();

    public int getModIndex() { return ordinal() + 1; }

    public IndexParam getIndexParam(ModParam param) {
        int i = 0;
        for (ModParam p : getModParams()) {
            if (p == param) { return new IndexParam(p.mk(),i,name()); }
        }
        throw new IllegalArgumentException("Invalid mod param " + param + " for settings module " + this);
    }



}
