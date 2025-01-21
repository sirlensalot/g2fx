package g2lib.protocol;

public enum Sections {
    // Perf sections
    SPerformanceName(Protocol.EntryName.FIELDS,0x29), // no length
    SPerformanceSettings(Protocol.PerformanceSettings.FIELDS,0x11),
    SGlobalKnobAssignments(Protocol.KnobAssignment.FIELDS_PERF,0x5f),

    // Patch Sections
    SPatchDescription(Protocol.PatchDescription.FIELDS, 0x21),
    SModuleList1(Protocol.ModuleList.FIELDS, 0x4a, 1),
    SModuleList0(Protocol.ModuleList.FIELDS, 0x4a, 0),
    SCurrentNote(Protocol.CurrentNote.FIELDS, 0x69),
    SCableList1(Protocol.CableList.FIELDS, 0x52, 1),
    SCableList0(Protocol.CableList.FIELDS, 0x52, 0),
    SPatchParams(Protocol.PatchParams.FIELDS, 0x4d, 2),
    SModuleParams1(Protocol.ModuleParams.FIELDS, 0x4d, 1),
    SModuleParams0(Protocol.ModuleParams.FIELDS, 0x4d, 0),
    SMorphParameters(Protocol.MorphParameters.FIELDS, 0x65),
    SKnobAssignments(Protocol.KnobAssignments.FIELDS_PATCH, 0x62),
    SControlAssignments(Protocol.ControlAssignments.FIELDS, 0x60),
    SMorphLabels(Protocol.MorphLabels.FIELDS, 0x5b, 2),
    SModuleLabels1(Protocol.ModuleLabels.FIELDS, 0x5b, 1),
    SModuleLabels0(Protocol.ModuleLabels.FIELDS, 0x5b, 0),
    SModuleNames1(Protocol.ModuleNames.FIELDS, 0x5a, 1),
    SModuleNames0(Protocol.ModuleNames.FIELDS, 0x5a, 0),
    STextPad(Protocol.TextPad.FIELDS, 0x6f),
    SPatchName(Protocol.EntryName.FIELDS,0x27);

    public final Fields fields;
    public final int type;
    public final Integer location;

    Sections(Fields fields, int type, int location) {
        this.fields = fields;
        this.type = type;
        this.location = location;
    }

    Sections(Fields fields, int type) {
        this.fields = fields;
        this.type = type;
        this.location = null;
    }

    @Override
    public String toString() {
        return String.format("%s[%x%s]",
                name(),
                type,
                location != null ? (":" + location) : "");
    }
}
