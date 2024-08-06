package g2lib;

import g2lib.protocol.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Protocol {

    public enum CableList implements FieldEnum {
        Reserved(12),
        CableCount(10),
        Cables(Cable.FIELDS,CableList.CableCount);
        CableList(int size) { f = new SizedField(this,size); }
        CableList(Fields fs, FieldEnum fe) { f = new SubfieldsField(this,fs,fe); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(CableList.class,values());

    }

    public enum Cable implements FieldEnum {
        Color         (3),
        ModuleFrom    (8),
        ConnectorFrom (6),
        LinkType      (1),
        ModuleTo      (8),
        ConnectorTo   (6);
        Cable(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Cable.class,values());
    }

    public enum ModuleModes implements FieldEnum {
        Data;
        final private Field f = new SizedField(this,6);
        @Override
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ModuleModes.class,values());
    }

    public enum ModuleList implements FieldEnum {
        //Location(2),
        ModuleCount(8),
        Modules(Module_.FIELDS,ModuleList.ModuleCount);
        ModuleList(int size) { f = new SizedField(this,size); }
        ModuleList(Fields fs,FieldEnum e) { f = new SubfieldsField(this,fs,e);}
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ModuleList.class,values());

    }

    public enum Module_ implements FieldEnum {

        Id        (8),
        Index     (8),
        Horiz     (7),
        Vert      (7),
        Color     (8),
        Uprate    (1),
        Leds      (1),
        Reserved  (6),
        ModeCount (4),
        Modes     (ModuleModes.FIELDS,ModeCount);

        Module_(int size) { f = new SizedField(this,size); }
        Module_(Fields fs, Module_ ixField) { f = new SubfieldsField(this,fs,ixField); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Module_.class,values());

    }

    public enum PatchDescription implements FieldEnum {
        //skip 7
        Reserved(Data8.FIELDS,7),
        Reserved2(5),
        Voices(5),
        Height(14),
        Unk2(3),
        Red(1),
        Blue(1),
        Yellow(1),
        Orange(1),
        Green(1),
        Purple(1),
        White(1),
        MonoPoly(2),
        Variation(8),
        Category(8);

        PatchDescription(int size) { f = new SizedField(this,size); }
        PatchDescription(Fields fs,int c) { f = new SubfieldsField(this,fs,c); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(PatchDescription.class,values());
    }

    public enum PatchParams implements FieldEnum {

        SectionCount(8),
        VariationCount(8),
        S1(),
        Morphs(MorphSettings.FIELDS,VariationCount),
        S2(),
        Section2(Settings2.FIELDS,VariationCount),
        S3(),
        Section3(Settings3.FIELDS,VariationCount),
        S4(),
        Section4(Settings4.FIELDS,VariationCount),
        S5(),
        Section5(Settings5.FIELDS,VariationCount),
        S6(),
        Section6(Settings6.FIELDS,VariationCount),
        S7(),
        Section7(Settings7.FIELDS,VariationCount);

        PatchParams(int size) { f = new SizedField(this,size); }
        PatchParams(Fields fs, PatchParams p) { f = new SubfieldsField(this,fs,p); }
        PatchParams() { f = new SubfieldsField(this,SectionHeader.FIELDS,1); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(PatchParams.class,values());
    }


    public enum Settings2 implements FieldEnum {
        Variation(8),
        PatchVol(7),
        ActiveMuted(7);
        Settings2(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Settings2.class,values());
    }

    public enum Settings3 implements FieldEnum {
        Variation(8),
        Glide(7),
        GlideTime(7);
        Settings3(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Settings3.class,values());
    }

    public enum Settings4 implements FieldEnum {
        Variation(8),
        Bend(7),
        Semi(7);
        Settings4(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Settings4.class,values());
    }


    public enum Settings5 implements FieldEnum {
        Variation(8),
        Vibrato(7),
        Cents(7),
        Rate(7);
        Settings5(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Settings5.class,values());
    }


    public enum Settings6 implements FieldEnum {
        Variation(8),
        Arpeggiator(7),
        Time(7),
        Type(7),
        Octaves(7);
        Settings6(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Settings6.class,values());
    }


    public enum Settings7 implements FieldEnum {
        Variation(8),
        OctShift(7),
        Sustain(7);
        Settings7(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Settings7.class,values());
    }

    public enum SectionHeader implements FieldEnum {
        Section(8),
        Entries(7);
        SectionHeader(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(SectionHeader.class,values());
    }


    public enum MorphSettings implements FieldEnum {
        Variation(8),
        Dials(),
        Modes();
        MorphSettings(int size) { f = new SizedField(this,size); }
        MorphSettings() { f = new SubfieldsField(this, Data7.FIELDS,8); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(MorphSettings.class,values());
    }

    public enum Data7 implements FieldEnum {
        Datum;
        Data7() { this.f = new SizedField(this,7); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Data7.class,values());
    }

    public enum Data8 implements FieldEnum {
        Datum;
        Data8() { this.f = new SizedField(this,8); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(Data8.class,values());
        public static List<FieldValues> asSubfield(int... values) {
            return Arrays.stream(values).boxed().map(v -> FIELDS.init().add(Datum.value(v))).toList();
        }
    }

    public enum ModuleParams implements FieldEnum {
        SetCount(8),
        VariationCount(8),
        ParamSet(ModuleParams.SetCount);
        ModuleParams(int size) { f = new SizedField(this,size); }
        ModuleParams(ModuleParams ix) { f = new SubfieldsField(this,ModuleParamSet.FIELDS,ix); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ModuleParams.class,values());
    }

    public enum ModuleParamSet implements FieldEnum {
        ModIndex(8),
        ParamCount(7),
        ModParams;

        ModuleParamSet(int size) { f = new SizedField(this,size); }
        ModuleParamSet() { f = new SubfieldsField(this,VarParams.FIELDS,ModuleParams.VariationCount); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ModuleParamSet.class,values());
    }

    public enum VarParams implements FieldEnum {
        Variation(8),
        Params;
        VarParams(int size) { f = new SizedField(this,size); }
        VarParams() { f = new SubfieldsField(this, Data7.FIELDS, ModuleParamSet.ParamCount); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(VarParams.class,values());
    }

    public enum MorphParameters implements FieldEnum {
        VariationCount(8),
        MorphCount(4),
        Reserved(20),
        VarMorphs(MorphParameters.VariationCount);
        MorphParameters(int size) { f = new SizedField(this,size); }
        MorphParameters(MorphParameters ix) { f = new SubfieldsField(this,VarMorph.FIELDS,ix); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(MorphParameters.class,values());
    }

    public enum VarMorph implements FieldEnum {
        Variation(4),
        Reserved0(24),
        Reserved1(24),
        Reserved2(8),
        MorphCount(8),
        VarMorphParams(VarMorph.MorphCount),
        Reserved3(4);
        VarMorph(int size) { f = new SizedField(this,size); }
        VarMorph(VarMorph ix) {
            f = new SubfieldsField(this,VarMorphParam.FIELDS,ix);
        }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(VarMorph.class,values());
    }

    public enum VarMorphParam implements FieldEnum {
        Location   (2),
        ModuleIndex(8),
        ParamIndex (7),
        Morph      (4),
        Range      (8);
        VarMorphParam(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(VarMorphParam.class,values());
    }

    public enum KnobAssignments implements FieldEnum {
        KnobCount(16),
        Knobs(KnobAssignments.KnobCount);
        KnobAssignments(int size) { f = new SizedField(this,size); }
        KnobAssignments(KnobAssignments ix) { f = new SubfieldsField(this,KnobAssignment.FIELDS,ix); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(KnobAssignments.class,values());
    }

    public enum KnobAssignment implements FieldEnum {
        Assigned(1),
        Params(KnobAssignment.Assigned);
        KnobAssignment(int size) { f = new SizedField(this,size); }
        KnobAssignment(KnobAssignment ix) { f = new SubfieldsField(this,KnobParams.FIELDS,ix); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(KnobAssignment.class,values());
    }

    public enum KnobParams implements FieldEnum {
        Location(2),
        Index(8),
        IsLed(7),
        Param(4);
        KnobParams(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(KnobParams.class,values());
    }

    public enum ControlAssignments implements FieldEnum {
        NumControls(7),
        Assignments(ControlAssignment.FIELDS,ControlAssignments.NumControls);
        ControlAssignments(int size) { f = new SizedField(this,size); }
        ControlAssignments(Fields fs,ControlAssignments ix) { f = new SubfieldsField(this,fs,ix); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ControlAssignments.class,values());
    }

    public enum ControlAssignment implements FieldEnum {
        MidiCC(7),
        Location(2),
        Index(8),
        Param(7);
        ControlAssignment(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ControlAssignment.class,values());
    }

    public enum ModuleNames implements FieldEnum {
        Reserved(6),
        NameCount(8),
        Names(ModuleName.FIELDS,ModuleNames.NameCount);
        ModuleNames(int size) { f = new SizedField(this,size); }
        ModuleNames(Fields fs,ModuleNames ix) { f = new SubfieldsField(this,fs,ix); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ModuleNames.class,values());
    }

    public enum ModuleName implements FieldEnum {
        ModuleIndex(8),
        Name;
        ModuleName(int size) { f = new SizedField(this,size); }
        ModuleName() { f = new StringField(this); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ModuleName.class,values());
    }

    public enum MorphLabels implements FieldEnum {
        LabelCount(8),
        Entry(8),
        Length(8),
        Labels(MorphLabel.FIELDS,MorphLabels.LabelCount);
        MorphLabels(int size) { f = new SizedField(this,size); }
        MorphLabels(Fields fs,MorphLabels ix) { f = new SubfieldsField(this,fs,8); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(MorphLabels.class,values());
    }


    public enum MorphLabel implements FieldEnum {
        Index(8),
        Length(8),
        Entry(8),
        Label();
        MorphLabel(int size) { f = new SizedField(this,size); }
        MorphLabel() { f = new StringField(this,6); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(MorphLabel.class,values());

    }

    public enum CurrentNote implements FieldEnum {
        Note(7),
        Attack(7),
        Release(7),
        NoteCount(5),
        Notes(NoteData.FIELDS,CurrentNote.NoteCount);
        CurrentNote(int size) { f = new SizedField(this,size); }
        CurrentNote(Fields fs,FieldEnum e) {
            final SubfieldsField.FieldCount c = new SubfieldsField.FieldCount(e);
            f = new SubfieldsField(this, fs, new SubfieldsField.SubfieldCount() {
                @Override
                public int getCount(List<FieldValues> values) {
                    return c.getCount(values) + 1;
                }
            });
        }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(CurrentNote.class,values());
    }

    public enum NoteData implements FieldEnum {
        Note(7),
        Attack(7),
        Release(7);
        NoteData(int size) { f = new SizedField(this,size); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(NoteData.class,values());
    }

    public enum ModuleLabels implements FieldEnum {
        ModuleCount(8),
        ModLabels(ModuleLabel.FIELDS,ModuleLabels.ModuleCount);
        ModuleLabels(int size) { f = new SizedField(this,size); }
        ModuleLabels(Fields fs,FieldEnum e) { f = new SubfieldsField(this,fs,e); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ModuleLabels.class,values());
    }

    public enum ModuleLabel implements FieldEnum {
        ModuleIndex(8),
        ModLabelLen(8),
        Labels(ParamLabel.FIELDS,ModuleLabel.ModLabelLen);
        ModuleLabel(int size) { f = new SizedField(this,size); }
        ModuleLabel(Fields fs,FieldEnum e) {
            final SubfieldsField.FieldCount fc = new SubfieldsField.FieldCount(e);
            f = new SubfieldsField(this, fs, new SubfieldsField.SubfieldCount() {
                @Override
                public int getCount(List<FieldValues> values) {
                    return fc.getCount(values)/7;
                }
            });
        }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ModuleLabel.class,values());
    }

    public enum ParamLabel implements FieldEnum {
        IsString(8),
        ParamLen(8),
        ParamIndex(8),
        Label();
        ParamLabel(int size) { f = new SizedField(this,size); }
        ParamLabel() { f = new StringField(this,6); }
        private final Field f;
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(ParamLabel.class,values());
    }

    public enum TextPad implements FieldEnum {
        Text;
        private final Field f = new StringField(this,StringField.NO_TERMINATION);
        public Field field() { return f; }
        public static final Fields FIELDS = new Fields(TextPad.class,values());
    }

}
