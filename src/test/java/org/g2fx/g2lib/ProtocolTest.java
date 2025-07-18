package org.g2fx.g2lib;

import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.protocol.*;
import org.g2fx.g2lib.state.PatchLoadData;
import org.g2fx.g2lib.state.PerformanceSettings;
import org.g2fx.g2lib.state.*;
import org.g2fx.g2lib.usb.UsbMessage;
import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.g2fx.g2lib.protocol.Protocol.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Optional<Integer> i = field.intValueMaybe(values);
        assertTrue(i.isPresent(),"value found: " + field);
        return i.get();
    }

    private static String assertString(FieldValues values, FieldEnum field) {
        Optional<String> s = field.stringValueMaybe(values);
        assertTrue(s.isPresent(),"value found: " + field);
        return s.get();
    }

    public static List<FieldValues> assertSubfields(FieldValues fv, int size, FieldEnum field) {
        Optional<List<FieldValues>> o = field.subfieldsValueMaybe(fv);
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




    private void testMorphLabels(Patch p) {
        FieldValues mls = p.getSection(Sections.SMorphLabels).values();
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
        FieldValues mns = p.getSection(Sections.SModuleNames1).values();
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

        mns = p.getSection(Sections.SModuleNames0).values();

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
        FieldValues knobs = p.getSection(Sections.SKnobAssignments).values();
        int kc = assertFieldEquals(knobs,0x78, Protocol.KnobAssignments.KnobCount);
        List<FieldValues> kas = assertSubfields(knobs, kc, Protocol.KnobAssignments.Knobs);
        for (int i = 0; i < kc; i++) {
            FieldValues ka = kas.get(i);
            int a = i == 0 ? 1 : 0;
            assertFieldEquals(ka,a,KnobAssignment.Assigned);
        }

        List<FieldValues> akas = p.getKnobAssignments().getActiveAssignments();
        assertEquals(1,akas.size());
        FieldValues kp = akas.getFirst();
        assertFieldEquals(kp,1,KnobParams.Location);
        assertFieldEquals(kp,1,KnobParams.Index);
        assertFieldEquals(kp,0,KnobParams.IsLed);
        assertFieldEquals(kp,0,KnobParams.Param);

    }

    private void testControlAssignments(Patch p) {
        FieldValues cass = p.getSection(Sections.SControlAssignments).values();
        //System.out.println(cass);
        assertFieldEquals(cass,0x02,Protocol.ControlAssignments.NumControls);
        List<FieldValues> cas = assertSubfields(cass, 2, Protocol.ControlAssignments.Assignments);
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
        FieldValues morphParams = p.getSection(Sections.SMorphParameters).values();

        assertFieldEquals(morphParams, vc,Protocol.MorphParameters.VariationCount);
        assertFieldEquals(morphParams,8 ,Protocol.MorphParameters.MorphCount);
        assertFieldEquals(morphParams,0 ,Protocol.MorphParameters.Reserved);
        List<FieldValues> vms = assertSubfields(morphParams, vc, Protocol.MorphParameters.VarMorphs);
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
            assertEquals(mc,vmps.size());
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

        FieldValues modParams = p.getSection(Sections.SModuleParams0).values();

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

        FieldValues modParams = p.getSection(Sections.SModuleParams1).values();

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
        Patch.Section s = p.getSection(Sections.SPatchParams);
        assertEquals(2, Sections.SPatchParams.location,"location"); //patch parameters

        FieldValues patchSettings = s.values();
        int vc = assertFieldEquals(patchSettings,vce, ModuleParams.VariationCount);
        assertFieldEquals(patchSettings,0x07, ModuleParams.SetCount);
        List<FieldValues> sps = new ArrayList<>(ModuleParams.ParamSet.subfieldsValue(patchSettings));
        assertEquals(7,sps.size());
        testSettingsSection(p, sps.removeFirst(), SettingsModules.Morphs,16,vce,List.of(
                List.of(0,0,0,0,0,0,0,0, 1,1,1,1,1,1,1,1),
                List.of(0,0,25,0,0,0,0,0, 1,1,1,1,1,1,1,1),
                List.of(0,0,0,0,0,0,0,0, 1,1,1,1,1,1,1,1)));
        testSettingsSection(p, sps.removeFirst(), SettingsModules.Gain,2,vce,List.of(
                List.of(0x64,1),
                List.of(0,1),
                List.of(0x64,1)));
        testSettingsSection(p, sps.removeFirst(), SettingsModules.Glide,2,vce,List.of(
                List.of(0,0x1c)));
        testSettingsSection(p, sps.removeFirst(), SettingsModules.Bend,2,vce,List.of(
                List.of(1,5),
                List.of(1,5),
                List.of(1,1)));
        testSettingsSection(p, sps.removeFirst(), SettingsModules.Vibrato,3,vce,List.of(
                List.of(0,0x32,0x40)));
        testSettingsSection(p, sps.removeFirst(), SettingsModules.Arpeggiator,4,vce,List.of(
                List.of(0,3,0,0)));
        testSettingsSection(p, sps.removeFirst(), SettingsModules.Misc,2,vce,List.of(
                List.of(1,1),
                List.of(1,1),
                List.of(2,1)));
        return vc;
    }


    private void testSettingsSection(Patch patch, FieldValues svs, SettingsModules section, int entries, int varCount,
                                     List<List<Integer>> varValues) {
        assertFieldEquals(svs,section.getModIndex(),ModuleParamSet.ModIndex);
        assertFieldEquals(svs,entries,ModuleParamSet.ParamCount);
        List<FieldValues> vvs = ModuleParamSet.ModParams.subfieldsValue(svs);
        for (int v = 0; v < varCount; v++) {
            List<Integer> expecteds = varValues.size() > v ? varValues.get(v) : varValues.getLast();
            FieldValues vs = vvs.get(v);
            assertFieldEquals(vs,v,VarParams.Variation);
            assertEquals(expecteds,
                    VarParams.Params.subfieldsValue(vs).stream().map(Data7.Datum::intValue).toList(),
                    "Settings Section " + section + " var " + v + " values");
            assertEquals(expecteds,
                    patch.getArea(AreaId.Settings).getSettingsModule(section).getVarValues(v));
        }
    }

    private void testCableLists(Patch p,int... indexes) {

        FieldValues cl = p.getSection(Sections.SCableList1).values();
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


        cl = p.getSection(Sections.SCableList0).values();
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
        FieldValues modl = p.getSection(Sections.SModuleList1).values();
        List<FieldValues> mods = assertSubfields(modl, 4, ModuleList.Modules);

        FieldValues module;
        List<FieldValues> modes;

        //Util.dumpBuffer(b2);
        module = mods.getFirst();
        assertFieldEquals(module,0x5c, UserModule.Id); //filter classic
        assertFieldEquals(module,0x01, UserModule.Index);
        assertFieldEquals(module,0x00, UserModule.Horiz);
        assertFieldEquals(module,0x09, UserModule.Vert);
        assertFieldEquals(module,0x00, UserModule.Color);
        assertFieldEquals(module,0x00, UserModule.Uprate);
        assertFieldEquals(module,0x00, UserModule.Leds);
        assertFieldEquals(module,0x00, UserModule.Reserved);
        assertFieldEquals(module,0x00, UserModule.ModeCount);
        assertSubfields(module, 0, UserModule.Modes);

        module = mods.get(1);
        assertFieldEquals(module,0x09, UserModule.Id); //osc c
        assertFieldEquals(module,0x02, UserModule.Index);
        assertFieldEquals(module,0x00, UserModule.Horiz);
        assertFieldEquals(module,0x06, UserModule.Vert);
        assertFieldEquals(module,0x00, UserModule.Color);
        assertFieldEquals(module,0x00, UserModule.Uprate);
        assertFieldEquals(module,0x00, UserModule.Leds);
        assertFieldEquals(module,0x00, UserModule.Reserved);
        assertFieldEquals(module,0x01, UserModule.ModeCount);
        modes = assertSubfields(module, 1, UserModule.Modes);
        assertFieldEquals(modes.getFirst(),0x02, ModuleModes.Data);

        module = mods.get(2);
        assertFieldEquals(module,0x17, UserModule.Id);  // ModADSR
        assertFieldEquals(module,0x03, UserModule.Index);
        assertFieldEquals(module,0x00, UserModule.Horiz);
        assertFieldEquals(module,0x0d, UserModule.Vert);
        assertFieldEquals(module,0x00, UserModule.Color);
        assertFieldEquals(module,0x01, UserModule.Uprate);
        assertFieldEquals(module,0x00, UserModule.Leds);
        assertFieldEquals(module,0x00, UserModule.Reserved);
        assertFieldEquals(module,0x00, UserModule.ModeCount);
        assertSubfields(module, 0, UserModule.Modes);


        module = mods.get(3);
        assertFieldEquals(module,0x04, UserModule.Id); // 2-out
        assertFieldEquals(module,0x04, UserModule.Index);
        assertFieldEquals(module,0x00, UserModule.Horiz);
        assertFieldEquals(module,0x12, UserModule.Vert);
        assertFieldEquals(module,0x00, UserModule.Color);
        assertFieldEquals(module,0x00, UserModule.Uprate);
        assertFieldEquals(module,0x01, UserModule.Leds);
        assertFieldEquals(module,0x00, UserModule.Reserved);
        assertFieldEquals(module,0x00, UserModule.ModeCount);
        assertSubfields(module, 0, UserModule.Modes);

        modl = p.getSection(Sections.SModuleList0).values();
        mods = assertSubfields(modl, 3, ModuleList.Modules);


        module = mods.get(indexes[0]);
        assertFieldEquals(module,0x7f, UserModule.Id); // FX Input
        assertFieldEquals(module,0x01, UserModule.Index);
        assertFieldEquals(module,0x01, UserModule.Horiz);
        assertFieldEquals(module,0x02, UserModule.Vert);
        assertFieldEquals(module,0x00, UserModule.Color);
        assertFieldEquals(module,0x00, UserModule.Uprate);
        assertFieldEquals(module,0x01, UserModule.Leds);
        assertFieldEquals(module,0x00, UserModule.Reserved);
        assertFieldEquals(module,0x00, UserModule.ModeCount);
        assertSubfields(module, 0, UserModule.Modes);


        module = mods.get(indexes[1]);
        //dumpFieldValues(module);
        assertFieldEquals(module,0xc2, UserModule.Id);//Mixer 2-1A
        assertFieldEquals(module,0x02, UserModule.Index);
        assertFieldEquals(module,0x01, UserModule.Horiz);
        assertFieldEquals(module,0x04, UserModule.Vert);
        assertFieldEquals(module,0x00, UserModule.Color);
        assertFieldEquals(module,0x01, UserModule.Uprate);
        assertFieldEquals(module,0x00, UserModule.Leds);
        assertFieldEquals(module,0x00, UserModule.Reserved);
        assertFieldEquals(module,0x00, UserModule.ModeCount);
        assertSubfields(module, 0, UserModule.Modes);


        module = mods.get(indexes[2]);
        assertFieldEquals(module,0x04, UserModule.Id); //2-out
        assertFieldEquals(module,0x03, UserModule.Index);
        assertFieldEquals(module,0x01, UserModule.Horiz);
        assertFieldEquals(module,0x09, UserModule.Vert);
        assertFieldEquals(module,0x00, UserModule.Color);
        assertFieldEquals(module,0x00, UserModule.Uprate);
        assertFieldEquals(module,0x01, UserModule.Leds);
        assertFieldEquals(module,0x00, UserModule.Reserved);
        assertFieldEquals(module,0x00, UserModule.ModeCount);
        assertSubfields(module, 0, UserModule.Modes);
    }

    private void testModuleLabels(Patch p) {
        FieldValues mlss = p.getSection(Sections.SModuleLabels1).values();
        assertFieldEquals(mlss,0x00,ModuleLabels.ModuleCount);

        mlss = p.getSection(Sections.SModuleLabels0).values();
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
        FieldValues tp = p.getSection(Sections.STextPad).values();
        assertFieldEquals(tp,"Writing notes ...",TextPad.Text);
    }

    private void testCurrentNote(Patch p) {
        FieldValues cns = p.getSection(Sections.SCurrentNote).values();
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
        Patch p = new Patch(Slot.A);
        p.readSection(buf, Sections.STextPad);
        testTextPad(p);

    }

    @Test
    void readCurrentNoteMessage() throws Exception {
        ByteBuffer buf = Util.readFile(CURRENT_NOTE_MSG);
        assertEquals(0x01,buf.get()); // cmd
        assertEquals(0x09,buf.get()); // slot 0
        assertEquals(0x00,buf.get()); // patch version
        Patch p = new Patch(Slot.A);
        p.readSection(buf, Sections.SCurrentNote);
        testCurrentNote(p);
    }


    @Test
    void patchDesc0() throws Exception {

        ByteBuffer buf = Util.readFile(PATCHMSG_0);

        Patch.readFromMessage(0, Slot.A,buf);

    }


    @Test
    void testPatchFromMessage302a() throws Exception {
        ByteBuffer buf = Util.readFile("data/msg_PatchDesc_302a.msg");
        Patch p = Patch.readFromMessage(4, Slot.A, buf);
    }

    @Test
    void testPatchFromMessage839d() throws Exception {
        ByteBuffer buf = Util.readFile("data/msg_PatchDesc_839d.msg");
        Patch p = Patch.readFromMessage(1, Slot.D, buf);
    }

    @Test
    void patchFromMessage() throws Exception {
        ByteBuffer buf = Util.readFile(PATCHMSG_1);
        Patch p = Patch.readFromMessage(0, Slot.B,buf);
//        assertEquals(9,p.slot);
        assertEquals(0,p.version);


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
        assertEquals(pd,p.getSection(Sections.SPatchDescription).values());
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
    }
    @Test
    public void patchFromFile() throws Exception {
        testFilePatch(Patch.readFromFile(Slot.A,PATCH_FILE),new int[]{0,2,1},new int[]{2,1,0,1,0});
    }

    private void testFilePatch(Patch p, int[] fxModuleIndexes, int[] cableIndexes) {
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
        assertEquals(pd, p.getSection(Sections.SPatchDescription).values(),"PatchDescription");

        testModules(p,fxModuleIndexes);

        testCurrentNote(p);

        testCableLists(p,cableIndexes); //LOL reversed in patch!!!
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
        Patch p = Patch.readFromMessage(0, Slot.B,msgfile);
        p.readSectionMessage(Util.readFile(CURRENT_NOTE_MSG), Sections.SCurrentNote);
        p.readSectionMessage(Util.readFile(TEXTPAD_MSG), Sections.STextPad);
        ByteBuffer msgbuf = p.writeMessage();
        assertEquals(Util.dumpBufferString(msgfile.rewind()),
                Util.dumpBufferString(msgbuf.rewind()));

        Patch.Section pd = p.getSection(Sections.SPatchDescription);
        pd.values().update(PatchDescription.Reserved.value(Data8.asSubfield(0, 0, 0, 0, 0, 0, 0)));
        pd.values().update(PatchDescription.Reserved2.value(0x00));
        //this file is made by g2lib as a manual test, so this is a regression
        assertEquals(Util.readFile("data/simplesynth001-g2lib.pch2").rewind(),p.writeFile().rewind());

    }

    @Test
    void roundtripPatchFile() throws Exception {
        Patch p = Patch.readFromFile(Slot.A,PATCH_FILE);
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
        FieldValues ss = Protocol.SynthSettings.FIELDS.read(bb);
        FieldValues ex = Protocol.SynthSettings.FIELDS.values(
                Protocol.SynthSettings.DeviceName.value("ModularG2R"),
                Protocol.SynthSettings.PerfMode.value(1),
                Protocol.SynthSettings.Reserved0.value(0),
                Protocol.SynthSettings.Reserved1.value(0),
                Protocol.SynthSettings.PerfBank.value(0),
                Protocol.SynthSettings.PerfLocation.value(0),
                Protocol.SynthSettings.MemoryProtect.value(0),
                Protocol.SynthSettings.Reserved2.value(0),
                Protocol.SynthSettings.MidiChannelA.value(0),
                Protocol.SynthSettings.MidiChannelB.value(1),
                Protocol.SynthSettings.MidiChannelC.value(2),
                Protocol.SynthSettings.MidiChannelD.value(3),
                Protocol.SynthSettings.MidiChannelGlobal.value(0),
                Protocol.SynthSettings.SysExId.value(16),
                Protocol.SynthSettings.LocalOn.value(1),
                Protocol.SynthSettings.Reserved3.value(0),
                Protocol.SynthSettings.Reserved4.value(0),
                Protocol.SynthSettings.ProgramChangeReceive.value(1),
                Protocol.SynthSettings.ProgramChangeSend.value(0),
                Protocol.SynthSettings.Reserved5.value(0),
                Protocol.SynthSettings.ControllersReceive.value(1),
                Protocol.SynthSettings.ControllersSend.value(0),
                Protocol.SynthSettings.Reserved6.value(0),
                Protocol.SynthSettings.SendClock.value(0),
                Protocol.SynthSettings.IgnoreExternalClock.value(0),
                Protocol.SynthSettings.Reserved7.value(0),
                Protocol.SynthSettings.TuneCent.value(0),
                Protocol.SynthSettings.GlobalOctaveShiftActive.value(0),
                Protocol.SynthSettings.Reserved8.value(0),
                Protocol.SynthSettings.GlobalOctaveShift.value(0),
                Protocol.SynthSettings.TuneSemi.value(0),
                Protocol.SynthSettings.Reserved9.value(0),
                Protocol.SynthSettings.PedalPolarity.value(0),
                Protocol.SynthSettings.ReservedA.value(64),
                Protocol.SynthSettings.ControlPedalGain.value(0));
        assertEquals(ex,ss,"SynthSettings");
    }


    @Test
    void readPerformanceSettingsMsg() throws Exception {
        ByteBuffer buf = Util.readFile("data/msg_PerfSettings_a69a.msg");
        Performance perf = new Performance().readFromMessage(buf);
        assertEquals("eff new6",perf.getName());
        testPerformanceSettings(perf.getPerfSettings());
        /*
        01 0c 00 29 65 66 66 20 6e 65 77 36 00 11 00 58   . . . ) e f f . n e w 6 . . . X
        00 04 00 78 00 00 00 00 4e 6f 20 6e 61 6d 65 00   . . . x . . . . N o . n a m e .
        01 01 00 00 00 00 7f 00 00 00 73 69 6d 70 6c 65   . . . . . . . . . . s i m p l e
        20 73 79 6e 74 68 20 30 30 31 01 01 00 00 00 00   . s y n t h . 0 0 1 . . . . . .
        7f 01 00 00 4e 6f 20 6e 61 6d 65 00 01 00 00 00   . . . . N o . n a m e . . . . .
        00 00 7f 02 00 00 4e 6f 20 6e 61 6d 65 00 01 00   . . . . . . N o . n a m e . . .
        00 00 00 00 7f 03 00 00 a6 9a                     . . . . . . . . . .
         */
    }

    @Test
    void readGlobalKnobsMsg() throws Exception {
        ByteBuffer buf = Util.readFile("data/msg_GlobalKnobs_850a.msg");
        Performance perf = new Performance();
        perf.readSectionMessage(buf,Sections.SGlobalKnobAssignments);
        assertEquals(0,perf.getGlobalKnobAssignments().getActiveAssignments().size());
    }

    private static void testPerformanceSettings(PerformanceSettings ps) {
        assertEquals(1, ps.selectedSlot().get());
        assertEquals(false, ps.keyboardRangeEnabled().get());
        assertEquals(0x78, (int) ps.masterClock().get());
        assertEquals(false, ps.masterClockRun().get());

        {
            SlotSettings ss = ps.getSlotSettings(Slot.A);
            assertEquals("No name", ss.patchName().get());
            assertEquals(true, ss.enabled().get());
            assertEquals(true, ss.keyboard().get());
            assertEquals(false, ss.hold().get());
            assertEquals(0, ss.bankIndex().get());
            assertEquals(0, ss.patchIndex().get());
            assertEquals(0, ss.keyboardRangeFrom().get());
            assertEquals(0x7f, ss.keyboardRangeTo().get());
        }

        {
            SlotSettings ss = ps.getSlotSettings(Slot.B);
            assertEquals("simple synth 001", ss.patchName().get());
            assertEquals(true, ss.enabled().get());
            assertEquals(true, ss.keyboard().get());
            assertEquals(false, ss.hold().get());
            assertEquals(0, ss.bankIndex().get());
            assertEquals(0, ss.patchIndex().get());
            assertEquals(0, ss.keyboardRangeFrom().get());
            assertEquals(0x7f, ss.keyboardRangeTo().get());
        }

        {
            SlotSettings ss = ps.getSlotSettings(Slot.C);
            assertEquals("No name", ss.patchName().get());
            assertEquals(true, ss.enabled().get());
            assertEquals(false, ss.keyboard().get());
            assertEquals(false, ss.hold().get());
            assertEquals(0, ss.bankIndex().get());
            assertEquals(0, ss.patchIndex().get());
            assertEquals(0, ss.keyboardRangeFrom().get());
            assertEquals(0x7f, ss.keyboardRangeTo().get());
        }

        {
            SlotSettings ss = ps.getSlotSettings(Slot.C);
            assertEquals("No name", ss.patchName().get());
            assertEquals(true, ss.enabled().get());
            assertEquals(false, ss.keyboard().get());
            assertEquals(false, ss.hold().get());
            assertEquals(0, ss.bankIndex().get());
            assertEquals(0, ss.patchIndex().get());
            assertEquals(0, ss.keyboardRangeFrom().get());
            assertEquals(0x7f, ss.keyboardRangeTo().get());
        }
    }

    @Test
    void readPerformanceFile() throws Exception {
        Performance perf = Performance.readFromFile("data/perf-20240802.prf2");
        testPerformanceSettings(perf.getPerfSettings());
        testFilePatch(perf.getSlot(Slot.B),new int[]{0,1,2},new int[]{0,1,2,0,1});

    }

    @Test
    void readPatchLoad() throws Exception {
        ByteBuffer buf = Util.readFile("data/msg_PatchLoadVA_b852.msg");
        Patch patch = new Patch(Slot.A);
        patch.version = 0;
        patch.readPatchLoadDataMsg(new UsbMessage(0,true,0,buf));
        PatchLoadData plVoice = patch.getArea(AreaId.Voice).getPatchLoadData();
        assertEquals(13, plVoice.getCycles());
        assertEquals(11, plVoice.getMem());
        ByteBuffer buf2 = Util.readFile("data/msg_PatchLoadFX_df05.msg");
        patch.readPatchLoadDataMsg(new UsbMessage(0,true,0,buf2));
        PatchLoadData plFx = patch.getArea(AreaId.Fx).getPatchLoadData();
        assertEquals(84, plFx.getCycles());
        assertEquals(87, plFx.getMem());
    }

}
