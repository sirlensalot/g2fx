package g2lib.state;

import g2lib.BitBuffer;
import g2lib.CRC16;
import g2lib.Util;
import g2lib.model.*;
import g2lib.protocol.FieldValue;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Fields;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import static g2lib.Protocol.*;

public class Patch {

    private static final Logger log = Util.getLogger(Patch.class);

    public static ByteBuffer patchHeader() {
        ByteBuffer header = ByteBuffer.allocate(80);
        for (String s : new String[]{
                "Version=Nord Modular G2 File Format 1",
                "Type=Patch",
                "Version=23",
                "Info=BUILD 320"
        }) {
            for (char c : s.toCharArray()) {
                header.put((byte) c);
            }
            header.put((byte)0x0d).put((byte)0x0a);
        }
        header.put((byte)0);
        header.rewind();
        return header.asReadOnlyBuffer();
    }

    public static final ByteBuffer HEADER = patchHeader();

    public record Section(Sections sections, FieldValues values) {

    }

    public enum Sections {

        SPatchDescription(PatchDescription.FIELDS,0x21  ),
        SModuleList1(ModuleList.FIELDS,0x4a,1),
        SModuleList0(ModuleList.FIELDS,0x4a,0),
        SCurrentNote(CurrentNote.FIELDS,0x69  ),
        SCableList1(CableList.FIELDS,0x52,1),
        SCableList0(CableList.FIELDS,0x52,0),
        SPatchParams(PatchParams.FIELDS,0x4d,2),
        SModuleParams1(ModuleParams.FIELDS,0x4d,1),
        SModuleParams0(ModuleParams.FIELDS,0x4d,0),
        SMorphParameters(MorphParameters.FIELDS,0x65  ),
        SKnobAssignments(KnobAssignments.FIELDS,0x62  ),
        SControlAssignments(ControlAssignments.FIELDS,0x60  ),
        SMorphLabels(MorphLabels.FIELDS,0x5b,2),
        SModuleLabels1(ModuleLabels.FIELDS,0x5b,1),
        SModuleLabels0(ModuleLabels.FIELDS,0x5b,0),
        SModuleNames1(ModuleNames.FIELDS,0x5a,1),
        SModuleNames0(ModuleNames.FIELDS,0x5a,0),
        STextPad(TextPad.FIELDS,0x6f  );

        private final Fields fields;
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

        @Override
        public String toString() {
            return String.format("%s[%x%s]",
                    name(),
                    type,
                    location != null ? (":" + location) : "");
        }
    }

    public static final Sections[] FILE_SECTIONS = Sections.values();

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
    public String text;
    public String name;
    public int slot = -1;
    public int version = -1;

    public static <T> T withSliceAhead(ByteBuffer buf, int length, Function<ByteBuffer,T> f) {
        return f.apply(Util.sliceAhead(buf,length));
    }

    public static void expectWarn(ByteBuffer buf,int expected,String filePath, String msg) {
        byte b = buf.get();
        if (b != expected) {
            log.warning(String.format("%s: expected %x, found %x at %s:%d",msg,expected,b,filePath,buf.position()-1));
        }
    }

    public static Patch readFromMessage(ByteBuffer buf) {
        Patch patch = new Patch();
        patch.readMessageHeader(buf);

        for (Sections ss : MSG_SECTIONS) {
            patch.readSection(buf,ss);
            if (ss == Sections.SPatchDescription) {
                expectWarn(buf,0x2d,"Message","USB extra 1");
                expectWarn(buf,0x00,"Message","USB extra 2");
            }
        }
        return patch;
    }

    private FieldValues getVarValues(int variation,List<FieldValues> fvs) {
        if (variation >= fvs.size()) {
            throw new IllegalArgumentException("Invalid/missing variation: " + variation);
        }
        return fvs.get(variation);
    }


    private FieldValues getMorphValues(int morph,List<FieldValues> fvs) {
        if (morph >= fvs.size()) {
            throw new IllegalArgumentException("Invalid/missing morph: " + morph);
        }
        return fvs.get(morph);
    }

