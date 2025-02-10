package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;
import g2lib.protocol.Sections;
import g2lib.util.BitBuffer;
import g2lib.util.CRC16;
import g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import static g2lib.state.Patch.fileHeader;
import static g2lib.state.Patch.verifyFileHeader;

public class Performance {

    private static final Logger log = Util.getLogger(Performance.class);

    public static final ByteBuffer HEADER = fileHeader(86, new String[]{
            "Version=Nord Modular G2 File Format 1",
            "Type=Performance",
            "Version=23",
            "Info=BUILD 320"
    });

    private int version;

    private FieldValues perfName;
    private String fileName;
    private PerformanceSettings perfSettings;
    private GlobalKnobAssignments globalKnobAssignments;
    private final Map<Slot,Patch> slots = new TreeMap<>();

    public Performance() {
        for (Slot s : Slot.values()) {
            slots.put(s,new Patch(s));
        }
    }

    public int getVersion() {
        return version;
    }

    public static Performance readFromFile(String filePath) throws Exception {
        ByteBuffer fileBuffer = verifyFileHeader(filePath, HEADER);

        ByteBuffer slice = fileBuffer.slice();
        int crc = CRC16.crc16(slice,0,slice.limit()-2);

        Util.expectWarn(fileBuffer,0x17,filePath,"header terminator");
        Performance perf = new Performance();
        perf.setVersion(fileBuffer.get());
        perf.perfSettings = new PerformanceSettings(
                readSectionSlice(fileBuffer,Sections.SPerformanceSettings));
        for (Slot s : Slot.values()) {
            Patch patch = new Patch(s);
            patch.version = 0; //TODO source?

            for (Sections ss : Patch.FILE_SECTIONS) {
                patch.readSection(fileBuffer,ss);
            }
            perf.slots.put(s,patch);
        }
        perf.globalKnobAssignments = new GlobalKnobAssignments(
                readSectionSlice(fileBuffer,Sections.SGlobalKnobAssignments));

        return perf;
    }

    public Performance readFromMessage(ByteBuffer buf) {
        readPerfMsgHeader(buf.rewind());
        Util.expectWarn(buf,Sections.SPerformanceName.type,"Message","Perf name");
        readPerformanceNameAndSettings(buf);
        return this;
    }

    public boolean readPerformanceNameAndSettings(ByteBuffer buf) {
        BitBuffer bb = new BitBuffer(buf.slice());
        perfName = Protocol.EntryName.FIELDS.read(bb);
        perfSettings = new PerformanceSettings(
                readSectionSlice(bb.slice(),Sections.SPerformanceSettings));
        log.fine(() -> "readPerformanceNameAndSettings");
        return true;
    }

    private static FieldValues readSectionSlice(ByteBuffer buf, Sections s) {
        return s.fields.read(Sections.sliceSection(s,buf));
    }

    private void readPerfMsgHeader(ByteBuffer buf) {
        Util.expectWarn(buf,0x01,"Message","Cmd 0x01");
        Util.expectWarn(buf,0x0c,"Message","Cmd 0x0c");
        Util.expectWarn(buf,version,"Message","Perf version");
    }

    public void readSectionMessage(ByteBuffer buf, Sections s) {
        readPerfMsgHeader(buf.rewind());
        FieldValues fvs = readSectionSlice(buf, s);
        updateSection(s, fvs);
    }

    private void updateSection(Sections s, FieldValues fvs) {
        switch (s) {
            case SGlobalKnobAssignments -> this.globalKnobAssignments = new GlobalKnobAssignments(fvs);
        }
    }

    public boolean readSectionSlice(Sections s, BitBuffer bb) {
        updateSection(s,s.fields.read(bb));
        log.fine(() -> "readSectionSlice: " + s);
        return true;
    }

    public GlobalKnobAssignments getGlobalKnobAssignments() {
        return globalKnobAssignments;
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

    public void setPatch(Slot slot, Patch patch) {
        slots.put(slot,patch);
    }

    public Patch getSlot(Slot slot) {
        return slots.get(slot);
    }

    public Patch getSelectedPatch() {
        if (perfSettings == null) { throw new IllegalStateException("Perf settings not initialized"); }
        return getSlot(getSelectedSlot());
    }

    public Slot getSelectedSlot() {
        return Slot.fromIndex(perfSettings.getSelectedSlot());
    }


    public void readAssignedVoicesMsg(ByteBuffer buf) {
        readPerfMsgHeader(buf);
        Util.expectWarn(buf,0x05,"msg","Assigned Voices type");
        readAssignedVoices(buf);
    }

    public boolean readAssignedVoices(ByteBuffer buf) {
        for (Slot s : Slot.values()) {
            getSlot(s).setAssignedVoices(Util.b2i(buf.get()));
        }
        return true;
    }

    public void setVersion(int version) {
        this.version = version;
        log.fine(() -> "setVersion: " + version);
    }
}
