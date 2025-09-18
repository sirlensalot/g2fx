package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.Visual;
import org.g2fx.g2lib.protocol.FieldValue;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.UsbMessage;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.CRC16;
import org.g2fx.g2lib.util.Util;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.g2fx.g2lib.util.Util.forEachIndexed;

public class Patch {

    private final Logger log;

    public static ByteBuffer fileHeader(int bufSize, String[] headerMsg) {
        ByteBuffer header = ByteBuffer.allocate(bufSize);
        for (String s : headerMsg) {
            for (char c : s.toCharArray()) {
                header.put((byte) c);
            }
            header.put((byte)0x0d).put((byte)0x0a);
        }
        header.put((byte)0);
        header.rewind();
        return header.asReadOnlyBuffer();
    }

    public static final ByteBuffer HEADER = fileHeader(80, new String[]{
            "Version=Nord Modular G2 File Format 1",
            "Type=Patch",
            "Version=23",
            "Info=BUILD 320"
    });

    public record Section(Sections sections, FieldValues values) { }

    public static final Sections[] FILE_SECTIONS = new Sections[] {
            Sections.SPatchDescription,
            Sections.SModuleList1,
            Sections.SModuleList0,
            Sections.SCurrentNote,
            Sections.SCableList1,
            Sections.SCableList0,
            Sections.SPatchParams,
            Sections.SModuleParams1,
            Sections.SModuleParams0,
            Sections.SMorphParameters,
            Sections.SKnobAssignments,
            Sections.SControlAssignments,
            Sections.SMorphLabels,
            Sections.SModuleLabels1,
            Sections.SModuleLabels0,
            Sections.SModuleNames1,
            Sections.SModuleNames0,
            Sections.STextPad
    };

    public static final Sections[] MSG_SECTIONS = new Sections[] {
            Sections.SPatchDescription,
            Sections.SModuleList1,
            Sections.SModuleList0,
            Sections.SCableList1,
            Sections.SCableList0,
            Sections.SPatchParams,
            Sections.SModuleParams1,
            Sections.SModuleParams0,
            Sections.SMorphParameters,
            Sections.SKnobAssignments,
            Sections.SControlAssignments,
            Sections.SModuleNames1,
            Sections.SModuleNames0,
            Sections.SMorphLabels,
            Sections.SModuleLabels1,
            Sections.SModuleLabels0
    };

    public final LinkedHashMap<Sections,Section> sections = new LinkedHashMap<>();
    private LibProperty<String> name;
    private final Slot slot;
    private int version;


    private PatchSettings patchSettings;
    private FieldValues textPad;
    private FieldValues currentNote;
    private final PatchArea voiceArea;
    private final PatchArea fxArea;
    private final PatchArea settingsArea;
    private KnobAssignments knobAssignments;
    private ControlAssignments controls;
    private MorphParameters morphParams;
    private LibProperty<Integer> assignedVoices = new LibProperty<>(0);
    private final List<PatchVisual> leds = new ArrayList<>();
    private final List<PatchVisual> metersAndGroups = new ArrayList<>();

    // file-perf
    public Patch(Slot slot) {
        this.log = Util.getLogger(getClass().getName() + ":" + slot);
        this.slot = slot;
        voiceArea = new PatchArea(slot,AreaId.Voice);
        fxArea = new PatchArea(slot,AreaId.Fx);
        settingsArea = new PatchArea(slot);
    }

    public int getVersion() {
        return version;
    }

    public Slot getSlot() {
        return slot;
    }

    public List<PatchVisual> getLeds() {
        return leds;
    }

    public List<PatchVisual> getMetersAndGroups() {
        return metersAndGroups;
    }

    // usb, file-perf, test, file-patch
    public void setVersion(int version) {
        this.version = version;
        log.info(() -> "setVersion: " + version);
    }

    public static <T> T withSliceAhead(ByteBuffer buf, int length, Function<ByteBuffer,T> f) {
        return f.apply(Util.sliceAhead(buf,length));
    }

