package org.g2fx.g2lib.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.UsbSender;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import static org.g2fx.g2lib.device.Device.dispatchFailure;
import static org.g2fx.g2lib.device.Device.dispatchSuccess;
import static org.g2fx.g2lib.protocol.Codes.*;
import static org.g2fx.g2lib.state.Patch.fileHeader;
import static org.g2fx.g2lib.state.Patch.verifyFileHeader;
import static org.g2fx.g2lib.util.Util.withYamlMap;

public class Performance {

    private static final Logger log = Util.getLogger(Performance.class);

    public static final ByteBuffer HEADER = fileHeader(86, new String[]{
            "Version=Nord Modular G2 File Format 1",
            "Type=Performance",
            "Version=23",
            "Info=BUILD 320"
    });

    private int version;

    private final LibProperty<String> perfName = new LibProperty<>("Empty perf");

    private PerformanceSettings perfSettings;
    private final GlobalKnobAssignments globalKnobAssignments = new GlobalKnobAssignments();
    private final Map<Slot,Patch> slots = new TreeMap<>();

    private final UsbSender usb;

    public Performance(UsbSender usb) {
        this.usb = usb;
        for (Slot s : Slot.values()) {
            slots.put(s,new Patch(s, usb));
        }
    }

    public int getVersion() {
        return version;
    }

    public static Performance readFromFile(String filePath,UsbSender sender) throws Exception {
        ByteBuffer fileBuffer = verifyFileHeader(filePath, HEADER);

        Util.expectWarn(fileBuffer,0x17,filePath,"header terminator");
        Performance perf = new Performance(sender);
        perf.setVersion(fileBuffer.get());
        perf.readPerformanceSettings(fileBuffer);
        for (Slot s : Slot.values()) {
            Patch patch = new Patch(s, sender);
            patch.setVersion(0); //TODO source?
            patch.readFileSections(fileBuffer);
            patch.name().set(perf.getPerfSettings().getSlotSettings(s).patchName().get());
            perf.slots.put(s,patch);
        }
        perf.readGlobalKnobAssignmentsType(fileBuffer);

        String name = new File(filePath).getName();
        perf.perfName.set(name.substring(0,name.length()-".prf2".length()));

        return perf;
    }


    public Patch readPatchFromFile(Slot slot, String path) throws Exception {
        Patch patch = Patch.readFromFile(slot,path,usb);
        String name = new File(path).getName();
        patch.name().set(name.substring(0,name.length()-".pch2".length()));
        slots.put(slot, patch);
        patch.sendPatch();
        for (Patch p : slots.values()) { if (p!=patch) p.sendUnk6Request(); }
        return patch;
    }

    /**
     * Read ahead perf settings.
     */
    private boolean readPerformanceSettings(ByteBuffer buf) {
        perfSettings = new PerformanceSettings(
                readSectionSlice(buf,Sections.SPerformanceSettings_11));
        return true;
    }


    public boolean readGlobalKnobAssignments(ByteBuffer buf) {
        log.info(() -> "readGlobalKnobAssignments");
        BitBuffer bb = BitBuffer.sliceAheadLength(buf);
        return readGlobalKnobAssignments(bb);
    }

    public boolean readGlobalKnobAssignmentsType(ByteBuffer buf) {
        BitBuffer bb = Sections.sliceAheadSection(Sections.SGlobalKnobAssignments_5f,buf);
        return readGlobalKnobAssignments(bb);
    }

    private boolean readGlobalKnobAssignments(BitBuffer bb) {
        FieldValues fvs = Sections.SGlobalKnobAssignments_5f.fields.read(bb);
        globalKnobAssignments.update(fvs);
        return true;
    }

    public void initNew() throws Exception {
        sendVersionRequest(); //blocking if online, otherwise noop
        perfName.set("Empty perf");
        perfSettings = new PerformanceSettings();
        for (Patch patch : slots.values()) {
            patch.initNew();
        }
    }

    public void writeToFile(File file) throws Exception {
        ByteBuffer buf = writeFile();
        Util.writeBuffer(buf.rewind(),file);
    }

    public ByteBuffer writeFile() throws Exception {
        ByteBuffer buf = ByteBuffer.allocateDirect(0xffff);
        buf.put(HEADER.rewind());
        int start = buf.position();
        buf.put(Util.asBytes(0x17,version));
        BitBuffer bb = BitBuffer.fromSlice(buf);
        writeSection(bb,Sections.SPerformanceSettings_11,perfSettings.getFieldValues());
        for (Patch patch : slots.values()) {
            patch.writeMessage(bb, PatchModule.FILE_VARIATIONS);
        }
        writeSection(bb,Sections.SGlobalKnobAssignments_5f,globalKnobAssignments.getFieldValues());
        buf.position(bb.limit());
        Util.writeCrc(buf,start);
        return buf;
    }

