package g2lib;

import g2lib.protocol.*;
import g2lib.state.Patch;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static g2lib.protocol.Protocol.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolTest {

    public static final String PATCH_FILE = "data/simplesynth001-20240802.pch2";
    public static final String PATCHMSG_1 = "data/msg_Slot1Patch_ed77.msg";
    public static final String PATCHMSG_0 = "data/msg_Slot0Patch_3dc3.msg";
    public static final String CURRENT_NOTE_MSG = "data/msg_Slot1Note_cc8f.msg";
    public static final String TEXTPAD_MSG = "data/msg_Slot1TextPad_5f41.msg";

    public static int assertFieldEquals(FieldValues values, int expected, FieldEnum field) {
        int actual = assertValue(values, field);
        assertEquals(String.format("%#02x",expected),
                String.format("%#02x",actual)
                ,field.toString());
        return actual;
    }

    public static void assertFieldEquals(FieldValues values, String expected, FieldEnum field) {
        String actual = assertString(values, field);
        assertEquals(expected,actual,field.toString());
    }


    private static int assertValue(FieldValues values, FieldEnum field) {
        Optional<Integer> i = field.intValue(values);
        assertTrue(i.isPresent(),"value found: " + field);
        return i.get();
    }

    private static String assertString(FieldValues values, FieldEnum field) {
        Optional<String> s = field.stringValue(values);
        assertTrue(s.isPresent(),"value found: " + field);
        return s.get();
    }

    public static List<FieldValues> assertSubfields(FieldValues fv, int size, FieldEnum field) {
        Optional<List<FieldValues>> o = field.subfieldsValue(fv);
        assertTrue(o.isPresent(),"subfields not found: " + field);
        List<FieldValues> fvs = o.get();
        assertEquals(size,fvs.size(),"size: " + field);
        return fvs;
    }



    private static List<FieldValues> assertVarParams(List<FieldValues> mps, int vc, int modIndex, int paramCount) {
        FieldValues mps1 = mps.removeFirst();
        assertFieldEquals(mps1, modIndex,ModuleParamSet.ModIndex);
        assertFieldEquals(mps1, paramCount, ModuleParamSet.ParamCount);
        return new ArrayList<>(assertSubfields(mps1, vc, ModuleParamSet.ModParams));
    }

    private static void assertModParams(int variation, List<FieldValues> vps, Integer... expecteds) {
        FieldValues vp = vps.get(variation);
        //System.out.println(vp);
        assertFieldEquals(vp, variation,VarParams.Variation);
        String message = "mod params var " + variation;
        List<Integer> el = Arrays.asList(expecteds);
        List<FieldValues> ps = assertSubfields(vp, el.size(), VarParams.Params);
        List<Integer> al = ps.stream().map(fv -> assertValue(fv, Data7.Datum)).toList();
        assertEquals(el,al, message);
    }

    private void sectionHeader(FieldValues vs, PatchParams f, int section, int entries) {
        assertTrue(f.subfieldsValue(vs).isPresent(),"header present: " + f);
        FieldValues h = f.subfieldsValue(vs).get().getFirst();
        assertFieldEquals(h,section,SectionHeader.Section);
        assertFieldEquals(h,entries,SectionHeader.Entries);
    }



    private void testMorphLabels(Patch p) {
        FieldValues mls = p.getSection(Patch.Sections.SMorphLabels).values();
        assertFieldEquals(mls,0x01,MorphLabels.LabelCount);
        assertFieldEquals(mls,0x01,MorphLabels.Entry);
        assertFieldEquals(mls,80,MorphLabels.Length);
        List<FieldValues> ls = assertSubfields(mls, 8, MorphLabels.Labels);
        String[] labels = {"Wheel","Vel","Keyb1","Aft.Tch","Sust.Pd","Ctrl.Pd","P.Stick", "G.Wh 2"};
        for (int i = 0; i < 8; i++) {
            FieldValues l = ls.get(i);
            assertFieldEquals(l,1,MorphLabel.Index);
            assertFieldEquals(l,8,MorphLabel.Length);
            assertFieldEquals(l,8+i,MorphLabel.Entry);
            assertFieldEquals(l,labels[i],MorphLabel.Label);
        }
    }



    private void testModuleNames(Patch p) {
        FieldValues mns = p.getSection(Patch.Sections.SModuleNames1).values();
        assertFieldEquals(mns,0x00,ModuleNames.Reserved);
        assertFieldEquals(mns,0x04,ModuleNames.NameCount);
        List<FieldValues> ns =
                new ArrayList<>(assertSubfields(mns, 4, ModuleNames.Names));

        FieldValues n = ns.removeFirst();
        assertFieldEquals(n,0x01,ModuleName.ModuleIndex);
        assertFieldEquals(n,"FltClassic1",ModuleName.Name);

        n = ns.removeFirst();
        assertFieldEquals(n,0x02,ModuleName.ModuleIndex);
        assertFieldEquals(n,"OscC1",ModuleName.Name);

        n = ns.removeFirst();
        assertFieldEquals(n,0x03,ModuleName.ModuleIndex);
        assertFieldEquals(n,"ModADSR1",ModuleName.Name);

        n = ns.removeFirst();
        assertFieldEquals(n,0x04,ModuleName.ModuleIndex);
        assertFieldEquals(n,"2-Out1",ModuleName.Name);

        mns = p.getSection(Patch.Sections.SModuleNames0).values();

        assertFieldEquals(mns,0x00,ModuleNames.Reserved);
        assertFieldEquals(mns,0x03,ModuleNames.NameCount);
        ns = new ArrayList<>(assertSubfields(mns, 3, ModuleNames.Names));

        n = ns.removeFirst();
        assertFieldEquals(n,0x01,ModuleName.ModuleIndex);
        assertFieldEquals(n,"Fx-In1",ModuleName.Name);

        n = ns.removeFirst();
        assertFieldEquals(n,0x02,ModuleName.ModuleIndex);
        assertFieldEquals(n,"Mix2-1A1",ModuleName.Name);

        n = ns.removeFirst();
        assertFieldEquals(n,0x03,ModuleName.ModuleIndex);
        assertFieldEquals(n,"2-Out1",ModuleName.Name);

    }

    private void testKnobAssignments(Patch p) {
        FieldValues knobs = p.getSection(Patch.Sections.SKnobAssignments).values();
        int kc = assertFieldEquals(knobs,0x78,KnobAssignments.KnobCount);
        List<FieldValues> kas = assertSubfields(knobs, kc, KnobAssignments.Knobs);
        for (int i = 0; i < kc; i++) {
            FieldValues ka = kas.get(i);
            int a = i == 0 ? 1 : 0;
            assertFieldEquals(ka,a,KnobAssignment.Assigned);
            List<FieldValues> kps = assertSubfields(ka, a, KnobAssignment.Params);
            if (i == 0) {
                FieldValues kp = kps.getFirst();
                assertFieldEquals(kp,1,KnobParams.Location);
                assertFieldEquals(kp,1,KnobParams.Index);
                assertFieldEquals(kp,0,KnobParams.IsLed);
                assertFieldEquals(kp,0,KnobParams.Param);

            }
        }

    }

    private void testControlAssignments(Patch p) {
        FieldValues cass = p.getSection(Patch.Sections.SControlAssignments).values();
        //System.out.println(cass);
        assertFieldEquals(cass,0x02,ControlAssignments.NumControls);
        List<FieldValues> cas = assertSubfields(cass, 2, ControlAssignments.Assignments);
        FieldValues ca = cas.getFirst();
        assertFieldEquals(ca,0x07,ControlAssignment.MidiCC); // volume CC (not avail as assign!)
        assertFieldEquals(ca,0x02,ControlAssignment.Location); // patch settings
        assertFieldEquals(ca,0x02,ControlAssignment.Index); // 0-2 = 0 -> vol/active
        assertFieldEquals(ca,0x00,ControlAssignment.Param); // volume
        ca = cas.get(1);
        assertFieldEquals(ca,0x11,ControlAssignment.MidiCC); // 17 (not avail as assign!)
        assertFieldEquals(ca,0x02,ControlAssignment.Location); // patch settings
        assertFieldEquals(ca,0x07,ControlAssignment.Index); // 7-2 = 5 -> oct/sus
        assertFieldEquals(ca,0x00,ControlAssignment.Param); // oct shift?? yes per manual

    }
    private void testMorphParams(Patch p, int vc) {
        FieldValues morphParams = p.getSection(Patch.Sections.SMorphParameters).values();

        assertFieldEquals(morphParams, vc,MorphParameters.VariationCount);
        assertFieldEquals(morphParams,8 ,MorphParameters.MorphCount);
        assertFieldEquals(morphParams,0 ,MorphParameters.Reserved);
        List<FieldValues> vms = assertSubfields(morphParams, vc, MorphParameters.VarMorphs);
        for (int i = 0; i < vc; i++) {
            FieldValues vm = vms.get(i);
            assertFieldEquals(vm,i,VarMorph.Variation);
            assertFieldEquals(vm,0,VarMorph.Reserved0);
            assertFieldEquals(vm,0,VarMorph.Reserved1);
            assertFieldEquals(vm,0,VarMorph.Reserved2);
            assertFieldEquals(vm,0,VarMorph.Reserved3);
            int mc = i == 1 ? 1 : 0;
            assertFieldEquals(vm,mc,VarMorph.MorphCount);
            List<FieldValues> vmps = assertSubfields(vm, mc, VarMorph.VarMorphParams);
            if (i == 1) {
                FieldValues vmp = vmps.getFirst();
                assertFieldEquals(vmp,0x01,VarMorphParam.Location);
                assertFieldEquals(vmp,0x02,VarMorphParam.ModuleIndex);
                assertFieldEquals(vmp,0x01,VarMorphParam.ParamIndex );
                assertFieldEquals(vmp,0x01,VarMorphParam.Morph      );
                assertFieldEquals(vmp,0x7f,VarMorphParam.Range      );
            }

        }
    }

    private void testModParams0(Patch p, int vc) {

        FieldValues modParams = p.getSection(Patch.Sections.SModuleParams0).values();

        assertFieldEquals(modParams,3,ModuleParams.SetCount);
        assertFieldEquals(modParams, vc,ModuleParams.VariationCount);
        List<FieldValues> mps = assertSubfields(modParams, 3, ModuleParams.ParamSet);

        List<FieldValues> vps = assertVarParams(mps, vc, 1, 3); //FX in
        int v = 0;
        assertModParams(v++,vps,1,1,2);
        assertModParams(v++,vps,1,1,2);
        while (v < vc) {
            assertModParams(v++,vps,0,1,1);
        }

        //dumpFieldValues(modParams);
        vps = assertVarParams(mps, vc, 2, 5); //Mixer 2-1A
        v = 0;
        while (v < vc) {
            assertModParams(v++,vps,100,1,100,v==2?0:1,0);
        }

        vps = assertVarParams(mps, vc, 3, 3); //2 out
        v = 0;
        while (v < vc) {
            assertModParams(v++,vps,0,1,0);
        }

    }

    private void testModParams1(Patch p, int vc) {

        FieldValues modParams = p.getSection(Patch.Sections.SModuleParams1).values();

        assertFieldEquals(modParams,0x04,ModuleParams.SetCount);
        assertFieldEquals(modParams, vc,ModuleParams.VariationCount);
        List<FieldValues> mps =
                new ArrayList<>(assertSubfields(modParams, 4, ModuleParams.ParamSet));

        List<FieldValues> vps;
        int v = 0;
        vps = assertVarParams(mps, vc, 1, 6); //filter classic
        assertModParams(v++,vps,58,0,1,11,0,1);
        assertModParams(v++,vps,58,0,1,11,0,1);
        while (v < vc) {
            assertModParams(v++,vps,75,0,0,0,2,1);
        }

        vps = assertVarParams(mps, vc, 2, 8); //Osc C
        v = 0;
        assertModParams(v++,vps,76,64,1,0,0,1,0,0);
        assertModParams(v++,vps,76, 0,1,0,0,1,0,0);
        while (v < vc) {
            assertModParams(v++,vps,64,64,1,0,0,1,0,0);
        }

        vps = assertVarParams(mps, vc, 3, 10); //ADSR
        v = 0;
        while (v < vc) {
            assertModParams(v++,vps,0, 54, 100, 14, 0, 0, 0, 0, 0, 1);
        }

        vps = assertVarParams(mps, vc, 4, 3); //2-out
        v = 0;
        assertModParams(v++,vps,3,1,1);
        assertModParams(v++,vps,3,1,1);
        while (v < vc) {
            assertModParams(v++,vps,0,1,0);
        }

    }

    @SuppressWarnings("unused")
    private static void dumpFieldValues(FieldValues fv) {
        dumpFieldValues(fv,0);
    }
    private static void dumpFieldValues(FieldValues fv,int indent) {
        Runnable ind = () -> {
            for (int i = 0; i < indent; i++) {
                System.out.print("  ");
            }
        };
        ind.run();
        for (int i = 0; i < fv.values.size(); i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            FieldValue v = fv.values.get(i);
            if (v instanceof SubfieldsValue) {
                System.out.println(v.field().name() + ": ");
                for (FieldValues sfv : ((SubfieldsValue) v).value()) {
                    dumpFieldValues(sfv, indent + 1);
                }
                System.out.print("  ");
            } else {
                System.out.print(v);
            }
        }
        System.out.println();
    }

    private int testPatchSettings(Patch p, int vce) {
        Patch.Section s = p.getSection(Patch.Sections.SPatchParams);
        assertEquals(2, Patch.Sections.SPatchParams.location,"location"); //patch parameters

        FieldValues patchSettings = s.values();
        int vc = assertFieldEquals(patchSettings,vce, PatchParams.VariationCount);
        assertFieldEquals(patchSettings,0x07, PatchParams.SectionCount);
        sectionHeader(patchSettings, PatchParams.S1,1,16);
        sectionHeader(patchSettings, PatchParams.S2,2,2);
        sectionHeader(patchSettings, PatchParams.S3,3,2);
        sectionHeader(patchSettings, PatchParams.S4,4,2);
        sectionHeader(patchSettings, PatchParams.S5,5,3);
        sectionHeader(patchSettings, PatchParams.S6,6,4);
        sectionHeader(patchSettings, PatchParams.S7,7,2);
        List<FieldValues> morphs = assertSubfields(patchSettings, vc, PatchParams.Morphs);
        List<FieldValues> s2 = assertSubfields(patchSettings, vc, PatchParams.SectionVolMuteds);
        List<FieldValues> s3 = assertSubfields(patchSettings, vc, PatchParams.SectionGlides);
        List<FieldValues> s4 = assertSubfields(patchSettings, vc, PatchParams.SectionBends);
        List<FieldValues> s5 = assertSubfields(patchSettings, vc, PatchParams.SectionVibratos);
        List<FieldValues> s6 = assertSubfields(patchSettings, vc, PatchParams.SectionArps);
        List<FieldValues> s7 = assertSubfields(patchSettings, vc, PatchParams.SectionOctSustains);
        for (int i = 0; i < vc; i++) {
            FieldValues ms = morphs.get(i);
            assertFieldEquals(ms,i,MorphSettings.Variation);
            List<FieldValues> mdials = assertSubfields(ms, 8, MorphSettings.Dials);
            List<FieldValues> mmodes = assertSubfields(ms, 8, MorphSettings.Modes);
            for (int j = 0; j < 8; j++) {
                if (i == 1 && j == 2) {
                    assertFieldEquals(mdials.get(j),25, Data7.Datum);
                } else {
                    assertFieldEquals(mdials.get(j),0x00, Data7.Datum);
                }
                assertFieldEquals(mmodes.get(j),0x01, Data7.Datum);
            }
            assertFieldEquals(s2.get(i),i, VolMutedSettings.Variation);
            assertFieldEquals(s3.get(i),i, GlideSettings.Variation);
            assertFieldEquals(s4.get(i),i, BendSettings.Variation);
            assertFieldEquals(s5.get(i),i, VibratoSettings.Variation);
            assertFieldEquals(s6.get(i),i, ArpSettings.Variation);
            assertFieldEquals(s7.get(i),i, OctSustainSettings.Variation);

            assertFieldEquals(s3.get(i),0x00, GlideSettings.Glide);
            assertFieldEquals(s3.get(i),0x1c, GlideSettings.GlideTime);

            assertFieldEquals(s5.get(i),0x00, VibratoSettings.Vibrato);
            assertFieldEquals(s5.get(i),0x32, VibratoSettings.Cents);
            assertFieldEquals(s5.get(i),0x40, VibratoSettings.Rate);

            assertFieldEquals(s6.get(i),0x00, ArpSettings.Arpeggiator);
            assertFieldEquals(s6.get(i),0x03, ArpSettings.Time);
            assertFieldEquals(s6.get(i),0x00, ArpSettings.Type);
            assertFieldEquals(s6.get(i),0x00, ArpSettings.Octaves);

            if (i == 1) {
                assertFieldEquals(s2.get(i),0x00, VolMutedSettings.PatchVol);
            } else {
                assertFieldEquals(s2.get(i),0x64, VolMutedSettings.PatchVol);
            }
            assertFieldEquals(s2.get(i),0x01, VolMutedSettings.ActiveMuted);
            if (i == 0 || i == 1) {
                assertFieldEquals(s4.get(i),0x05, BendSettings.Semi);
                assertFieldEquals(s7.get(i),0x01, OctSustainSettings.OctShift);
            } else {
                assertFieldEquals(s4.get(i),0x01, BendSettings.Semi);
                assertFieldEquals(s7.get(i),0x02, OctSustainSettings.OctShift);
            }

            assertFieldEquals(s4.get(i),0x01, BendSettings.Bend);

            assertFieldEquals(s7.get(i),0x01, OctSustainSettings.Sustain);

        }

        return vc;
    }
    private void testCableLists(Patch p,int... indexes) {

        FieldValues cl = p.getSection(Patch.Sections.SCableList1).values();
        //dumpFieldValues(cl);
        assertFieldEquals(cl,0,CableList.Reserved);
        assertFieldEquals(cl,3,CableList.CableCount);
        List<FieldValues> cs = assertSubfields(cl, 3, CableList.Cables);


        FieldValues cable = cs.get(indexes[0]);
        assertFieldEquals(cable,0x00, Cable.Color);
        assertFieldEquals(cable,0x03, Cable.SrcModule); //ModADSR
        assertFieldEquals(cable,0x01, Cable.SrcConn);
        assertFieldEquals(cable,0x01, Cable.Direction);
        assertFieldEquals(cable,0x04, Cable.DestModule); //2-out
        assertFieldEquals(cable,0x00, Cable.DestConn);

        cable = cs.get(indexes[1]);
        assertFieldEquals(cable,0x00, Cable.Color);
        assertFieldEquals(cable,0x01, Cable.SrcModule); //FltClassic
        assertFieldEquals(cable,0x00, Cable.SrcConn);
        assertFieldEquals(cable,0x01, Cable.Direction);
        assertFieldEquals(cable,0x03, Cable.DestModule); //ModADSR
        assertFieldEquals(cable,0x05, Cable.DestConn);

        cable = cs.get(indexes[2]);
        assertFieldEquals(cable,0x00, Cable.Color);
        assertFieldEquals(cable,0x02, Cable.SrcModule); //Osc C
        assertFieldEquals(cable,0x00, Cable.SrcConn);
        assertFieldEquals(cable,0x01, Cable.Direction);
        assertFieldEquals(cable,0x01, Cable.DestModule); // FltClassic
        assertFieldEquals(cable,0x00, Cable.DestConn);


        cl = p.getSection(Patch.Sections.SCableList0).values();
        assertFieldEquals(cl,0,CableList.Reserved);
        assertFieldEquals(cl,2,CableList.CableCount);
        cs = assertSubfields(cl, 2, CableList.Cables);

        cable = cs.get(indexes[3]);
        assertFieldEquals(cable,0x00, Cable.Color);
        assertFieldEquals(cable,0x02, Cable.SrcModule);
        assertFieldEquals(cable,0x00, Cable.SrcConn);
        assertFieldEquals(cable,0x01, Cable.Direction);
        assertFieldEquals(cable,0x03, Cable.DestModule);
        assertFieldEquals(cable,0x00, Cable.DestConn);

        cable = cs.get(indexes[4]);
        assertFieldEquals(cable,0x00, Cable.Color);
        assertFieldEquals(cable,0x01, Cable.SrcModule);
        assertFieldEquals(cable,0x00, Cable.SrcConn);
        assertFieldEquals(cable,0x01, Cable.Direction);
        assertFieldEquals(cable,0x02, Cable.DestModule);
        assertFieldEquals(cable,0x00, Cable.DestConn);



    }
    private void testModules(Patch p, int... indexes) {
        FieldValues modl = p.getSection(Patch.Sections.SModuleList1).values();
        List<FieldValues> mods = assertSubfields(modl, 4, ModuleList.Modules);

        FieldValues module;
        List<FieldValues> modes;

        //Util.dumpBuffer(b2);
        module = mods.get(0);
        assertFieldEquals(module,0x5c, Module_.Id); //filter classic
        assertFieldEquals(module,0x01, Module_.Index);
        assertFieldEquals(module,0x00, Module_.Horiz);
        assertFieldEquals(module,0x09, Module_.Vert);
        assertFieldEquals(module,0x00, Module_.Color);
        assertFieldEquals(module,0x00, Module_.Uprate);
        assertFieldEquals(module,0x00, Module_.Leds);
        assertFieldEquals(module,0x00, Module_.Reserved);
        assertFieldEquals(module,0x00, Module_.ModeCount);
        assertSubfields(module, 0, Module_.Modes);

        module = mods.get(1);
        assertFieldEquals(module,0x09, Module_.Id); //osc c
        assertFieldEquals(module,0x02, Module_.Index);
        assertFieldEquals(module,0x00, Module_.Horiz);
        assertFieldEquals(module,0x06, Module_.Vert);
        assertFieldEquals(module,0x00, Module_.Color);
        assertFieldEquals(module,0x00, Module_.Uprate);
        assertFieldEquals(module,0x00, Module_.Leds);
        assertFieldEquals(module,0x00, Module_.Reserved);
        assertFieldEquals(module,0x01, Module_.ModeCount);
        modes = assertSubfields(module, 1, Module_.Modes);
        assertFieldEquals(modes.getFirst(),0x02, ModuleModes.Data);

        module = mods.get(2);
        assertFieldEquals(module,0x17, Module_.Id);  // ModADSR
        assertFieldEquals(module,0x03, Module_.Index);
        assertFieldEquals(module,0x00, Module_.Horiz);
        assertFieldEquals(module,0x0d, Module_.Vert);
        assertFieldEquals(module,0x00, Module_.Color);
        assertFieldEquals(module,0x01, Module_.Uprate);
        assertFieldEquals(module,0x00, Module_.Leds);
        assertFieldEquals(module,0x00, Module_.Reserved);
        assertFieldEquals(module,0x00, Module_.ModeCount);
        assertSubfields(module, 0, Module_.Modes);


        module = mods.get(3);
        assertFieldEquals(module,0x04, Module_.Id); // 2-out
        assertFieldEquals(module,0x04, Module_.Index);
        assertFieldEquals(module,0x00, Module_.Horiz);
        assertFieldEquals(module,0x12, Module_.Vert);
        assertFieldEquals(module,0x00, Module_.Color);
        assertFieldEquals(module,0x00, Module_.Uprate);
        assertFieldEquals(module,0x01, Module_.Leds);
        assertFieldEquals(module,0x00, Module_.Reserved);
        assertFieldEquals(module,0x00, Module_.ModeCount);
        assertSubfields(module, 0, Module_.Modes);

        modl = p.getSection(Patch.Sections.SModuleList0).values();
        mods = assertSubfields(modl, 3, ModuleList.Modules);


        module = mods.get(indexes[0]);
        assertFieldEquals(module,0x7f, Module_.Id); // FX Input
        assertFieldEquals(module,0x01, Module_.Index);
        assertFieldEquals(module,0x01, Module_.Horiz);
        assertFieldEquals(module,0x02, Module_.Vert);
        assertFieldEquals(module,0x00, Module_.Color);
        assertFieldEquals(module,0x00, Module_.Uprate);
        assertFieldEquals(module,0x01, Module_.Leds);
        assertFieldEquals(module,0x00, Module_.Reserved);
        assertFieldEquals(module,0x00, Module_.ModeCount);
        assertSubfields(module, 0, Module_.Modes);


        module = mods.get(indexes[1]);
        //dumpFieldValues(module);
        assertFieldEquals(module,0xc2, Module_.Id);//Mixer 2-1A
        assertFieldEquals(module,0x02, Module_.Index);
        assertFieldEquals(module,0x01, Module_.Horiz);
        assertFieldEquals(module,0x04, Module_.Vert);
        assertFieldEquals(module,0x00, Module_.Color);
        assertFieldEquals(module,0x01, Module_.Uprate);
        assertFieldEquals(module,0x00, Module_.Leds);
        assertFieldEquals(module,0x00, Module_.Reserved);
        assertFieldEquals(module,0x00, Module_.ModeCount);
        assertSubfields(module, 0, Module_.Modes);


        module = mods.get(indexes[2]);
        assertFieldEquals(module,0x04, Module_.Id); //2-out
        assertFieldEquals(module,0x03, Module_.Index);
        assertFieldEquals(module,0x01, Module_.Horiz);
        assertFieldEquals(module,0x09, Module_.Vert);
        assertFieldEquals(module,0x00, Module_.Color);
        assertFieldEquals(module,0x00, Module_.Uprate);
        assertFieldEquals(module,0x01, Module_.Leds);
        assertFieldEquals(module,0x00, Module_.Reserved);
        assertFieldEquals(module,0x00, Module_.ModeCount);
        assertSubfields(module, 0, Module_.Modes);
    }

    private void testModuleLabels(Patch p) {
        FieldValues mlss = p.getSection(Patch.Sections.SModuleLabels1).values();
        assertFieldEquals(mlss,0x00,ModuleLabels.ModuleCount);

        mlss = p.getSection(Patch.Sections.SModuleLabels0).values();
        assertFieldEquals(mlss,0x01,ModuleLabels.ModuleCount);
        List<FieldValues> ml = assertSubfields(mlss, 1, ModuleLabels.ModLabels);
        FieldValues mls = ml.getFirst();
        assertFieldEquals(mls,0x02,ModuleLabel.ModuleIndex);
        assertFieldEquals(mls,0x14,ModuleLabel.ModLabelLen);
        List<FieldValues> ls = assertSubfields(mls, 2, ModuleLabel.Labels);

        FieldValues l = ls.getFirst();
        assertFieldEquals(l,0x01,ParamLabel.IsString);
        assertFieldEquals(l,0x08,ParamLabel.ParamLen);
        assertFieldEquals(l,0x01,ParamLabel.ParamIndex);
        assertFieldEquals(l,"Ch 1",ParamLabel.Label);

        l = ls.get(1);
        assertFieldEquals(l,0x01,ParamLabel.IsString);
        assertFieldEquals(l,0x08,ParamLabel.ParamLen);
        assertFieldEquals(l,0x03,ParamLabel.ParamIndex);
        assertFieldEquals(l,"Ch Two",ParamLabel.Label);
    }


    private void testTextPad(Patch p) {
        FieldValues tp = p.getSection(Patch.Sections.STextPad).values();
        assertFieldEquals(tp,"Writing notes ...",TextPad.Text);
    }

    private void testCurrentNote(Patch p) {
        FieldValues cns = p.getSection(Patch.Sections.SCurrentNote).values();
        assertFieldEquals(cns,0x40,CurrentNote.Note);
        assertFieldEquals(cns,0x00,CurrentNote.Attack);
        assertFieldEquals(cns,0x00,CurrentNote.Release);
        assertFieldEquals(cns,0x05,CurrentNote.NoteCount); //note that this stores actual count - 1
        List<FieldValues> ns = assertSubfields(cns, 6, CurrentNote.Notes);
        for (int i = 0; i < 6; i++) {
            FieldValues n = ns.get(i);
            assertFieldEquals(n,0x40,NoteData.Note);
            assertFieldEquals(n,0x00,NoteData.Attack);
            assertFieldEquals(n,0x00,NoteData.Release);
        }
    }


    @Test
    void readTextPadMessage() throws Exception {
        ByteBuffer buf = Util.readFile(TEXTPAD_MSG);
        assertEquals(0x01,buf.get()); // cmd
        assertEquals(0x09,buf.get()); // slot 0
        assertEquals(0x00,buf.get()); // patch version
        Patch p = new Patch();
        p.readSection(buf, Patch.Sections.STextPad);
        testTextPad(p);

    }

    @Test
    void readCurrentNoteMessage() throws Exception {
        ByteBuffer buf = Util.readFile(CURRENT_NOTE_MSG);
        assertEquals(0x01,buf.get()); // cmd
        assertEquals(0x09,buf.get()); // slot 0
        assertEquals(0x00,buf.get()); // patch version
        Patch p = new Patch();
        p.readSection(buf, Patch.Sections.SCurrentNote);
        testCurrentNote(p);
    }


    @Test
    void patchDesc0() throws Exception {

        ByteBuffer buf = Util.readFile(PATCHMSG_0);

        Patch.readFromMessage(buf);

    }

    @Test
    void patchFromMessage() throws Exception {
        ByteBuffer buf = Util.readFile(PATCHMSG_1);
        Patch p = Patch.readFromMessage(buf);
        assertEquals(9,p.slot);


        FieldValues pd = PatchDescription.FIELDS.values(
                PatchDescription.Reserved.value(Data8.asSubfield(1, 0xfc, 0, 0, 1, 0, 0)), //USB
                PatchDescription.Reserved2.value(0x04), //USB
                PatchDescription.Voices.value(0x05),
                PatchDescription.Height.value(374),
                PatchDescription.Unk2.value(0x01),
                PatchDescription.Red.value(0x01),
                PatchDescription.Blue.value(0x01),
                PatchDescription.Yellow.value(0x01),
                PatchDescription.Orange.value(0x01),
                PatchDescription.Green.value(0x01),
                PatchDescription.Purple.value(0x01),
                PatchDescription.White.value(0x01),
                PatchDescription.MonoPoly.value(0x00),
                PatchDescription.Variation.value(0x01),
                PatchDescription.Category.value(0x00),
                PatchDescription.Reserved3.value(0x00)
        );
        assertEquals(pd,p.getSection(Patch.Sections.SPatchDescription).values());
        testModules(p,0,1,2);
        testCableLists(p,0,1,2,0,1);
        int vc = testPatchSettings(p,10);
        testModParams1(p, vc);
        testModParams0(p, vc);
        testMorphParams(p, vc);
        testKnobAssignments(p);
        testControlAssignments(p);
        testModuleNames(p);
        testMorphLabels(p);
        testModuleLabels(p);

        p.toPatch().writeYaml("data/hello.yaml");
    }
    @Test
    public void patchFromFile() throws Exception {
        Patch p = Patch.readFromFile(PATCH_FILE);
        FieldValues pd = PatchDescription.FIELDS.values(
                PatchDescription.Reserved.value(Data8.asSubfield(0, 0, 0, 0, 0, 0, 0)), //File
                PatchDescription.Reserved2.value(0x00), //File
                PatchDescription.Voices.value(0x05),
                PatchDescription.Height.value(374),
                PatchDescription.Unk2.value(0x01),
                PatchDescription.Red.value(0x01),
                PatchDescription.Blue.value(0x01),
                PatchDescription.Yellow.value(0x01),
                PatchDescription.Orange.value(0x01),
                PatchDescription.Green.value(0x01),
                PatchDescription.Purple.value(0x01),
                PatchDescription.White.value(0x01),
                PatchDescription.MonoPoly.value(0x00),
                PatchDescription.Variation.value(0x01),
                PatchDescription.Category.value(0x00),
                PatchDescription.Reserved3.value(0x00)
        );
        assertEquals(pd,p.getSection(Patch.Sections.SPatchDescription).values(),"PatchDescription");

        testModules(p,0,2,1);

        testCurrentNote(p);

        testCableLists(p,2,1,0,1,0); //LOL reversed in patch!!!
        int vc = testPatchSettings(p,9);
        testModParams1(p,vc);
        testModParams0(p,vc);
        testMorphParams(p,vc);
        testKnobAssignments(p);
        testControlAssignments(p);
        testMorphLabels(p);
        testModuleLabels(p);
        testModuleNames(p);
        testTextPad(p);
    }

    @Test
    void roundtripMsgFile() throws Exception {
        ByteBuffer msgfile = Util.readFile(PATCHMSG_1);
        Patch p = Patch.readFromMessage(msgfile);
        p.readSectionMessage(Util.readFile(CURRENT_NOTE_MSG), Patch.Sections.SCurrentNote);
        p.readSectionMessage(Util.readFile(TEXTPAD_MSG), Patch.Sections.STextPad);
        ByteBuffer msgbuf = p.writeMessage();
        assertEquals(msgfile.rewind(),msgbuf.rewind());

        Patch.Section pd = p.getSection(Patch.Sections.SPatchDescription);
        pd.values().update(PatchDescription.Reserved.value(Data8.asSubfield(0, 0, 0, 0, 0, 0, 0)));
        pd.values().update(PatchDescription.Reserved2.value(0x00));
        //this file is made by g2lib as a manual test, so this is a regression
        assertEquals(Util.readFile("data/simplesynth001-g2lib.pch2").rewind(),p.writeFile().rewind());

    }

    @Test
    void roundtripPatchFile() throws Exception {
        Patch p = Patch.readFromFile(PATCH_FILE);
        ByteBuffer buf = p.writeFile();
        ByteBuffer filebuf = Util.readFile(PATCH_FILE);
        assertEquals(filebuf.rewind(),buf.rewind());
    }


    @Test
    void readSynthSettingsMessage() throws Exception {
        ByteBuffer buf = Util.readFile("data/msg_SynthSettings_f574.msg");
        assertEquals(0x01,buf.get()); // cmd
        assertEquals(0x0c,buf.get());
        assertEquals(0x00,buf.get()); //perf version
        assertEquals(0x03,buf.get()); //synth settings
        BitBuffer bb = new BitBuffer(buf.slice());
        FieldValues ss = SynthSettings.FIELDS.read(bb);
        FieldValues ex = SynthSettings.FIELDS.values(
                SynthSettings.DeviceName.value("ModularG2R"),
                SynthSettings.PerfMode.value(1),
                SynthSettings.Reserved0.value(0),
                SynthSettings.Reserved1.value(0),
                SynthSettings.PerfBank.value(0),
                SynthSettings.PerfLocation.value(0),
                SynthSettings.MemoryProtect.value(0),
                SynthSettings.Reserved2.value(0),
                SynthSettings.MidiChannelA.value(0),
                SynthSettings.MidiChannelB.value(1),
                SynthSettings.MidiChannelC.value(2),
                SynthSettings.MidiChannelD.value(3),
                SynthSettings.MidiChannelGlobal.value(0),
                SynthSettings.SysExId.value(16),
                SynthSettings.LocalOn.value(1),
                SynthSettings.Reserved3.value(0),
                SynthSettings.Reserved4.value(0),
                SynthSettings.ProgramChangeReceive.value(1),
                SynthSettings.ProgramChangeSend.value(0),
                SynthSettings.Reserved5.value(0),
                SynthSettings.ControllersReceive.value(1),
                SynthSettings.ControllersSend.value(0),
                SynthSettings.Reserved6.value(0),
                SynthSettings.SendClock.value(0),
                SynthSettings.IgnoreExternalClock.value(0),
                SynthSettings.Reserved7.value(0),
                SynthSettings.TuneCent.value(0),
                SynthSettings.GlobalOctaveShiftActive.value(0),
                SynthSettings.Reserved8.value(0),
                SynthSettings.GlobalOctaveShift.value(0),
                SynthSettings.TuneSemi.value(0),
                SynthSettings.Reserved9.value(0),
                SynthSettings.PedalPolarity.value(0),
                SynthSettings.ReservedA.value(64),
                SynthSettings.ControlPedalGain.value(0));
        assertEquals(ex,ss,"SynthSettings");
    }

    @Test
    void readEntryList() throws Exception {
        ByteBuffer buf = Util.readFile("data/msg_PatchListMessage00_19f4.msg");
        assertEquals(0x01, buf.get()); // cmd
        assertEquals(0x0c, buf.get());
        assertEquals(0x00, buf.get()); //perf version
        assertEquals(0x13, buf.get()); //entry list
        BitBuffer bb = new BitBuffer(buf.slice());
        FieldValues fvs = BankEntries.FIELDS.read(bb);
        System.out.println(fvs);
    }

}
