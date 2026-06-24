package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.UsbMessage;
import org.g2fx.g2lib.usb.UsbSender;
import org.g2fx.g2lib.usb.UsbSlotSender;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.CRC16;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.g2fx.g2lib.device.Device.dispatchSuccess;
import static org.g2fx.g2lib.protocol.Codes.*;

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

    public final LinkedHashMap<Sections, Sections.Section> sections = new LinkedHashMap<>();
    private final LibProperty<String> name = new LibProperty<>("No name");
    private final Slot slot;
    private int version;


    private final PatchSettings patchSettings;
    private FieldValues textPad;
    private FieldValues currentNote;
    private final PatchArea voiceArea;
    private final PatchArea fxArea;
    private final PatchArea settingsArea;
    private final UsbSlotSender slotSender;
    private KnobAssignments knobAssignments;
    private ControlAssignments controls;
    private MorphParameters morphParams;
    private LibProperty<Integer> assignedVoices = new LibProperty<>(0);
    private final PatchVisuals visuals;

    // file-perf
    public Patch(Slot slot, UsbSender sender) {
        this.slot = slot;
        log = Util.getLogger(getClass(),slot);
        this.slotSender = new UsbSlotSender(sender,this);
        voiceArea = new PatchArea(slot,AreaId.Voice,slotSender,this::updateVisuals);
        fxArea = new PatchArea(slot,AreaId.Fx,slotSender,this::updateVisuals);
        settingsArea = new PatchArea(slot,slotSender);
        visuals = new PatchVisuals(slot,voiceArea,fxArea);
        patchSettings = new PatchSettings(slot,slotSender);
    }

    public int getVersion() {
        return version;
    }

    public Slot getSlot() {
        return slot;
    }


    // usb, file-perf, test, file-patch
    public void setVersion(int version) {
        if (this.version == version) { return; }
        this.version = version;
        log.info(() -> "setVersion: " + version);
    }

    public static <T> T withSliceAhead(ByteBuffer buf, int length, Function<ByteBuffer,T> f) {
        return f.apply(Util.sliceAhead(buf,length));
    }

    public KnobAssignments getKnobAssignments() {
        return knobAssignments;
    }


    public void initNew() {
        patchSettings.initNew();
        //user modules are empty so no-op
        currentNote = Protocol.CurrentNote.FIELDS.values(
                Protocol.CurrentNote.Note.value(0x40),
                Protocol.CurrentNote.Attack.value(0),
                Protocol.CurrentNote.Release.value(0),
                Protocol.CurrentNote.NoteCount.value(0),//meaning 1
                Protocol.CurrentNote.Notes.value(List.of(
                        Protocol.NoteData.FIELDS.values(
                                Protocol.NoteData.Note.value(0x40),
                                Protocol.NoteData.Attack.value(0),
                                Protocol.NoteData.Release.value(0)
                        ))));
        //cables empty
        settingsArea.initSettingsParams();
        //user module params empty
        morphParams = new MorphParameters();
        knobAssignments = new KnobAssignments(slot);
        controls = new ControlAssignments();
        settingsArea.initMorphLabels();
        //module labels empty
        textPad = Protocol.TextPad.FIELDS.values(Protocol.TextPad.Text.value(""));

    }

    /**
     * The big patch message starts with 0x21 section and dispatches here, as
     * does patch settings messages which are only an 0x21 section.
     */
    // usb, test
    public void readPatchDescription(ByteBuffer buf) {
        for (Sections ss : Sections.MSG_SECTIONS) {
            readAheadSection(buf,ss);
            // 0x21 is first in either case
            if (ss == Sections.SPatchDescription_21) {
                // test for solo solo on patch settings change from synth
                if (buf.remaining() == 2) {
                    log.info(()->"readPatchDescription: just 0x21 message read");
                    return;
                }
                Util.expectWarn(log, buf,0x2d,"Message","USB extra 1");
                Util.expectWarn(log, buf,0x00,"Message","USB extra 2");
            }
        }
        updateVisuals();
    }

    private void updateVisuals() {
        visuals.updateVisualIndex();
    }

    // test
    public static Patch readFromMessage(int version, Slot slot, ByteBuffer buf, UsbSender sender) {
        Patch patch = new Patch(slot, sender);
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
        Util.expectWarn(log, buf,0x01,"Message","Cmd");
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
    public static Patch readFromFile(Slot slot, String filePath, UsbSender sender) throws Exception {
        ByteBuffer fileBuffer = verifyFileHeader(filePath, HEADER);

        ByteBuffer slice = fileBuffer.slice();
        int crc = CRC16.crc16(slice,0,slice.limit()-2);

        Patch patch = new Patch(slot, sender);
        Util.expectWarn(patch.log, fileBuffer,0x17,filePath,"header terminator");
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
        for (Sections ss : Sections.FILE_SECTIONS) {
            readAheadSection(fileBuffer,ss);
        }
        updateVisuals();
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
        Sections.Section ss = getSection(s);
        if (ss == null) {
            throw new IllegalArgumentException("No section in patch: " + s);
        }
        Sections.writeSection(buf, ss);

    }

    /*
     * TODO this currently writes an inbound-style patch message, change to outbound
     */
    public ByteBuffer writeMessageOld() throws Exception {

        ByteBuffer buf = ByteBuffer.allocateDirect(2048);

        writeMessageHeader(buf);
        for (Sections s : Sections.MSG_SECTIONS) {
            writeSection(buf,s);
            if (s == Sections.SPatchDescription_21) {
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



    /**
     * write patch message to existing bitbuffer. TODO same as sending patch?
     */
    public void writeMessage(BitBuffer bb, int variationCount) throws Exception {
        for (Sections s : Sections.FILE_SECTIONS) {
            bb.put(8,s.type);
            int lpos = bb.getBytePosition();
            bb.put(16,0);
            int ss = bb.getBytePosition();
            s.writeLocation(bb);
            FieldValues fvs = getSectionValues(s,variationCount);
            fvs.write(bb);
            int len = bb.getBytePosition() - ss;
            bb.writeLength(lpos, len);
            log.info(() -> String.format("writeMessage: %s, length %x",s,len));
            bb.padToByte();
        }
    }


    public void sendPatch() throws Exception {
        log.info("sendPerf");
        slotSender.getSender().sendBulk("sendPatch",true, writeMessage());
        sendPatchLoadResponse();
        sendSelectedParamRequest();
    }

    public ByteBuffer writeMessage() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(0xffff);
        buf.put(Util.asBytes(
                M_CMD,
                S_SLOT_REQ + slot.ordinal(),
                V_NEW_PATCH,
                O_CREATE,
                0x00, // ??
                0x00, // ??
                0x00  // ??
        ));
        BitBuffer bb = new BitBuffer(buf);
        FieldValues name = Protocol.EntryName.FIELDS.values(Protocol.EntryName.Name.value(name().get()));
        name.write(bb);
        writeMessage(bb,PatchModule.MAX_VARIATIONS);
        return bb.toBuffer();
    }

    private FieldValues getSectionValues(Sections s, int variationCount) {
        return switch (s) {
            case SPatchDescription_21 -> patchSettings.values();
            case SModuleList1_4a -> voiceArea.getModuleListValues();
            case SModuleList0_4a -> fxArea.getModuleListValues();
            case SCurrentNote_69 -> currentNote;
            case SCableList1_52 -> voiceArea.getCableListValues();
            case SCableList0_52 -> fxArea.getCableListValues();
            case SPatchParams_4d -> settingsArea.getParamsValues(variationCount);
            case SModuleParams1_4d -> voiceArea.getParamsValues(variationCount);
            case SModuleParams0_4d -> fxArea.getParamsValues(variationCount);
            case SMorphParameters_65 -> morphParams.getFieldValues(variationCount);
            case SKnobAssignments_62 -> knobAssignments.values();
            case SControlAssignments_60 -> controls.values();
            case SMorphLabels_5b -> settingsArea.getMorphLabelValues();
            case SModuleLabels1_5b -> voiceArea.getModuleLabelValues();
            case SModuleLabels0_5b -> fxArea.getModuleLabelValues();
            case SModuleNames1_5a -> voiceArea.getModuleNameValues();
            case SModuleNames0_5a -> fxArea.getModuleNameValues();
            case STextPad_6f -> textPad;
            default -> throw new IllegalArgumentException("Unknown patch section: " + s);
        };
    }


    public ByteBuffer writeFile() throws Exception {
        ByteBuffer buf = ByteBuffer.allocateDirect(0xffff);
        buf.put(HEADER.rewind());
        int start = buf.position();
        if (version == -1) {
            throw new RuntimeException("writeFile: version not initialized");
        }
        buf.put(Util.asBytes(0x17,version));
        writeFileSections(buf);
        Util.writeCrc(buf, start);
        return buf;
    }

    public void writeFileSections(ByteBuffer buf) throws Exception {
        for (Sections s : Sections.FILE_SECTIONS) {
            writeSection(buf,s);
        }
    }


    /**
     * Read ahead+update section from byte buffer TYPE -> LENGTH -> [LOCATION] -> ...
     */
    public void readAheadSection(ByteBuffer buf, Sections s) {
        BitBuffer bb = Sections.sliceAheadSection(s,buf);
        readSectionSlice(bb, s);
    }

    /**
     * read section from BitBuffer [LOCATION] -> ...
     */
    // usb, and via readSection
    public boolean readSectionSlice(BitBuffer bb, Sections s) {
        if (s.area != null) {
            AreaId a = AreaId.readLocation(bb);
            if (s.area != a) {
                throw new IllegalArgumentException(String.format("Bad location: %s, %s",a, s));
            }
        }
        return readSection(bb, s);
    }

    /**
     * read section from BitBuffer after [LOCATION], populating FieldValues and updating.
     */
    private boolean readSection(BitBuffer bb, Sections s) {
        FieldValues fvs;
        int startIx = bb.getBitPosition();
        try {
            fvs = s.fields.read(bb);
        } catch (RuntimeException e) {
            log.log(Level.SEVERE,String.format("Error reading section %s",s),e);
            throw e;
        }
        updateSection(s, new Sections.Section(s, fvs));
        return true;
    }

    private void updateSection(Sections s, Sections.Section section) {
        sections.put(s, section);
        log.info("updateSection: " + s);
        final FieldValues fvs = section.values();
        switch (s) {
            case SPatchDescription_21 -> patchSettings.update(fvs);
            case SPatchParams_4d -> settingsArea.setModuleParamValues(fvs);
            case STextPad_6f -> this.textPad = fvs;
            case SCurrentNote_69 -> this.currentNote = fvs;
            case SModuleList0_4a -> fxArea.addModules(fvs);
            case SModuleList1_4a -> voiceArea.addModules(fvs);
            case SModuleParams0_4d -> fxArea.setModuleParamValues(fvs);
            case SModuleParams1_4d -> voiceArea.setModuleParamValues(fvs);
            case SModuleLabels0_5b -> fxArea.setModuleLabels(fvs);
            case SModuleLabels1_5b -> voiceArea.setModuleLabels(fvs);
            case SModuleNames0_5a -> fxArea.setModuleNames(fvs);
            case SModuleNames1_5a -> voiceArea.setModuleNames(fvs);
            case SCableList0_52 -> fxArea.addCables(fvs);
            case SCableList1_52 -> voiceArea.addCables(fvs);
            case SMorphLabels_5b -> settingsArea.setMorphLabels(fvs);
            case SKnobAssignments_62 -> this.knobAssignments = new KnobAssignments(fvs,slot);
            case SControlAssignments_60 -> this.controls = new ControlAssignments(fvs);
            case SMorphParameters_65 -> this.morphParams = new MorphParameters(fvs);
            case SPatchName_27 -> this.name.set(Protocol.EntryName.Name.stringValue(fvs));
        }
    }

    // test
    public void readPatchLoadDataMsg(UsbMessage msg) {
        ByteBuffer buf = msg.getBufferx();
        readMessageHeader(buf);
        Util.expectWarn(log, buf,Sections.SPatchLoadData_72.type,"msg","PatchLoad");
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
        readAheadSection(buf,s);
    }


    // usb
    public boolean readParamUpdate(ByteBuffer buf) {
        FieldValues fvs = Protocol.ParamUpdate.FIELDS.read(new BitBuffer(buf.slice()));
        getArea(Protocol.ParamUpdate.Location.intValue(fvs)).updateParam(fvs);
        return dispatchSuccess(() -> "readParamUpdate");
    }


    // 01 b5 61 00 00 00 00 00 00 00 00
    public boolean readVarChange(ByteBuffer buf) {
        int var = buf.get();
        patchSettings.variation().set(var);
        return dispatchSuccess(()->"readVarChange: " + var);
    }




    public void setAssignedVoices(int i) {
        log.info(() -> "setAssignedVoices: " + i);
        this.assignedVoices.set(i);
    }

    public LibProperty<Integer> assignedVoices() {
        return assignedVoices;
    }

    public Sections.Section getSection(Sections key) {
        return sections.get(key);
    }

    public PatchSettings getPatchSettings() {
        return patchSettings;
    }

    public LibProperty<String> name() {
        return name;
    }

    public MorphParameters getMorphParams() {
        return morphParams;
    }

    public PatchVisuals getVisuals() {
        return visuals;
    }

    public boolean readParams(ByteBuffer buf) {
        BitBuffer bb = BitBuffer.sliceAhead(buf,Util.getShort(buf));
        Sections s = switch (AreaId.readLocation(bb)) {
            case Voice -> Sections.SModuleParams1_4d;
            case Fx -> Sections.SModuleParams0_4d;
            case Settings -> Sections.SPatchParams_4d;
        };
        return readSection(bb,s);
    }

    public boolean readParamLabels(ByteBuffer buf) {
        BitBuffer bb = BitBuffer.sliceAhead(buf,Util.getShort(buf));
        AreaId a = AreaId.readLocation(bb);
        Sections s = switch (a) {
            case Voice -> Sections.SModuleLabels1_5b;
            case Fx -> Sections.SModuleLabels0_5b;
            case Settings -> Sections.SMorphLabels_5b;
        };
        return readSection(bb,s);
    }

    public FieldValues getCurrentNote() {
        return currentNote;
    }


    public void loadFromDevice() throws Exception {
        sendSlotPatchRequest();
        sendSlotNameRequest();
        sendSlotCurrentNoteRequest();
        sendSlotTextRequest();
        sendSlotResourcesRequests();
        sendPatchLoadResponse();
        sendSelectedParamRequest();
    }

    public void initialize() throws Exception {
        sendSlotVersionRequest();
        sendSlotPatchRequest();
        sendSlotNameRequest();
        sendSlotCurrentNoteRequest();
        sendSlotTextRequest();
        sendSlotResourcesRequests();
        sendPatchLoadResponse();
        sendSelectedParamRequest();
    }

    public void sendSlotVersionRequest() throws Exception {
        slotSender.getSender().sendSystemRequest("slot version " + slot,
                O_VERSION, slot.ordinal());
    }

    private void sendSlotTextRequest() throws Exception {
        slotSender.sendSlotRequest("slot text " + slot, O_PATCH_TEXT);
    }

    private void sendSlotCurrentNoteRequest() throws Exception {
        slotSender.sendSlotRequest("slot note" + slot, O_CURRENT_NOTE);
    }

    private void sendSlotNameRequest() throws Exception {
        slotSender.sendSlotRequest("slot name" + slot, O_PATCH_NAME);
    }

    private void sendSlotPatchRequest() throws Exception {
        slotSender.sendSlotRequest("slot patch" + slot, O_PATCH);
    }

    public void sendSelectedParamRequest() throws Exception {
        slotSender.sendSlotRequest("selected param", O_SELECTED_PARAM);
    }

    public void sendPatchLoadResponse() throws Exception {
        slotSender.sendSlotRequest("patch load response", O_PATCH_LOAD_RESPONSE);
    }

    public void sendSlotResourcesRequests() throws Exception {
        sendSlotResourcesRequest(slotSender, AreaId.Voice);
        sendSlotResourcesRequest(slotSender, AreaId.Fx);
    }

    public static void sendSlotResourcesRequest(UsbSlotSender sender, AreaId area) throws Exception {
        sender.sendSlotRequest("patch load " + area, O_RESOURCES_USED, area.ordinal());
    }

}
