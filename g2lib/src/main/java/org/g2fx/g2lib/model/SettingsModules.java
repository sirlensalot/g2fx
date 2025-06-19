package org.g2fx.g2lib.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public enum SettingsModules {
    Morphs {
        public List<NamedParam> mkParams() {
            List<NamedParam> ps = new ArrayList<>(mkMorphParams(ModParam.MorphDial, m -> List.of()));
            ps.addAll(mkMorphParams(ModParam.MorphMode, m -> List.of("Knob", m)));
            return ps;
        }
    },
    Gain {
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.GainVolume, ModParam.GainActiveMuted);
        }
    },
    Glide {
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.Glide, ModParam.GlideSpeed);
        }
    },
    Bend {
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.BendEnable, ModParam.BendSemi);
        }
    },
    Vibrato {
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.Vibrato, ModParam.VibCents, ModParam.VibRate);
        }
    },
    Arpeggiator {
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.ArpEnable, ModParam.ArpTime, ModParam.ArpDir, ModParam.ArpOctaves);
        }
    },
    Misc {
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.MiscOctShift, ModParam.MiscSustain);
        }
    };

    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};

    public abstract List<NamedParam> mkParams();

    protected static List<NamedParam> mkParams(ModParam... params) {
        return Arrays.stream(params).map(NamedParam::new).toList();
    }

    protected List<NamedParam> mkMorphParams(ModParam modParam, Function<String,List<String>> labelF) {
        return Arrays.stream(MORPH_LABELS).map(m -> new NamedParam(modParam, m, labelF.apply(m))).toList();
    }

    public int getModIndex() { return ordinal() + 1; }

}