    public G2Patch toPatch() {
        G2Patch gp = new G2Patch("Untitled");
        FieldValues fv = getSection(Sections.SPatchDescription).values();
        gp.voices = PatchDescription.Voices.intValueRequired(fv);
        gp.height = PatchDescription.Height.intValueRequired(fv);
        gp.monoPoly = PatchDescription.MonoPoly.intValueRequired(fv);
        gp.variation = PatchDescription.Variation.intValueRequired(fv);
        gp.category = PatchDescription.Category.intValueRequired(fv);
        //TODO colors

        fv = getSection(Sections.SPatchParams).values();
        int vc = PatchParams.VariationCount.intValueRequired(fv);
        List<FieldValues> morphs = PatchParams.Morphs.subfieldsValueRequired(fv);
        List<FieldValues> volMuteds = PatchParams.SectionVolMuteds.subfieldsValueRequired(fv);
        List<FieldValues> glides = PatchParams.SectionGlides.subfieldsValueRequired(fv);
        List<FieldValues> bends = PatchParams.SectionBends.subfieldsValueRequired(fv);
        List<FieldValues> vibratos = PatchParams.SectionVibratos.subfieldsValueRequired(fv);
        List<FieldValues> arps = PatchParams.SectionArps.subfieldsValueRequired(fv);
        List<FieldValues> octSustains = PatchParams.SectionOctSustains.subfieldsValueRequired(fv);

        for (int v = 0; v < vc; v++) {

            FieldValues morph = getVarValues(v, morphs);
            List<FieldValues> dials = MorphSettings.Dials.subfieldsValueRequired(morph);
            gp.getSettingsModule(SettingsModules.MorphDials).setParams
                    (v,dials.stream().map(Data7.Datum::intValueRequired).toList());
            List<FieldValues> modes = MorphSettings.Modes.subfieldsValueRequired(morph);
            gp.getSettingsModule(SettingsModules.MorphModes).setParams
                    (v,modes.stream().map(Data7.Datum::intValueRequired).toList());

            FieldValues ss = getVarValues(v, volMuteds);
            gp.getSettingsModule(SettingsModules.Gain).setParams(v,List.of(
                    VolMutedSettings.PatchVol.intValueRequired(ss),
                    VolMutedSettings.ActiveMuted.intValueRequired(ss)));

            ss = getVarValues(v,glides);
            gp.getSettingsModule(SettingsModules.Glide).setParams(v,List.of(
                    GlideSettings.Glide.intValueRequired(ss),
                    GlideSettings.GlideTime.intValueRequired(ss)));

            ss = getVarValues(v,bends);
            gp.getSettingsModule(SettingsModules.Bend).setParams(v,List.of(
                    BendSettings.Bend.intValueRequired(ss),
                    BendSettings.Semi.intValueRequired(ss)));

            ss = getVarValues(v,vibratos);
            gp.getSettingsModule(SettingsModules.Vibrato).setParams(v,List.of(
                    VibratoSettings.Vibrato.intValueRequired(ss),
                    VibratoSettings.Cents.intValueRequired(ss),
                    VibratoSettings.Rate.intValueRequired(ss)));

            ss = getVarValues(v,arps);
            gp.getSettingsModule(SettingsModules.Arpeggiator).setParams(v,List.of(
                    ArpSettings.Arpeggiator.intValueRequired(ss),
                    ArpSettings.Time.intValueRequired(ss),
                    ArpSettings.Octaves.intValueRequired(ss),
                    ArpSettings.Type.intValueRequired(ss)));

            ss = getVarValues(v,octSustains);
            gp.getSettingsModule(SettingsModules.Misc).setParams(v,List.of(
                    OctSustainSettings.OctShift.intValueRequired(ss),
                    OctSustainSettings.Sustain.intValueRequired(ss)));

        }

        fv = getSection(Sections.SModuleList1).values();
        addModules(fv,gp.voiceArea);
        fv = getSection(Sections.SModuleList0).values();
        addModules(fv,gp.fxArea);

        fv = getSection(Sections.SModuleParams1).values();
        setModuleParams(fv, gp.voiceArea, vc);
        fv = getSection(Sections.SModuleParams0).values();
        setModuleParams(fv, gp.fxArea, vc);

        fv = getSection(Sections.SModuleNames1).values();
        setModuleNames(fv, gp.voiceArea);
        fv = getSection(Sections.SModuleNames0).values();
        setModuleNames(fv, gp.fxArea);

        fv = getSection(Sections.SModuleLabels1).values();
        setModuleLabels(fv, gp.voiceArea);
        fv = getSection(Sections.SModuleLabels0).values();
        setModuleLabels(fv, gp.fxArea);

        fv = getSection(Sections.SMorphParameters).values();
        List<FieldValues> vms = MorphParameters.VarMorphs.subfieldsValueRequired(fv);
        for (FieldValues vm : vms) {
            int v = VarMorph.Variation.intValueRequired(vm);
            List<FieldValues> vmps = VarMorph.VarMorphParams.subfieldsValueRequired(vm);
            for (FieldValues vmp : vmps) {
                PatchArea<G2Module> area = gp.getUserArea(VarMorphParam.Location.intValueRequired(vmp));
                G2Module m = area.getModuleRequired(VarMorphParam.ModuleIndex.intValueRequired(vmp));
                m.setMorph(v,VarMorphParam.ParamIndex.intValueRequired(vmp),
                        VarMorphParam.Morph.intValueRequired(vmp),
                        VarMorphParam.Range.intValueRequired(vmp));
            }
        }

        fv = getSection(Sections.SKnobAssignments).values();
        List<FieldValues> kas = KnobAssignments.Knobs.subfieldsValueRequired(fv);
        for (FieldValues ka : kas) {
            if (KnobAssignment.Assigned.intValueRequired(ka) == 1) {
                List<FieldValues> kps = KnobAssignment.Params.subfieldsValueRequired(ka);
                for (FieldValues kp : kps) {
                    ParamModule m = gp.getArea(KnobParams.Location.intValueRequired(kp))
                            .getModuleRequired(KnobParams.Index.intValueRequired(kp));
                    m.assignKnob(KnobParams.Param.intValueRequired(kp),
                            KnobParams.IsLed.intValueRequired(kp) == 1);
                }
            }
        }

        fv = getSection(Sections.SControlAssignments).values();
        List<FieldValues> cas = ControlAssignments.Assignments.subfieldsValueRequired(fv);
        for (FieldValues ca : cas) {
            ParamModule m = gp.getArea(ControlAssignment.Location.intValueRequired(ca))
                    .getModuleRequired(ControlAssignment.Index.intValueRequired(ca));
            m.assignMidiControl(ControlAssignment.Param.intValueRequired(ca),
                    ControlAssignment.MidiCC.intValueRequired(ca));
        }

        fv = getSection(Sections.SMorphLabels).values();
        List<FieldValues> ls = MorphLabels.Labels.subfieldsValueRequired(fv);
        for (FieldValues l : ls) {
            gp.getSettingsModule(SettingsModules.MorphModes).setParamLabel(
                    MorphLabel.Entry.intValueRequired(l) - 8,
                    MorphLabel.Label.stringValueRequired(l));
        }

        return gp;
    }