    public void sendPerf() throws Exception {
        log.info("sendPerf");
        usb.sendBulk("sendPerf",true,writeMessage()); //I_VERSION in response
        for (Patch s : slots.values()) {
            s.sendSlotResourcesRequests();
            s.sendUnk6Request();
            s.sendSelectedParamRequest();
        }
        sendMasterClockRequest();
    }

    private ByteBuffer writeMessage() throws Exception {
        ByteBuffer buf = ByteBuffer.allocateDirect(0xffff);
        buf.put(Util.asBytes(
                M_CMD,
                S_PERF_REQ,
                V_NEW_PERF,
                O_CREATE,
                0x00, // ??
                0x00, // ??
                0x00  // ??
        ));
        BitBuffer bb = BitBuffer.fromSlice(buf);
        FieldValues name = Protocol.EntryName.FIELDS.values(Protocol.EntryName.Name.value(perfName.get()));
        name.write(bb);
        bb.put(8,0x1a);
        bb.put(8,Sections.SPerformanceName_29.type);
        name.write(bb);
        writeSection(bb, Sections.SPerformanceSettings_11, perfSettings.getFieldValues());
        for (Patch patch : slots.values()) {
            patch.writeMessage(bb, PatchModule.MAX_VARIATIONS);
        }
        writeSection(bb, Sections.SGlobalKnobAssignments_5f,globalKnobAssignments.getFieldValues());
        buf.limit(bb.limit());
        return buf;
    }

    private static void writeSection(BitBuffer bb, Sections s, FieldValues fvs) throws Exception {
        bb.put(8, s.type);
        int lpos = (short) bb.limit();
        bb.put(16,0); //length holder
        int ss = bb.limit();
        fvs.write(bb);
        bb.writeLength(lpos, bb.limit()-ss);
    }

    /**
     * Read (ahead) performance name + settings chunks.
     */
    public boolean readPerformanceNameAndSettings(ByteBuffer buf) {
        BitBuffer bb = new BitBuffer(buf.slice());
        int pos = buf.position();
        perfName.set(Protocol.EntryName.Name.stringValue(Protocol.EntryName.FIELDS.read(bb)));
        pos += bb.getBitIndex()/8;
        ByteBuffer buf2 = bb.slice();
        readPerformanceSettings(buf2);
        pos += buf2.position();
        buf.position(pos);
        return true;
    }

    /**
     * Parse (ahead) section TYPE -> LENGTH -> data
     */
    public static FieldValues readSectionSlice(ByteBuffer buf, Sections s) {
        return s.fields.read(Sections.sliceAheadSection(s,buf));
    }


    public boolean setMasterClock(ByteBuffer buf) {
        //3f ff 01 79
        buf.get(); // 0xff
        byte ty = buf.get();
        byte val = buf.get();
        switch (ty) {
            case 0: perfSettings.masterClockRun().set(val==1); break;
            case 1: perfSettings.masterClock().set(Util.b2i(val)); break;
            default: return dispatchFailure("setMasterClock: unrecognized type: %s", ty);
        }
        return dispatchSuccess(() -> "setMasterClock, type=" + ty + ", value=" + val);
    }


    // usb
    public boolean readExtMasterClock(ByteBuffer buf) {
        buf.get();
        int v = Util.getShort(buf);
        //TODO?????
        return dispatchSuccess(() -> "readExtMasterClock: " + v);
    }



    // 00 31 c8 00 00 00 00 00 00 00 00
    public boolean readSlotChange(ByteBuffer buf) {
        int slot = buf.get();
        perfSettings.selectedSlot().set(slot);
        return dispatchSuccess(() -> "readSlotChange: " + slot);
    }


    public GlobalKnobAssignments getGlobalKnobAssignments() {
        return globalKnobAssignments;
    }

    public LibProperty<String> perfName() {
        return perfName;
    }

    public String getName() {
        return perfName.get();
    }

    public PerformanceSettings getPerfSettings() {
        return perfSettings;
    }

    public Patch getSlot(Slot slot) {
        return slots.get(slot);
    }

    public Patch getSelectedPatch() {
        if (perfSettings == null) { throw new IllegalStateException("Perf settings not initialized"); }
        return getSlot(getSelectedSlot());
    }

