package g2lib.state;

import g2lib.BitBuffer;
import g2lib.Util;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;
import g2lib.protocol.Sections;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Performance {
    private final int version;

    public static final Sections[] FILE_SECTIONS = new Sections[] {
            Sections.SPerformanceSettings,
            // patch descriptions TODO
            Sections.SGlobalKnobAssignments
    };

    public static final Sections[] MSG_SECTIONS = new Sections[] {
            Sections.SPerformanceName, // or special-case in readMsg since no length
            Sections.SPerformanceSettings
    };

    private FieldValues perfName;
    private FieldValues perfSettings;
    private FieldValues globalKnobAssignments;
    private List<Patch> slots = new ArrayList<>(4);

    public Performance(byte version) {
        this.version = Util.b2i(version);
    }

    public static Performance readFromMessage(byte version, ByteBuffer buf) {
        Performance perf = new Performance(version);
        buf.get(); //0x01 TODO expectWarn
        buf.get(); //0x0c
        buf.get(); //perf version 00
        buf.get(); //0x29, perf name
        BitBuffer bb = new BitBuffer(buf.slice());
        perf.perfName = Protocol.PerformanceName.FIELDS.read(bb);
        ByteBuffer buf1 = bb.slice();
        buf1.get(); //0x11 perf settings TODO readSection
        bb = BitBuffer.sliceAhead(buf1,Util.getShort(buf1));
        perf.perfSettings = Protocol.PerformanceSettings.FIELDS.read(bb);
        return perf;
    }

    public FieldValues getPerfName() {
        return perfName;
    }

    public FieldValues getPerfSettings() {
        return perfSettings;
    }
}
