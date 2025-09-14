package org.g2fx.g2lib.model;

import org.g2fx.g2gui.controls.IndexParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public enum SettingsModules {
    Morphs {
        @Override
        public List<ModParam> getModParams() {
            ArrayList<ModParam> ps = new ArrayList<>(Collections.nCopies(8, ModParam.MorphDial));
            ps.addAll(Collections.nCopies(8,ModParam.MorphMode));
            return ps;
        }

        public List<NamedParam> mkParams() {
            List<NamedParam> ps = new ArrayList<>(mkMorphParams(ModParam.MorphDial, m -> List.of()));
            ps.addAll(mkMorphParams(ModParam.MorphMode, m -> List.of("Knob", m)));
            return ps;
        }
    },
    Gain {
        public List<ModParam> getModParams() {
            return List.of(ModParam.GainVolume, ModParam.GainActiveMuted);
        }
    },
    Glide {
        public List<ModParam> getModParams() {
            return List.of(ModParam.GlideControl, ModParam.GlideSpeed);
        }
    },
    Bend {
        public List<ModParam> getModParams() {
            return List.of(ModParam.BendEnable, ModParam.BendSemi);
        }
    },
    Vibrato {
        public List<ModParam> getModParams() {
            return List.of(ModParam.VibratoControl, ModParam.VibCents, ModParam.VibRate);
        }
    },
    Arpeggiator {
        public List<ModParam> getModParams() {
            return List.of(ModParam.ArpEnable, ModParam.ArpTime, ModParam.ArpDir, ModParam.ArpOctaves);
        }
    },
    Misc {
        public List<ModParam> getModParams() {
            return List.of(ModParam.MiscOctShift, ModParam.MiscSustain);
        }
    };

    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};
    public static final String MORPH_GW1="G.Wh 1";


    public abstract List<ModParam> getModParams();

    public List<NamedParam> mkParams() {
        return getModParams().stream().map(NamedParam::new).toList();
    }

    protected List<NamedParam> mkMorphParams(ModParam modParam, Function<String,List<String>> labelF) {
        return Arrays.stream(MORPH_LABELS).map(m -> new NamedParam(modParam, m, labelF.apply(m))).toList();
    }

    public int getModIndex() { return ordinal() + 1; }

    public IndexParam getIndexParam(ModParam param) {
        int i = 0;
        for (ModParam p : getModParams()) {
            if (p == param) { return new IndexParam(p.mk(),i,name()); }
        }
        throw new IllegalArgumentException("Invalid mod param " + param + " for settings module " + this);
    }

}