    public KnobAssignments getKnobAssignments() {
        return knobAssignments;
    }

    // usb, test
    public void readPatchDescription(ByteBuffer buf) {
        for (Sections ss : MSG_SECTIONS) {
            readSection(buf,ss);
            if (ss == Sections.SPatchDescription) {
                Util.expectWarn(buf,0x2d,"Message","USB extra 1");
                Util.expectWarn(buf,0x00,"Message","USB extra 2");
            }
        }
        updateVisualIndex();
    }

    // test
    public static Patch readFromMessage(int version, Slot slot, ByteBuffer buf) {
        Patch patch = new Patch(slot);
        patch.setVersion(version);
        patch.readMessageHeader(buf);
        patch.readPatchDescription(buf);
        return patch;
    }

    public PatchArea getArea(AreaId areaId) {
        return switch (areaId) {
            case Fx -> fxArea;
            case Voice -> voiceArea;
            case Settings -> settingsArea;
        };
    }

    public PatchArea getSettingsArea() { return getArea(AreaId.Settings); }

    public PatchArea getArea(int index) {
        return switch (index) {
            case 0 -> fxArea;
            case 1 -> voiceArea;
            case 2 -> settingsArea;
            default -> throw new IllegalArgumentException("Invalid area index: " + index);
        };
    }

    // test
    public void readMessageHeader(ByteBuffer buf) {
        Util.expectWarn(buf,0x01,"Message","Cmd");
        int slot = buf.get();
        if (!this.slot.testSlotId(slot)) {
            throw new IllegalArgumentException(String.format("Slot mismatch: %s, %d",this.slot,slot));
        }
        int version = buf.get();
        if (this.version != version) {
            throw new IllegalArgumentException(String.format("Slot version mismatch: %s, %d",this.version,version));
        }
    }

    public void writeMessageHeader(ByteBuffer buf) {
        buf.put(Util.asBytes(0x01,slot.ordinal()+8,version));
    }

    // file-patch
    public static Patch readFromFile(Slot slot, String filePath) throws Exception {
        ByteBuffer fileBuffer = verifyFileHeader(filePath, HEADER);

        ByteBuffer slice = fileBuffer.slice();
        int crc = CRC16.crc16(slice,0,slice.limit()-2);

        Util.expectWarn(fileBuffer,0x17,filePath,"header terminator");
        Patch patch = new Patch(slot);
        patch.setVersion(fileBuffer.get());

        patch.readFileSections(fileBuffer);

        int fcrc = Util.getShort(fileBuffer);
        if (fcrc != crc) {
            throw new RuntimeException(String.format("CRC mismatch: %x %x",crc,fcrc));
        }

        return patch;
    }

    // file-patch, file-perf
    public void readFileSections(ByteBuffer fileBuffer) {
        for (Sections ss : FILE_SECTIONS) {
            readSection(fileBuffer,ss);
        }
        updateVisualIndex();
    }


    public static ByteBuffer verifyFileHeader(String filePath, ByteBuffer header) throws Exception {
        ByteBuffer fileBuffer = Util.readFile(filePath);
        withSliceAhead(fileBuffer, header.limit(), buf -> {
            if (!header.rewind().equals(buf.rewind())) {
                throw new RuntimeException("Unexpected file header: " + Util.dumpBufferString(buf));
            }
            return true;
        });
        return fileBuffer;
    }


    public void writeSection(ByteBuffer buf, Sections s) throws  Exception {
        Section ss = getSection(s);
        if (ss == null) {
            throw new IllegalArgumentException("No section in patch: " + s);
        }
        BitBuffer bb = new BitBuffer(1024);
        if (s.location != null) {
            bb.put(2,s.location);
        }
        FieldValues fvs = ss.values;
        for (FieldValue fv : fvs.values) {
            fv.write(bb);
        }
        ByteBuffer bbuf = bb.toBuffer();
//        log.info(String.format("Wrote: %s, len=%x, crc=%x: %s\n",s,bb.limit(),CRC16.crc16(bbuf),Util.dumpBufferString(bbuf)));

        buf.put((byte) s.type);
        Util.putShort(buf,bbuf.limit());
        bbuf.rewind();
        while(bbuf.hasRemaining()) {
            buf.put(bbuf.get());
        }

    }