    private static void setModuleLabels(FieldValues fv, PatchArea<G2Module> area) {
        List<FieldValues> mls = ModuleLabels.ModLabels.subfieldsValueRequired(fv);
        for (FieldValues ml : mls) {
            G2Module m = area.getModuleRequired(ModuleLabel.ModuleIndex.intValueRequired(ml));
            List<FieldValues> ls = ModuleLabel.Labels.subfieldsValueRequired(ml);
            for (FieldValues l : ls) {
                m.setParamLabel(ParamLabel.ParamIndex.intValueRequired(l),
                        ParamLabel.Label.stringValueRequired(l));
            }
        }
    }

    private static void setModuleNames(FieldValues fv, PatchArea<G2Module> area) {
        List<FieldValues> mns = ModuleNames.Names.subfieldsValueRequired(fv);
        for (FieldValues mn : mns) {
            G2Module m = area.getModuleRequired(ModuleName.ModuleIndex.intValueRequired(mn));
            m.name = ModuleName.Name.stringValueRequired(mn);
        }
    }

    private void setModuleParams(FieldValues fv, PatchArea<G2Module> area, int vc) {
        List<FieldValues> pss = ModuleParams.ParamSet.subfieldsValueRequired(fv);
        for (FieldValues ps : pss) {
            int mi = ModuleParamSet.ModIndex.intValueRequired(ps);
            List<FieldValues> vps = ModuleParamSet.ModParams.subfieldsValueRequired(ps);
            G2Module m = area.getModuleRequired(mi);
            for (int v = 0; v < vc; v++) {
                FieldValues vp = getVarValues(v, vps);
                List<FieldValues> mps = VarParams.Params.subfieldsValueRequired(vp);
                m.setParams(v,
                        mps.stream().map(Data7.Datum::intValueRequired).toList());
            }
        }
    }

