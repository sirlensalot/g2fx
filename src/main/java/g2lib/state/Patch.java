package g2lib.state;

import g2lib.BitBuffer;
import g2lib.CRC16;
import g2lib.Util;
import g2lib.model.*;
import g2lib.protocol.FieldValue;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Sections;
import g2lib.usb.UsbMessage;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import static g2lib.protocol.Protocol.*;

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
    public String text;
    public String name;
    public int slot = -1;
    public int version = -1;

    public static <T> T withSliceAhead(ByteBuffer buf, int length, Function<ByteBuffer,T> f) {
        return f.apply(Util.sliceAhead(buf,length));
    }

    public static Patch readFromMessage(ByteBuffer buf) {
        Patch patch = new Patch();
        patch.readMessageHeader(buf);

        for (Sections ss : MSG_SECTIONS) {
            patch.readSection(buf,ss);
            if (ss == Sections.SPatchDescription) {
                Util.expectWarn(buf,0x2d,"Message","USB extra 1");
                Util.expectWarn(buf,0x00,"Message","USB extra 2");
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
        FieldValues fv = getSectionValues(Sections.SPatchDescription);
        gp.voices = PatchDescription.Voices.intValueRequired(fv);
        gp.height = PatchDescription.Height.intValueRequired(fv);
        gp.monoPoly = PatchDescription.MonoPoly.intValueRequired(fv);
        gp.variation = PatchDescription.Variation.intValueRequired(fv);
        gp.category = PatchDescription.Category.intValueRequired(fv);
        gp.cvRed = PatchDescription.Red.intValueRequired(fv) == 1;
        gp.cvBlue = PatchDescription.Blue.intValueRequired(fv) == 1;
        gp.cvYellow = PatchDescription.Yellow.intValueRequired(fv) == 1;
        gp.cvOrange = PatchDescription.Orange.intValueRequired(fv) == 1;
        gp.cvGreen = PatchDescription.Green.intValueRequired(fv) == 1;
        gp.cvPurple = PatchDescription.Purple.intValueRequired(fv) == 1;
        gp.cvWhite = PatchDescription.White.intValueRequired(fv) == 1;

        fv = getSectionValues(Sections.SPatchParams);
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

        fv = getSectionValues(Sections.SModuleList1);
        addModules(fv,gp.voiceArea);
        fv = getSectionValues(Sections.SModuleList0);
        addModules(fv,gp.fxArea);

        fv = getSectionValues(Sections.SModuleParams1);
        setModuleParams(fv, gp.voiceArea, vc);
        fv = getSectionValues(Sections.SModuleParams0);
        setModuleParams(fv, gp.fxArea, vc);

        fv = getSectionValues(Sections.SModuleNames1);
        setModuleNames(fv, gp.voiceArea);
        fv = getSectionValues(Sections.SModuleNames0);
        setModuleNames(fv, gp.fxArea);

        fv = getSectionValues(Sections.SModuleLabels1);
        setModuleLabels(fv, gp.voiceArea);
        fv = getSectionValues(Sections.SModuleLabels0);
        setModuleLabels(fv, gp.fxArea);

        fv = getSectionValues(Sections.SMorphParameters);
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

        fv = getSectionValues(Sections.SKnobAssignments);
        List<FieldValues> kas = KnobAssignments.KnobsPatch.subfieldsValueRequired(fv);
        for (FieldValues ka : kas) {
            if (KnobAssignment.Assigned.intValueRequired(ka) == 1) {
                List<FieldValues> kps = KnobAssignment.ParamsPatch.subfieldsValueRequired(ka);
                for (FieldValues kp : kps) {
                    ParamModule m = gp.getArea(KnobParams.Location.intValueRequired(kp))
                            .getModuleRequired(KnobParams.Index.intValueRequired(kp));
                    m.assignKnob(KnobParams.Param.intValueRequired(kp),
                            KnobParams.IsLed.intValueRequired(kp) == 1);
                }
            }
        }

        fv = getSectionValues(Sections.SControlAssignments);
        List<FieldValues> cas = ControlAssignments.Assignments.subfieldsValueRequired(fv);
        for (FieldValues ca : cas) {
            ParamModule m = gp.getArea(ControlAssignment.Location.intValueRequired(ca))
                    .getModuleRequired(ControlAssignment.Index.intValueRequired(ca));
            m.assignMidiControl(ControlAssignment.Param.intValueRequired(ca),
                    ControlAssignment.MidiCC.intValueRequired(ca));
        }

        fv = getSectionValues(Sections.SMorphLabels);
        List<FieldValues> ls = MorphLabels.Labels.subfieldsValueRequired(fv);
        for (FieldValues l : ls) {
            gp.getSettingsModule(SettingsModules.MorphModes).setParamLabel(
                    MorphLabel.Entry.intValueRequired(l) - 8,
                    MorphLabel.Label.stringValueRequired(l));
        }

        fv = getSectionValues(Sections.SCableList1);
        setCables(fv, gp.voiceArea);
        fv = getSectionValues(Sections.SCableList0);
        setCables(fv, gp.fxArea);

        Section tps = getSection(Sections.STextPad);
        if (tps != null) {
            gp.setTextPad(TextPad.Text.stringValueRequired(tps.values()));
        }

        return gp;
    }

    private FieldValues getSectionValues(Sections ss) {
        Section s = getSection(ss);
        if (s == null) {
            throw new IllegalArgumentException("Section not found: " + ss);
        }
        return s.values();
    }

    private static void setCables(FieldValues fv, PatchArea<G2Module> area) {
        List<FieldValues> cs = CableList.Cables.subfieldsValueRequired(fv);
        for (FieldValues c : cs) {
            G2Module srcMod = area.getModuleRequired(Cable.SrcModule.intValueRequired(c));
            G2Module destMod = area.getModuleRequired(Cable.DestModule.intValueRequired(c));
            int direction = Cable.Direction.intValueRequired(c);
            int srci = Cable.SrcConn.intValueRequired(c);
            Connector src = direction == 1 ? srcMod.getOutPort(srci) : srcMod.getInPort(srci);
            Connector dest = destMod.getInPort(Cable.DestConn.intValueRequired(c));
            G2Cable cable = new G2Cable(srcMod, src, destMod, dest,
                    direction,
                    Cable.Color.intValueRequired(c));
            area.addCable(cable);
        }
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
            m.setName(ModuleName.Name.stringValueRequired(mn));
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
        Util.expectWarn(buf,0x01,"Message","Cmd");
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
        Util.expectWarn(fileBuffer,0x17,filePath,"header");
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


    public void readSection(ByteBuffer buf, Sections s) {
        BitBuffer bb = Util.sliceSection(s.type,buf);
        //log.info(s + ": length " + bb.limit());
        readSectionSlice(bb, s);
    }

    public void readSectionSlice(BitBuffer bb, Sections s) {
        if (s.location != null) {
            Integer loc = bb.get(2);
            if (!loc.equals(s.location)) {
                throw new IllegalArgumentException(String.format("Bad location: %x, %s",loc, s));
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
    
    public void readSectionMessage(UsbMessage msg, Sections s) {
        readSectionMessage(msg.buffer().position(msg.extended() ? 0 : 1),s);
    }

    public Section getSection(Sections key) {
        return sections.get(key);
    }


}
