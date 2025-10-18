package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;

public enum Sections {
    // Perf sections
    SPerformanceName_29(Protocol.EntryName.FIELDS,0x29), // no length
    SPerformanceSettings_11(Protocol.PerformanceSettings.FIELDS,0x11),
    SGlobalKnobAssignments_5f(Protocol.GlobalKnobAssignments.FIELDS,0x5f),

    // Patch Sections
    SPatchDescription_21(Protocol.PatchDescription.FIELDS, 0x21),
    SModuleList1_4a(Protocol.ModuleList.FIELDS, 0x4a, 1),
    SModuleList0_4a(Protocol.ModuleList.FIELDS, 0x4a, 0),
    SCurrentNote_69(Protocol.CurrentNote.FIELDS, 0x69),
    SCableList1_52(Protocol.CableList.FIELDS, 0x52, 1),
    SCableList0_52(Protocol.CableList.FIELDS, 0x52, 0),
    SPatchParams_4d(Protocol.ModuleParams.FIELDS, 0x4d, 2),
    SModuleParams1_4d(Protocol.ModuleParams.FIELDS, 0x4d, 1),
    SModuleParams0_4d(Protocol.ModuleParams.FIELDS, 0x4d, 0),
    SMorphParameters_65(Protocol.MorphParameters.FIELDS, 0x65),
    SKnobAssignments_62(Protocol.KnobAssignments.FIELDS, 0x62),
    SControlAssignments_60(Protocol.ControlAssignments.FIELDS, 0x60),
    SMorphLabels_5b(Protocol.MorphLabels.FIELDS, 0x5b, 2),
    SModuleLabels1_5b(Protocol.ModuleLabels.FIELDS, 0x5b, 1),
    SModuleLabels0_5b(Protocol.ModuleLabels.FIELDS, 0x5b, 0),
    SModuleNames1_5a(Protocol.ModuleNames.FIELDS, 0x5a, 1),
    SModuleNames0_5a(Protocol.ModuleNames.FIELDS, 0x5a, 0),
    STextPad_6f(Protocol.TextPad.FIELDS, 0x6f),
    SPatchName_27(Protocol.EntryName.FIELDS,0x27),

    SPatchLoadData_72(Protocol.PatchLoadData.FIELDS,0x72);

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


    public record Section(Sections sections, FieldValues values) { }

    /**
     * A protocol "Section" starts with bytes [type,lmsb,llsb]
     * with LMSB+LLSB being the section length, then content.
     * @return a BitBuffer sliced at content start.
     */
    public static BitBuffer sliceSection(Sections s, ByteBuffer buf) {
        int t = buf.get();
        if (t != s.type) {
            throw new IllegalArgumentException(String.format("Section incorrect %s %x %x",s,s.type,t));
        }
        return BitBuffer.sliceAhead(buf, Util.getShort(buf));
    }

    public static void writeSection(ByteBuffer buf, Section ss) throws Exception {
        writeSection(buf,ss.sections,ss.values);
    }

    public static void writeSection(ByteBuffer buf, Sections s, FieldValues fvs) throws Exception {

        buf.put((byte) s.type);

        BitBuffer bb = new BitBuffer(0xffff); //TODO need dynamic allocation or reuse

        if (s.location != null) {
            bb.put(2, s.location);
        }
        fvs.write(bb);

        bb.dumpToBuffer(buf);

    }


    @Override
    public String toString() {
        return String.format("%s[%x%s]",
                name(),
                type,
                location != null ? (":" + location) : "");
    }
}