    public ByteBuffer writeMessage() throws Exception {

        ByteBuffer buf = ByteBuffer.allocateDirect(2048);

        writeMessageHeader(buf);
        for (Sections s : MSG_SECTIONS) {
            writeSection(buf,s);
            if (s == Sections.SPatchDescription) {
                buf.put(Util.asBytes(0x2d,0x00));
            }
        }
        buf.limit(buf.position());
        int crc = CRC16.crc16(buf.rewind());
        buf.position(buf.limit());
        buf.limit(buf.position()+2);
        //log.info(String.format("%x",crc));
        Util.putShort(buf,crc);
        return buf;
    }

    public ByteBuffer writeFile() throws Exception {
        ByteBuffer buf = ByteBuffer.allocateDirect(2048);
        buf.put(HEADER.rewind());
        int start = buf.position();
        if (version == -1) {
            throw new RuntimeException("writeFile: version not initialized");
        }
        buf.put(Util.asBytes(0x17,version));
        for (Sections s : FILE_SECTIONS) {
            writeSection(buf,s);
        }
        buf.limit(buf.position());
        buf.rewind();
        int crc = CRC16.crc16(buf,start,buf.limit()-start);
        buf.position(buf.limit());
        buf.limit(buf.position()+2);
        Util.putShort(buf,crc);
        return buf;
    }


    // file-perf, usb, file-patch
    public void readSection(ByteBuffer buf, Sections s) {
        BitBuffer bb = Sections.sliceSection(s,buf);
        //log.info(s + ": length " + bb.limit());
        readSectionSlice(bb, s);
    }

    // usb, and via readSection
    public boolean readSectionSlice(BitBuffer bb, Sections s) {
        int startIx = bb.getBitIndex();
        if (s.location != null) {
            Integer loc = bb.get(2);
            if (!loc.equals(s.location)) {
                throw new IllegalArgumentException(String.format("Bad location: %x, %s",loc, s));
            }
        }
        FieldValues fvs;
        try {
            fvs = s.fields.read(bb);
        } catch (RuntimeException e) {
            File file = new File(String.format("data/error_%s.msg",s.name()));
            log.severe(String.format("Error reading section %s, dumping buffer to %s",s,file));
            ByteBuffer data = bb.setBitIndex(startIx).shiftedSlice();
            try { Util.writeBuffer(data,file); } catch (Exception ignored) {}
            throw e;
        }
        updateSection(s, new Section(s, fvs));
        return true;
    }

    // via readSectionSlice only
    private void updateSection(Sections s, Section section) {
        sections.put(s, section);
        log.info("updateSection: " + s);
        switch (s) {
            case SPatchDescription ->
                this.patchSettings = new PatchSettings(section.values);
            case SPatchParams -> settingsArea.setUserModuleParams(section.values);
            case STextPad -> this.textPad = section.values;
            case SCurrentNote -> this.currentNote = section.values;
            case SModuleList0 -> fxArea.addModules(section.values);
            case SModuleList1 -> voiceArea.addModules(section.values);
            case SModuleParams0 -> fxArea.setUserModuleParams(section.values);
            case SModuleParams1 -> voiceArea.setUserModuleParams(section.values);
            case SModuleLabels0 -> fxArea.setModuleLabels(section.values);
            case SModuleLabels1 -> voiceArea.setModuleLabels(section.values);
            case SModuleNames0 -> fxArea.setModuleNames(section.values);
            case SModuleNames1 -> voiceArea.setModuleNames(section.values);
            case SCableList0 -> fxArea.addCables(section.values);
            case SCableList1 -> voiceArea.addCables(section.values);
            case SMorphLabels -> settingsArea.setMorphLabels(section.values);
            case SKnobAssignments -> this.knobAssignments = new KnobAssignments(section.values,slot);
            case SControlAssignments -> this.controls = new ControlAssignments(section.values);
            case SMorphParameters -> this.morphParams = new MorphParameters(section.values);
            case SPatchName -> this.name = LibProperty.stringFieldProperty(section.values, Protocol.EntryName.Name);
        }
    }

