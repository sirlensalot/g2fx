package g2lib.model;

import g2lib.protocol.FieldEnum;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public enum SettingsModules {
    MorphDials {
        public List<FieldValues> getParamValues(FieldValues patchParams) {
            return Protocol.PatchParams.Morphs.subfieldsValueRequired(patchParams);
        }
        public List<NamedParam> mkParams() {
            return mkMorphParams(ModParam.MorphDial, m -> List.of());
        }

        @Override
        public List<Integer> getVarValues(FieldValues varFvs) {
            return getMorphVarValues(Protocol.MorphSettings.Dials, varFvs);
        }
    },
    MorphModes {
        public List<FieldValues> getParamValues(FieldValues patchParams) {
            return Protocol.PatchParams.Morphs.subfieldsValueRequired(patchParams);
        }
        public List<NamedParam> mkParams() {
            return mkMorphParams(ModParam.MorphMode, m -> List.of("Knob", m));
        }

        @Override
        public List<Integer> getVarValues(FieldValues varFvs) {
            return getMorphVarValues(Protocol.MorphSettings.Modes, varFvs);
        }
    },
    Gain {
        public List<FieldValues> getParamValues(FieldValues patchParams) {
            return Protocol.PatchParams.SectionGain.subfieldsValueRequired(patchParams);
        }
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.GainVolume, ModParam.GainActiveMuted);
        }

        @Override
        public List<FieldEnum> getFields() {
            return List.of(
                    Protocol.GainSettings.PatchVol,
                    Protocol.GainSettings.ActiveMuted
            );
        }
    },
    Glide {
        public List<FieldValues> getParamValues(FieldValues patchParams) {
            return Protocol.PatchParams.SectionGlides.subfieldsValueRequired(patchParams);
        }
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.Glide, ModParam.GlideSpeed);
        }

        @Override
        public List<FieldEnum> getFields() {
            return List.of(
                    Protocol.GlideSettings.Glide,
                    Protocol.GlideSettings.GlideTime
            );
        }
    },
    Bend {
        public List<FieldValues> getParamValues(FieldValues patchParams) {
            return Protocol.PatchParams.SectionBends.subfieldsValueRequired(patchParams);
        }
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.BendEnable, ModParam.BendSemi);
        }

        @Override
        public List<FieldEnum> getFields() {
            return List.of(
                    Protocol.BendSettings.Bend,
                    Protocol.BendSettings.Semi
            );
        }
    },
    Vibrato {
        public List<FieldValues> getParamValues(FieldValues patchParams) {
            return Protocol.PatchParams.SectionVibratos.subfieldsValueRequired(patchParams);
        }
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.Vibrato, ModParam.VibCents, ModParam.VibRate);
        }

        @Override
        public List<FieldEnum> getFields() {
            return List.of(
                    Protocol.VibratoSettings.Vibrato,
                    Protocol.VibratoSettings.Cents,
                    Protocol.VibratoSettings.Rate
            );
        }
    },
    Arpeggiator {
        public List<FieldValues> getParamValues(FieldValues patchParams) {
            return Protocol.PatchParams.SectionArps.subfieldsValueRequired(patchParams);
        }
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.ArpEnable, ModParam.ArpTime, ModParam.ArpDir, ModParam.ArpOctaves);
        }

        @Override
        public List<FieldEnum> getFields() {
            return List.of(
                    Protocol.ArpSettings.Arpeggiator,
                    Protocol.ArpSettings.Time,
                    Protocol.ArpSettings.Type,
                    Protocol.ArpSettings.Octaves
            );
        }
    },
    Misc {
        public List<FieldValues> getParamValues(FieldValues patchParams) {
            return Protocol.PatchParams.SectionMisc.subfieldsValueRequired(patchParams);
        }
        public List<NamedParam> mkParams() {
            return mkParams(ModParam.MiscOctShift, ModParam.MiscSustain);
        }

        @Override
        public List<FieldEnum> getFields() {
            return List.of(
                    Protocol.OctSustainSettings.OctShift,
                    Protocol.OctSustainSettings.Sustain
            );
        }
    };

    public static final String[] MORPH_LABELS =
            {"Wheel","Vel","Keyb","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick","G.Wh 2"};

    public abstract List<FieldValues> getParamValues(FieldValues patchParams);

    public abstract List<NamedParam> mkParams();

    protected static List<NamedParam> mkParams(ModParam... params) {
        return Arrays.stream(params).map(NamedParam::new).toList();
    }

    protected List<NamedParam> mkMorphParams(ModParam modParam, Function<String,List<String>> labelF) {
        return Arrays.stream(MORPH_LABELS).map(m -> new NamedParam(modParam, m, labelF.apply(m))).toList();
    }

    public List<FieldEnum> getFields() {
        throw new UnsupportedOperationException("getFields");
    }

    public List<Integer> getVarValues(final FieldValues varFvs) {
        return getFields().stream().map(f -> f.intValueRequired(varFvs)).toList();
    }

    protected List<Integer> getMorphVarValues(Protocol.MorphSettings ms, FieldValues morph) {
        List<FieldValues> dials = ms.subfieldsValueRequired(morph);
        return dials.stream().map(Protocol.Data7.Datum::intValueRequired).toList();
    }

}
