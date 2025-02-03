package g2lib.state;

import g2lib.BitBuffer;
import g2lib.CRC16;
import g2lib.Util;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;
import g2lib.protocol.Sections;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

import static g2lib.state.Patch.fileHeader;
import static g2lib.state.Patch.verifyFileHeader;

public class Performance {

    public static final ByteBuffer HEADER = fileHeader(86, new String[]{
            "Version=Nord Modular G2 File Format 1",
            "Type=Performance",
            "Version=23",
            "Info=BUILD 320"
    });

    private final int version;

    private FieldValues perfName;
    private String fileName;
    private PerformanceSettings perfSettings;
    private FieldValues globalKnobAssignments;
    private Map<Integer,Patch> slots = new TreeMap<>();

    public Performance(byte version) {
        this.version = Util.b2i(version);
    }

    public static Performance readFromFile(String filePath) throws Exception {
        ByteBuffer fileBuffer = verifyFileHeader(filePath, HEADER);

        ByteBuffer slice = fileBuffer.slice();
        int crc = CRC16.crc16(slice,0,slice.limit()-2);

        Util.expectWarn(fileBuffer,0x17,filePath,"header terminator");
        Performance perf = new Performance(fileBuffer.get());
        perf.perfSettings = new PerformanceSettings(
                readSection(fileBuffer,Sections.SPerformanceSettings));
        for (int s = 0; s < 4; s++) {
            Patch patch = new Patch();
            patch.version = 0; //TODO source?

            for (Sections ss : Patch.FILE_SECTIONS) {
                patch.readSection(fileBuffer,ss);
            }
            perf.slots.put(s,patch);
        }
        perf.globalKnobAssignments = readSection(fileBuffer,Sections.SGlobalKnobAssignments);

        return perf;
    }

    public Performance readFromMessage(ByteBuffer buf) {
        //String s = Util.dumpBufferString(buf);
        Util.expectWarn(buf,0x01,"Message","Cmd 0x01");
        Util.expectWarn(buf,0x0c,"Message","Cmd 0x0c");
        Util.expectWarn(buf,version,"Message","Perf version");
        Util.expectWarn(buf,Sections.SPerformanceName.type,"Message","Perf name");
        BitBuffer bb = new BitBuffer(buf.slice());
        perfName = Protocol.EntryName.FIELDS.read(bb);
        perfSettings = new PerformanceSettings(
                readSection(bb.slice(),Sections.SPerformanceSettings));
        return this;
    }

    private static FieldValues readSection(ByteBuffer buf, Sections s) {
        return s.fields.read(Util.sliceSection(s,buf));
    }


    public String getName() {
        return perfName == null ? fileName : Protocol.EntryName.Name.stringValueRequired(perfName);
    }

    public void setName(String name) {
        perfName.update(Protocol.EntryName.Name.value(name));
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public PerformanceSettings getPerfSettings() {
        return perfSettings;
    }

    public void setPatch(int slot, Patch patch) {
        slots.put(slot,patch);
    }

    public Patch getSlot(int slot) {
        return slots.get(slot);
    }
}