    private static void addModules(FieldValues fv, PatchArea<G2Module> area) {
        List<FieldValues> mods = ModuleList.Modules.subfieldsValueRequired(fv);
        for (FieldValues mod : mods) {
            int ix = Module_.Index.intValueRequired(mod);
            ModuleType type = ModuleType.getById(Module_.Id.intValueRequired(mod));
            G2Module gm = new G2Module(type,ix);
            gm.horiz = Module_.Horiz.intValueRequired(mod);
            gm.vert = Module_.Vert.intValueRequired(mod);
            gm.color = Module_.Color.intValueRequired(mod);
            gm.uprate = Module_.Uprate.intValueRequired(mod);
            gm.leds = Module_.Leds.intValueRequired(mod) == 1;
            List<FieldValues> modes = Module_.Modes.subfieldsValueRequired(mod);
            for (int i = 0; i < modes.size(); i++) {
                gm.setMode(i,ModuleModes.Data.intValueRequired(modes.get(i)));
            }
            area.addModule(gm);
        }
    }

    public void readMessageHeader(ByteBuffer buf) {
        expectWarn(buf,0x01,"Message","Cmd");
        int slot = buf.get();
        if (this.slot == -1) {
            this.slot = slot;
        } else if (this.slot != slot) {
            throw new IllegalArgumentException(String.format("Slot mismatch: %d, %d",this.slot,slot));
        }
        int version = buf.get();
        if (this.version == -1) {
            this.version = version;
        } else if (this.version != version) {
            throw new IllegalArgumentException(String.format("Slot mismatch: %d, %d",this.slot,version));
        }
    }

    public void writeMessageHeader(ByteBuffer buf) {
        if (slot == -1 || version == -1) {
            throw new RuntimeException("writeMessageHeader: slot/version not initialized");
        }
        buf.put(Util.asBytes(0x01,slot,version));
    }

    public static Patch readFromFile(String filePath) throws Exception {
        ByteBuffer fileBuffer = Util.readFile(filePath);
        withSliceAhead(fileBuffer,HEADER.limit(),buf -> {
            if (!HEADER.rewind().equals(buf.rewind())) {
                throw new RuntimeException("Unexpected file header: " + Util.dumpBufferString(buf));
            }
            return true;
        });

        ByteBuffer slice = fileBuffer.slice();
        int crc = CRC16.crc16(slice,0,slice.limit()-2);

        Patch patch = new Patch();
        expectWarn(fileBuffer,0x17,filePath,"header");
        patch.version = fileBuffer.get();

        for (Sections ss : FILE_SECTIONS) {
            patch.readSection(fileBuffer,ss);
        }

        int fcrc = Util.getShort(fileBuffer);
        if (fcrc != crc) {
            throw new RuntimeException(String.format("CRC mismatch: %x %x",crc,fcrc));
        }

        return patch;
    }


    public static BitBuffer sliceSection(int type, ByteBuffer buf) {
        int t = buf.get();
        if (t != type) {
            throw new IllegalArgumentException(String.format("Section incorrect %x %x",type,t));
        }
        return BitBuffer.sliceAhead(buf,Util.getShort(buf));
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
        for (Patch.Sections s : MSG_SECTIONS) {
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
        for (Patch.Sections s : FILE_SECTIONS) {
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

    public void readSection(ByteBuffer buf, Sections s) {
        BitBuffer bb = sliceSection(s.type,buf);
        //log.info(s + ": length " + bb.limit());
        if (s.location != null) {
            Integer loc = bb.get(2);
            if (!loc.equals(s.location)) {
                throw new IllegalArgumentException(String.format("Bad location: %x, %s",loc,s));
            }
        }
        FieldValues fvs = s.fields.read(bb);
//        log.info(String.format("Read: %s, len=%x, crc=%x: %s\n",s,bb.limit(),CRC16.crc16(bb.toBuffer()),
//                Util.dumpBufferString(bb.toBuffer())));

        sections.put(s,new Section(s,fvs));
    }

    public void readSectionMessage(ByteBuffer buf, Sections s) {
        readMessageHeader(buf);
        readSection(buf,s);
    }

    public Section getSection(Sections key) {
        return sections.get(key);
    }


}
