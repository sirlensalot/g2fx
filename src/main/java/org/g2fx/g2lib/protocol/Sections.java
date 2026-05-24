package org.g2fx.g2lib.protocol;

import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import static org.g2fx.g2lib.state.AreaId.*;

public enum Sections {
    // Perf sections
    SPerformanceName_29(Protocol.EntryName.FIELDS,0x29), // no length
    SPerformanceSettings_11(Protocol.PerformanceSettings.FIELDS,0x11),
    SGlobalKnobAssignments_5f(Protocol.GlobalKnobAssignments.FIELDS,0x5f),

    // Patch Sections
    SPatchDescription_21(Protocol.PatchDescription.FIELDS, 0x21),
    SModuleList1_4a(Protocol.ModuleList.FIELDS, 0x4a, Voice),
    SModuleList0_4a(Protocol.ModuleList.FIELDS, 0x4a, Fx),
    SCurrentNote_69(Protocol.CurrentNote.FIELDS, 0x69),
    SCableList1_52(Protocol.CableList.FIELDS, 0x52, Voice),
    SCableList0_52(Protocol.CableList.FIELDS, 0x52, Fx),
    SPatchParams_4d(Protocol.ModuleParams.FIELDS, 0x4d, Settings),
    SModuleParams1_4d(Protocol.ModuleParams.FIELDS, 0x4d, Voice),
    SModuleParams0_4d(Protocol.ModuleParams.FIELDS, 0x4d, Fx),
    SMorphParameters_65(Protocol.MorphParameters.FIELDS, 0x65),
    SKnobAssignments_62(Protocol.KnobAssignments.FIELDS, 0x62),
    SControlAssignments_60(Protocol.ControlAssignments.FIELDS, 0x60),
    SMorphLabels_5b(Protocol.MorphLabels.FIELDS, 0x5b, Settings),
    SModuleLabels1_5b(Protocol.ModuleLabels.FIELDS, 0x5b, Voice),
    SModuleLabels0_5b(Protocol.ModuleLabels.FIELDS, 0x5b, Fx),
    SModuleNames1_5a(Protocol.ModuleNames.FIELDS, 0x5a, Voice),
    SModuleNames0_5a(Protocol.ModuleNames.FIELDS, 0x5a, Fx),
    STextPad_6f(Protocol.TextPad.FIELDS, 0x6f),
    SPatchName_27(Protocol.EntryName.FIELDS,0x27),

    SPatchLoadData_72(Protocol.PatchLoadData.FIELDS,0x72);
    /**
     * Sections for inbound USB patch messages.
     */
    public static final Sections[] MSG_SECTIONS = new Sections[] {
            SPatchDescription_21,
            SModuleList1_4a,
            SModuleList0_4a,
            SCableList1_52,
            SCableList0_52,
            SPatchParams_4d,
            SModuleParams1_4d,
            SModuleParams0_4d,
            SMorphParameters_65,
            SKnobAssignments_62,
            SControlAssignments_60,
            SModuleNames1_5a,
            SModuleNames0_5a,
            SMorphLabels_5b,
            SModuleLabels1_5b,
            SModuleLabels0_5b
    };
    /**
     * Sections for patch/perf files and outbound perf usb (patch confirm TODO)
     */
    public static final Sections[] FILE_SECTIONS = new Sections[] {
            SPatchDescription_21,
            SModuleList1_4a,
            SModuleList0_4a,
            SCurrentNote_69,
            SCableList1_52,
            SCableList0_52,
            SPatchParams_4d,
            SModuleParams1_4d,
            SModuleParams0_4d,
            SMorphParameters_65,
            SKnobAssignments_62,
            SControlAssignments_60,
            SMorphLabels_5b,
            SModuleLabels1_5b,
            SModuleLabels0_5b,
            SModuleNames1_5a,
            SModuleNames0_5a,
            STextPad_6f
    };

    private static final Logger log = Util.getLogger(Sections.class);

    public final Fields fields;
    public final int type;
    public final AreaId area;

    Sections(Fields fields, int type, AreaId area) {
        this.fields = fields;
        this.type = type;
        this.area = area;
    }

    Sections(Fields fields, int type) {
        this.fields = fields;
        this.type = type;
        this.area = null;
    }


    public record Section(Sections sections, FieldValues values) { }

    /**
     * Read/enforce type byte, then perform
     * {@link BitBuffer#sliceAheadLength(ByteBuffer)}
     */
    public static BitBuffer sliceAheadSection(Sections s, ByteBuffer buf) {
        int t = buf.get();
        if (t != s.type) {
            throw new IllegalArgumentException(String.format("Section incorrect %s %x %x",s,s.type,t));
        }
        return BitBuffer.sliceAheadLength(buf);
    }

    public static void writeSection(ByteBuffer buf, Section ss) throws Exception {
        writeSection(buf,ss.sections,ss.values);
    }

    public static void writeSection(ByteBuffer buf, Sections s, FieldValues fvs) throws Exception {

        buf.put((byte) s.type);

        BitBuffer bb = new BitBuffer(0xffff); //TODO need dynamic allocation or reuse

        s.writeLocation(bb);

        fvs.write(bb);

        ByteBuffer bbuf = bb.toBuffer();
        Util.putShort(buf,bbuf.limit());

        bbuf.rewind();
        while(bbuf.hasRemaining()) {
            buf.put(bbuf.get());
        }

    }

    public void writeLocation(BitBuffer b) throws Exception {
        if (area != null) { area.writeLocation(b); }
    }


    @Override
    public String toString() {
        return String.format("%s[%x%s]",
                name(),
                type,
                area != null ? (":" + area.name()) : "");
    }
}
