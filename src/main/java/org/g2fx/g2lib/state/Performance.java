package org.g2fx.g2lib.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.UsbPerfSender;
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

    private LibProperty<String> perfName;

    private String fileName;
    private PerformanceSettings perfSettings;
    private GlobalKnobAssignments globalKnobAssignments;
    private final Map<Slot,Patch> slots = new TreeMap<>();

    private final UsbPerfSender sender;

    public Performance(UsbSender sysSender) {
        this.sender = new UsbPerfSender(sysSender,this);
        for (Slot s : Slot.values()) {
            slots.put(s,new Patch(s, sysSender));
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
        perf.perfSettings = new PerformanceSettings(
                readSectionSlice(fileBuffer,Sections.SPerformanceSettings_11));
        for (Slot s : Slot.values()) {
            Patch patch = new Patch(s, sender);
            patch.setVersion(0); //TODO source?
            patch.readFileSections(fileBuffer);
            perf.slots.put(s,patch);
        }
        perf.globalKnobAssignments = new GlobalKnobAssignments(
                readSectionSlice(fileBuffer,Sections.SGlobalKnobAssignments_5f));

        return perf;
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
        Sections.writeSection(buf,Sections.SPerformanceSettings_11,perfSettings.getFieldValues());
        for (Patch patch : slots.values()) {
            patch.writeFileSections(buf);
        }
        Sections.writeSection(buf,Sections.SGlobalKnobAssignments_5f,globalKnobAssignments.getFieldValues());
        Util.writeCrc(buf,start);
        return buf;
    }

    // test
    public Performance readFromMessage(ByteBuffer buf) {
        readPerfMsgHeader(buf.rewind());
        Util.expectWarn(buf,Sections.SPerformanceName_29.type,"Message","Perf name");
        readPerformanceNameAndSettings(buf);
        return this;
    }

    // usb, test
    public boolean readPerformanceNameAndSettings(ByteBuffer buf) {
        BitBuffer bb = new BitBuffer(buf.slice());
        perfName = LibProperty.stringFieldProperty(Protocol.EntryName.FIELDS.read(bb),Protocol.EntryName.Name);
        perfSettings = new PerformanceSettings(
                readSectionSlice(bb.slice(),Sections.SPerformanceSettings_11));
        log.info(() -> "readPerformanceNameAndSettings");
        return true;
    }

    // file-perf, test
    private static FieldValues readSectionSlice(ByteBuffer buf, Sections s) {
        return s.fields.read(Sections.sliceSection(s,buf));
    }

    // test
    private void readPerfMsgHeader(ByteBuffer buf) {
        Util.expectWarn(buf,0x01,"Message","Cmd 0x01");
        Util.expectWarn(buf,0x0c,"Message","Cmd 0x0c");
        Util.expectWarn(buf,version,"Message","Perf version");
    }

    // test
    public void readSectionMessage(ByteBuffer buf, Sections s) {
        readPerfMsgHeader(buf.rewind());
        FieldValues fvs = readSectionSlice(buf, s);
        updateSection(s, fvs);
    }

    // usb, test
    private void updateSection(Sections s, FieldValues fvs) {
        switch (s) {
            case SGlobalKnobAssignments_5f -> this.globalKnobAssignments = new GlobalKnobAssignments(fvs);
        }
    }

    // usb
    public boolean readSectionSlice(Sections s, BitBuffer bb) {
        updateSection(s,s.fields.read(bb));
        log.info(() -> "readSectionSlice: " + s);
        return true;
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

    public void setFileName(String fileName) {
        perfName = new LibProperty<>(fileName);
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
}