    public Slot getSelectedSlot() {
        return Slot.fromIndex(perfSettings.selectedSlot().get());
    }

    // usb
    public boolean readAssignedVoices(ByteBuffer buf) {
        for (Slot s : Slot.values()) {
            getSlot(s).setAssignedVoices(Util.b2i(buf.get()));
        }
        return true;
    }

    // usb, file-perf
    public void setVersion(int version) {
        this.version = version;
        log.info(() -> "setVersion: " + version);
    }

    public void dumpYaml(String fileName) throws Exception {

        var top = withYamlMap(m -> {
            m.put("slots",slots.values().stream().map(s -> withYamlMap(sm -> {
                sm.put("modules", withYamlMap(am -> {
                     Arrays.stream(AreaId.USER_AREAS).sequential().forEach(a -> {
                         am.put(a.toString(),s.getArea(a).getModules().stream().map(pm -> {
                             return withYamlMap(mm -> {
                                 mm.put("index",pm.getIndex());
                                 mm.put("name",pm.name().get());
                                 ModuleType type = pm.getUserModuleData().getType();
                                 mm.put("type", type.name().substring(2));
                                 if (!type.modes.isEmpty()) {
                                     mm.put("modes", withYamlMap(modem ->
                                             Streams.forEachPair(type.modes.stream(), pm.getUserModuleData().getModes().stream(), (tm, lp) -> {
                                                 modem.put(tm.name(), lp.get());
                                             })));
                                 }
                                 if (!type.getParams().isEmpty()) {
                                     mm.put("params", withYamlMap(pmms -> {
                                         int i = 1;
                                         for (List<Integer> vs : pm.getAllVarValues()) {
                                             pmms.put(Long.toString(i++), withYamlMap(paramm ->
                                                     Streams.forEachPair(type.getParams().stream(), vs.stream(), (tp, v) ->
                                                             paramm.put(tp.name(), v))));
                                         }
                                     }));
                                 }
                             });
                         }).toList());
                     });
                }));
            })).toList());
        });
        ObjectMapper mapper = Util.mkYamlMapper();
        mapper.writeValue(
                new File(fileName),
                top);
    }

    public void initialize() throws Exception {

        sendVersionRequest();

        usb.sendSystemRequest("Synth settings", // technically "device scope" but comes back w/ perf version
                O_SYNTH_SETTINGS);

        usb.sendSystemRequest("unknown 1", // technically "device scope" but comes back w/ perf version
                O_UNKNOWN1);

        sendPerfSettingsRequest();

        sendPerfUnk2Request();

        sendMasterClockRequest();

        sendGlobalKnobsRequest();

        for (Patch p : slots.values()) {
            p.initialize();
        }

        sendAssignedVoicesRequest();

    }

    public void loadFromDevice() throws Exception {

        sendVersionRequest();

        sendPerfSettingsRequest();

        sendPerfUnk2Request();

        for (Patch p : slots.values()) {
            p.loadFromDevice();
        }

        sendGlobalKnobsRequest();

        usb.sendStartStopComm(true);

    }

    private void sendAssignedVoicesRequest() throws Exception {
        usb.sendSystemRequest("assigned voices",
                O_ASSIGNED_VOICES);
    }

    private void sendGlobalKnobsRequest() throws Exception {
        usb.sendPerfRequest(getVersion(),"global knobs",
                O_GLOBAL_KNOBS);
    }

    private void sendPerfUnk2Request() throws Exception {
        usb.sendPerfRequest(getVersion(),"unknown 2",
                O_UNKNOWN2);
    }

    private void sendPerfSettingsRequest() throws Exception {
        usb.sendPerfRequest(getVersion(),"perf settings",
                O_PERF_SETTINGS);
    }

    private void sendMasterClockRequest() throws Exception {
        //  TODO master clock can be O_EXT_MASTER_CLOCK = 0x5d or S_SET_MASTER_CLOCK = 0x3f
        usb.sendSystemRequest("master clock",
                O_MASTER_CLOCK);
    }

    private void sendVersionRequest() throws Exception {
        usb.sendSystemRequest("perf version",
                O_VERSION,
                S_PERF_04);
    }


    public Iterable<Patch> slots() {
        return slots.values();
    }

    public void loadPatchFromDevice(Slot slot, int version) throws Exception {
        Patch patch = new Patch(slot, usb);
        slots.put(slot, patch);
        patch.setVersion(version);
        patch.sendSlotVersionRequest();
        patch.loadFromDevice();
        sendGlobalKnobsRequest();
        usb.sendStartStopComm(true);
    }
}
