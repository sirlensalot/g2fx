package g2lib.state;

import g2lib.BitBuffer;
import g2lib.Util;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;
import g2lib.protocol.Sections;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    private Map<Integer,Patch> slots = new TreeMap<>();

    public Performance(byte version) {
        this.version = Util.b2i(version);
    }

    public Performance readFromMessage(ByteBuffer buf) {
        Util.expectWarn(buf,0x01,"Message","Cmd 0x01");
        Util.expectWarn(buf,0x0c,"Message","Cmd 0x0c");
        Util.expectWarn(buf,version,"Message","Perf version");
        Util.expectWarn(buf,Sections.SPerformanceName.type,"Message","Perf name");
        BitBuffer bb = new BitBuffer(buf.slice());
        perfName = Protocol.PerformanceName.FIELDS.read(bb);
        ByteBuffer buf1 = bb.slice();
        Util.expectWarn(buf1,Sections.SPerformanceSettings.type, "Message","perf settings");
        bb = BitBuffer.sliceAhead(buf1,Util.getShort(buf1));
        perfSettings = Protocol.PerformanceSettings.FIELDS.read(bb);
        return this;
    }



    public FieldValues getPerfName() {
        return perfName;
    }

    public FieldValues getPerfSettings() {
        return perfSettings;
    }

    public void setPatch(int slot, Patch patch) {
        slots.put(slot,patch);
    }
}