    // test
    public void readPatchLoadDataMsg(UsbMessage msg) {
        ByteBuffer buf = msg.getBufferx();
        readMessageHeader(buf);
        Util.expectWarn(buf,Sections.SPatchLoadData.type,"msg","PatchLoad");
        readPatchLoadData(buf);
    }

    // usb
    public boolean readPatchLoadData(ByteBuffer buf) {
        FieldValues fvs = Protocol.PatchLoadData.FIELDS.read(new BitBuffer(buf.slice()));
        getArea(Protocol.PatchLoadData.Location.intValue(fvs)).setPatchLoadData(fvs);
        return true;
    }

    // usb
    public boolean readSelectedParam(ByteBuffer buf) {
        FieldValues fvs = Protocol.SelectedParam.FIELDS.read(new BitBuffer(buf.slice()));
        getArea(Protocol.SelectedParam.Location.intValue(fvs)).setSelectedParam(fvs);
        return true;
    }


    // test
    public void readSectionMessage(ByteBuffer buf, Sections s) {
        readMessageHeader(buf);
        readSection(buf,s);
    }

    // usb, file-patch
    private void updateVisualIndex() {
        leds.clear();
        voiceArea.addVisuals(Visual.VisualType.Led,leds);
        fxArea.addVisuals(Visual.VisualType.Led,leds);
        log.info(() -> "leds: " + leds);

        metersAndGroups.clear();
        voiceArea.addVisuals(null,metersAndGroups);
        fxArea.addVisuals(null,metersAndGroups);
        log.info(() -> "metersAndGroups: " + metersAndGroups);
    }

    // usb
    public boolean readVolumeData(ByteBuffer buf) {
        List<PatchVisual> updated = new ArrayList<>();
        metersAndGroups.forEach(v -> {
            buf.get(); // unknown
            int i = Util.b2i(buf.get());
            if (v.update(i)) {
                updated.add(v);
            }
        });
        log.info(() -> "readVolumeData: " + updated);
        return true;
    }

    // usb
    public boolean readLedData(ByteBuffer buf) {
        buf.get(); //unknown
        ByteBuffer buf2 = buf.slice();
        List<PatchVisual> updated = new ArrayList<>();
        forEachIndexed(leds,(v,i) ->  {
            int bi = Math.floorDiv(i,4);
            int bm = Math.floorMod(i,4) * 2;
            int b = (buf2.get(bi) & (0x03 << bm)) >>> bm;
            //log.info(String.format("%s %s %s %s %s",i,bi,bm,b,Integer.toBinaryString(buf2.get(bi))));
            if (v.update(b)) {
                updated.add(v);
            }
        });
        log.info(() -> "readLedData: " + updated);
        return true;
    }

    // usb
    public boolean readParamUpdate(ByteBuffer buf) {
        FieldValues fvs = Protocol.ParamUpdate.FIELDS.read(new BitBuffer(buf.slice()));
        getArea(Protocol.ParamUpdate.Location.intValue(fvs)).updateParam(fvs);
        return true;
    }



    public void setAssignedVoices(int i) {
        log.info(() -> "setAssignedVoices: " + i);
        this.assignedVoices.set(i);
    }

    public LibProperty<Integer> assignedVoices() {
        return assignedVoices;
    }

    public Section getSection(Sections key) {
        return sections.get(key);
    }

    public PatchSettings getPatchSettings() {
        return patchSettings;
    }

    public LibProperty<String> name() {
        return name;
    }
}
