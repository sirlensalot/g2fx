package g2lib.model;

import java.util.List;

public enum ModuleType {
  M_Keyboard(1,2,"Keyboard",
    ModPage.InOut.ix(5),
    List.of(),
    List.of(
      Port.out("Pitch", ConnColor.Blue,2,1),
      Port.out("Gate", ConnColor.Yellow,5,1),
      Port.out("Lin", ConnColor.Blue,9,1),
      Port.out("Release", ConnColor.Blue,12,1),
      Port.out("Note", ConnColor.Blue,15,1),
      Port.out("Exp", ConnColor.Blue,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_4_Out(3,2,"4 outputs",
    ModPage.InOut.ix(5),
    List.of(
      Port.in("In1", ConnColor.Red,13,1),
      Port.in("In2", ConnColor.Red,15,1),
      Port.in("In3", ConnColor.Red,17,1),
      Port.in("In4", ConnColor.Red,19,1)
    ),
    List.of(),
    List.of(
      ModParam.Dst_2.mk("Destination"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Pad_1.mk("Pad")
    ),
    List.of()
  ),
  M_2_Out(4,2,"2 outputs",
    ModPage.InOut.ix(5),
    List.of(
      Port.in("InL", ConnColor.Red,17,1),
      Port.in("InR", ConnColor.Red,19,1)
    ),
    List.of(),
    List.of(
      ModParam.Dst_1.mk("Destination"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Pad_1.mk("Pad")
    ),
    List.of()
  ),
  M_Invert(5,2,"Logic Inverter",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("In1", ConnColor.Yellow_orange,7,1),
      Port.in("In2", ConnColor.Yellow_orange,15,1)
    ),
    List.of(
      Port.out("Out1", ConnColor.Yellow_orange,11,1),
      Port.out("Out2", ConnColor.Yellow_orange,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_OscB(7,5,"Osc B",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Pitch", ConnColor.Blue_red,0,3),
      Port.in("PitchVar", ConnColor.Blue_red,0,4),
      Port.in("Sync", ConnColor.Red,0,1),
      Port.in("FmMod", ConnColor.Red,9,4),
      Port.in("ShapeMod", ConnColor.Red,14,4)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,4)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.Level_100.mk("PitchMod"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.Level_100.mk("FmAmount"),
      ModParam.PW.mk("Shape"),
      ModParam.Level_100.mk("ShapeMod"),
      ModParam.OscBWaveform.mk("Waveform"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.FmLinTrk.mk("FmMode")
    ),
    List.of()
  ),
  M_OscShpB(8,4,"Osc Shape B",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Pitch", ConnColor.Blue_red,0,2),
      Port.in("PitchVar", ConnColor.Blue_red,0,3),
      Port.in("Sync", ConnColor.Red,0,1),
      Port.in("FmMod", ConnColor.Red,10,3),
      Port.in("ShapeMod", ConnColor.Red,15,3)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,3)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.Level_100.mk("PitchMod"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.Level_100.mk("FmAmount"),
      ModParam.PW.mk("Shape"),
      ModParam.Level_100.mk("ShapeMod"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.FmLinTrk.mk("FmMode")
    ),
    List.of(
      ModParam.OscWaveform_3.mk("Waveform")
    )
  ),
  M_OscC(9,3,"Osc C",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("PitchVar", ConnColor.Blue_red,0,2),
      Port.in("Sync", ConnColor.Red,12,2),
      Port.in("FmMod", ConnColor.Red,14,2),
      Port.in("Pitch", ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.Level_100.mk("FmAmount"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.FmLinTrk.mk("FmMode"),
      ModParam.Level_100.mk("PitchMod")
    ),
    List.of(
      ModParam.OscWaveform_2.mk("Waveform")
    )
  ),
  M_Reverb(12,3,"reverb",
    ModPage.FX.ix(5),
    List.of(
      Port.in("InL", ConnColor.Red,17,0),
      Port.in("InR", ConnColor.Red,19,0)
    ),
    List.of(
      Port.out("OutL", ConnColor.Red,17,2),
      Port.out("OutR", ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.ReverbTime.mk("Time"),
      ModParam.Level_100.mk("Brightness"),
      ModParam.Level_100.mk("DryWet"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of(
      ModParam.RoomType.mk("RoomType")
    )
  ),
  M_OscString(13,3,"Osc String",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,19,0),
      Port.in("Pitch", ConnColor.Blue,0,1),
      Port.in("PitchVar", ConnColor.Blue,0,2)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.Level_100.mk("PitchMod"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.Level_100.mk("Decay"),
      ModParam.Level_100.mk("Moisture"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Sw8_1(15,4,"Switch 8-1",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In1", ConnColor.Blue_red,6,0),
      Port.in("In2", ConnColor.Blue_red,7,1),
      Port.in("In3", ConnColor.Blue_red,9,0),
      Port.in("In4", ConnColor.Blue_red,11,1),
      Port.in("In5", ConnColor.Blue_red,13,0),
      Port.in("In6", ConnColor.Blue_red,14,1),
      Port.in("In7", ConnColor.Blue_red,15,0),
      Port.in("In8", ConnColor.Blue_red,18,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,3),
      Port.out("Control", ConnColor.Blue,9,3)
    ),
    List.of(
            ModParam.Sw_3.mk("Sel",
            "In 1", "In 2", "In 3", "In 4", "In 5", "In 6", "In 7", "In 8")
    ),
    List.of()
  ),
  M_ValSw1_2(17,2,"Value Switch 1-2",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("Input", ConnColor.Blue_red,14,2),
      Port.in("Ctrl", ConnColor.Blue_red,0,2)
    ),
    List.of(
      Port.out("OutOn", ConnColor.Blue_red,16,2),
      Port.out("OutOff", ConnColor.Blue_red,19,2)
    ),
    List.of(
      ModParam.ValSwVal.mk("Val",
              "Out 1", "Out 2")
    ),
    List.of()
  ),
  M_X_Fade(18,2,"Cross Fader",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1", ConnColor.Blue_red,14,2),
      Port.in("In2", ConnColor.Blue_red,16,2),
      Port.in("Mod", ConnColor.Blue_red,6,2)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,2)
    ),
    List.of(
      ModParam.Level_100.mk("MixMod"),
      ModParam.Bipolar_127.mk("Mix"),
      ModParam.LogLin.mk("LogLin")
    ),
    List.of()
  ),
  M_Mix4_1B(19,2,"Mixer 4-1 B",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1", ConnColor.Blue_red,5,1),
      Port.in("In2", ConnColor.Blue_red,8,1),
      Port.in("In3", ConnColor.Blue_red,12,1),
      Port.in("In4", ConnColor.Blue_red,14,1),
      Port.in("Chain", ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.MixLevel.mk("Lev1"),
      ModParam.MixLevel.mk("Lev2"),
      ModParam.MixLevel.mk("Lev3"),
      ModParam.MixLevel.mk("Lev4"),
      ModParam.ExpLin_2.mk("ExpLin")
    ),
    List.of()
  ),
  M_EnvADSR(20,4,"Envelop ADSR",
    ModPage.Env.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,19,0),
      Port.in("Gate", ConnColor.Yellow,0,2),
      Port.in("AM", ConnColor.Blue,0,3)
    ),
    List.of(
      Port.out("Env", ConnColor.Blue,18,3),
      Port.out("Out", ConnColor.Blue_red,19,3)
    ),
    List.of(
      ModParam.EnvShape_3.mk("Shape"),
      ModParam.EnvTime.mk("Attack"),
      ModParam.EnvTime.mk("Decay"),
      ModParam.EnvLevel.mk("Sustain"),
      ModParam.EnvTime.mk("Release"),
      ModParam.PosNegInvBipInv.mk("OutputType"),
      ModParam.OffOn.mk("KB"),
      ModParam.EnvNR.mk("NR")
    ),
    List.of()
  ),
  M_Mux1_8(21,2,"Multiplexer 1-8",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,6,1),
      Port.in("Ctrl", ConnColor.Blue,0,1)
    ),
    List.of(
      Port.out("Out1", ConnColor.Blue_red,7,1),
      Port.out("Out2", ConnColor.Blue_red,9,1),
      Port.out("Out3", ConnColor.Blue_red,11,1),
      Port.out("Out4", ConnColor.Blue_red,12,1),
      Port.out("Out5", ConnColor.Blue_red,14,1),
      Port.out("Out6", ConnColor.Blue_red,16,1),
      Port.out("Out7", ConnColor.Blue_red,18,1),
      Port.out("Out8", ConnColor.Blue_red,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_PartQuant(22,2,"Partial Quantizer",
    ModPage.Note.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.PartialRange.mk("Range")
    ),
    List.of()
  ),
  M_ModADSR(23,5,"Envelope Modulation ADSR",
    ModPage.Env.ix(5),
    List.of(
      Port.in("Gate", ConnColor.Yellow,0,4),
      Port.in("AttackMod", ConnColor.Blue,2,4),
      Port.in("DecayMod", ConnColor.Blue,5,4),
      Port.in("SustainMod", ConnColor.Blue,8,4),
      Port.in("ReleaseMod", ConnColor.Blue,11,4),
      Port.in("In", ConnColor.Blue_red,19,0),
      Port.in("AM", ConnColor.Blue,0,4)
    ),
    List.of(
      Port.out("Env", ConnColor.Blue,19,4),
      Port.out("Out", ConnColor.Blue_red,17,4)
    ),
    List.of(
      ModParam.EnvTime.mk("Attack"),
      ModParam.EnvTime.mk("Decay"),
      ModParam.EnvLevel.mk("Sustain"),
      ModParam.EnvTime.mk("Release"),
      ModParam.Level_100.mk("AttackMod"),
      ModParam.Level_100.mk("DecayMod"),
      ModParam.Level_100.mk("SustainMod"),
      ModParam.Level_100.mk("ReleaseMod"),
      ModParam.PosNegInvBipInv.mk("OutputType"),
      ModParam.OffOn.mk("KB")
    ),
    List.of()
  ),
  M_LfoC(24,2,"LFO C",
    ModPage.LFO.ix(5),
    List.of(
      Port.in("Rate", ConnColor.Blue,0,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.LfoRate_3.mk("Rate"),
      ModParam.PolyMono.mk("PolyMono"),
      ModParam.OutTypeLfo.mk("OutputType"),
      ModParam.LfoRange_3.mk("Range"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of(
      ModParam.LfoWaveform_1.mk("Waveform")
    )
  ),
  M_LfoShpA(25,5,"LFO Shape A",
    ModPage.LFO.ix(5),
    List.of(
      Port.in("Rate", ConnColor.Blue,1,4),
      Port.in("RateVar", ConnColor.Blue,2,4),
      Port.in("Rst", ConnColor.Blue,0,1),
      Port.in("ShapeMod", ConnColor.Blue,9,4),
      Port.in("PhaseMod", ConnColor.Blue,12,4),
      Port.in("Dir", ConnColor.Blue,1,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue,19,4),
      Port.out("Snc", ConnColor.Blue,0,4)
    ),
    List.of(
      ModParam.LfoRate_4.mk("Rate"),
      ModParam.LfoRange_4.mk("Range"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.Level_100.mk("RateMod"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.LfoShpAPW.mk("Shape"),
      ModParam.Level_100.mk("PhaseMod"),
      ModParam.Phase.mk("Phase"),
      ModParam.Level_100.mk("ShapeMod"),
      ModParam.PolyMono.mk("PolyMono"),
      ModParam.OutTypeLfo.mk("OutputType"),
      ModParam.LfoShpA__Waveform.mk("Waveform")
    ),
    List.of()
  ),
  M_LfoA(26,3,"LFO A",
    ModPage.LFO.ix(5),
    List.of(
      Port.in("Rate", ConnColor.Blue,0,1),
      Port.in("RateVar", ConnColor.Blue,0,2)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue,19,2)
    ),
    List.of(
      ModParam.LfoRate_3.mk("Rate"),
      ModParam.PolyMono.mk("PolyMono"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.Level_100.mk("RateMod"),
      ModParam.LfoA_Waveform.mk("Waveform"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.OutTypeLfo.mk("OutputType"),
      ModParam.LfoRange_3.mk("Range")
    ),
    List.of()
  ),
  M_OscMaster(27,3,"Osc Master",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Pitch", ConnColor.Blue_red,0,1),
      Port.in("PitchVar", ConnColor.Blue_red,0,2)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,2)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.FreqMode_2.mk("FreqMode"),
      ModParam.Level_100.mk("PitchMod")
    ),
    List.of()
  ),
  M_Saturate(28,2,"Saturate",
    ModPage.Shaper.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1),
      Port.in("Mod", ConnColor.Blue_red,8,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("Amount"),
      ModParam.Level_100.mk("AmountMod"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.SaturateCurve.mk("Curve")
    ),
    List.of()
  ),
  M_MetNoise(29,2,"Metallic Noise Oscillator",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("FreqMod", ConnColor.Blue_red,4,1),
      Port.in("ColorMod", ConnColor.Blue_red,11,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("Color"),
      ModParam.Level_100.mk("Freq"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Level_100.mk("FreqMod"),
      ModParam.Level_100.mk("ColorMod")
    ),
    List.of()
  ),
  M_Device(30,3,"Device",
    ModPage.InOut.ix(5),
    List.of(),
    List.of(
      Port.out("Wheel", ConnColor.Blue,0,2),
      Port.out("AftTouch", ConnColor.Blue,3,2),
      Port.out("ControlPedal", ConnColor.Blue,6,2),
      Port.out("SustainPedal", ConnColor.Yellow,10,2),
      Port.out("PitchStick", ConnColor.Blue,13,2),
      Port.out("GlobalWheel1", ConnColor.Blue,16,2),
      Port.out("GlobalWheel2", ConnColor.Blue,19,2)
    ),
    List.of(),
    List.of()
  ),
  M_Noise(31,2,"Noise",
    ModPage.Osc.ix(5),
    List.of(),
    List.of(
      Port.out("Out", ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.NoiseColor.mk("Color"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Eq2Band(32,3,"Eq 2 Band",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,19,0)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.EqdB.mk("LoSlope"),
      ModParam.EqdB.mk("HiSlope"),
      ModParam.Level_100.mk("Level"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.EqLoFreq.mk("LoFreq"),
      ModParam.EqHiFreq.mk("HiFreq")
    ),
    List.of()
  ),
  M_Eq3Band(33,4,"Eq 3 Band",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,19,0)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,3)
    ),
    List.of(
      ModParam.EqdB.mk("LoSlope"),
      ModParam.EqdB.mk("MidGain"),
      ModParam.EqMidFreq.mk("MidFreq"),
      ModParam.EqdB.mk("HiSlope"),
      ModParam.Level_100.mk("Level"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.EqLoFreq.mk("LoFreq"),
      ModParam.EqHiFreq.mk("HiFreq")
    ),
    List.of()
  ),
  M_ShpExp(34,2,"Shape Exp",
    ModPage.Shaper.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1),
      Port.in("Mod", ConnColor.Blue_red,8,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("Amount"),
      ModParam.Level_100.mk("AmountMod"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.ShpExpCurve.mk("Curve")
    ),
    List.of()
  ),
  M_SwOnOffM(36,2,"Switch On/Off Momentary",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1),
      Port.out("Ctrl", ConnColor.Blue,0,1)
    ),
    List.of(
      ModParam.OffOn.mk("On","On")
    ),
    List.of()
  ),
  M_Pulse(38,2,"Pulse",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("In", ConnColor.Yellow_orange,15,0),
      Port.in("Time", ConnColor.Blue_red,4,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Yellow_orange,19,1)
    ),
    List.of(
      ModParam.LogicTime.mk("Time"),
      ModParam.Level_100.mk("TimeMod"),
      ModParam.LogicRange.mk("Range")
    ),
    List.of(
      ModParam.PulseMode.mk("Mode")
    )
  ),
  M_Mix8_1B(40,4,"Mixer 8-1 B",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1", ConnColor.Blue_red,3,1),
      Port.in("In2", ConnColor.Blue_red,5,1),
      Port.in("In3", ConnColor.Blue_red,7,1),
      Port.in("In4", ConnColor.Blue_red,9,1),
      Port.in("In5", ConnColor.Blue_red,11,1),
      Port.in("In6", ConnColor.Blue_red,13,1),
      Port.in("In7", ConnColor.Blue_red,15,1),
      Port.in("In8", ConnColor.Blue_red,17,1),
      Port.in("Chain", ConnColor.Blue_red,19,0)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,3)
    ),
    List.of(
      ModParam.MixLevel.mk("Lev1"),
      ModParam.MixLevel.mk("Lev2"),
      ModParam.MixLevel.mk("Lev3"),
      ModParam.MixLevel.mk("Lev4"),
      ModParam.MixLevel.mk("Lev5"),
      ModParam.MixLevel.mk("Lev6"),
      ModParam.MixLevel.mk("Lev7"),
      ModParam.MixLevel.mk("Lev8"),
      ModParam.ExpLin_2.mk("ExpLin"),
      ModParam.Pad_3.mk("Pad")
    ),
    List.of()
  ),
  M_EnvH(41,2,"Envelope Hold",
    ModPage.Env.ix(5),
    List.of(
      Port.in("Trig", ConnColor.Yellow,0,1),
      Port.in("AM", ConnColor.Blue,3,1),
      Port.in("In", ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Env", ConnColor.Blue,15,1),
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.EnvTime.mk("Hold"),
      ModParam.PosNegInv.mk("OutputType")
    ),
    List.of()
  ),
  M_Delay(42,2,"Logic Delay",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("In", ConnColor.Yellow_orange,15,0),
      Port.in("TimeMod", ConnColor.Blue_red,4,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Yellow_orange,19,1)
    ),
    List.of(
      ModParam.LogicTime.mk("Time"),
      ModParam.Level_100.mk("TimeMod"),
      ModParam.LogicRange.mk("Range")
    ),
    List.of(
      ModParam.LogicDelayMode.mk("Mode")
    )
  ),
  M_Constant(43,2,"Constant Value",
    ModPage.Level.ix(5),
    List.of(),
    List.of(
      Port.out("Out", ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.LevBipUni.mk("Level"),
      ModParam.BipUni.mk("BipUni")
    ),
    List.of()
  ),
  M_LevMult(44,2,"Level Multiplier",
    ModPage.Level.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1),
      Port.in("Mod", ConnColor.Blue_red,14,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_FltVoice(45,4,"Filter Voice",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,19,0),
      Port.in("VowelMod", ConnColor.Blue,9,3),
      Port.in("FreqMod", ConnColor.Red,0,3)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,3)
    ),
    List.of(
      ModParam.Vowel.mk("Vowel1"),
      ModParam.Vowel.mk("Vowel2"),
      ModParam.Vowel.mk("Vowel3"),
      ModParam.Level_100.mk("Level"),
      ModParam.Level_100.mk("Vowel"),
      ModParam.Bipolar_127.mk("VowelMod"),
      ModParam.Level_100.mk("Freq"),
      ModParam.Bipolar_127.mk("FreqMod"),
      ModParam.Bipolar_127.mk("Res"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_EnvAHD(46,4,"Envelope AHD",
    ModPage.Env.ix(5),
    List.of(
      Port.in("Trig", ConnColor.Yellow,0,2),
      Port.in("AM", ConnColor.Blue,0,3),
      Port.in("In", ConnColor.Blue_red,19,0)
    ),
    List.of(
      Port.out("Env", ConnColor.Blue,18,0),
      Port.out("Out", ConnColor.Blue_red,19,0)
    ),
    List.of(
      ModParam.EnvShape_3.mk("Shape"),
      ModParam.EnvTime.mk("Attack"),
      ModParam.EnvTime.mk("Hold"),
      ModParam.EnvNR.mk("NR"),
      ModParam.EnvTime.mk("Decay"),
      ModParam.PosNegInv.mk("OutputType"),
      ModParam.OffOn.mk("KB")
    ),
    List.of()
  ),
  M_Pan(47,2,"Pan",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,14,1),
      Port.in("Mod", ConnColor.Blue_red,6,1)
    ),
    List.of(
      Port.out("OutL", ConnColor.Blue_red,16,1),
      Port.out("OutR", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("PanMod"),
      ModParam.Bipolar_127.mk("Pan"),
      ModParam.LogLin.mk("LogLin")
    ),
    List.of()
  ),
  M_MixStereo(48,5,"Mixer Stereo",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1", ConnColor.Blue_red,0,2),
      Port.in("In2", ConnColor.Blue_red,2,2),
      Port.in("In3", ConnColor.Blue_red,5,2),
      Port.in("In4", ConnColor.Blue_red,8,2),
      Port.in("In5", ConnColor.Blue_red,10,2),
      Port.in("In6", ConnColor.Blue_red,13,2)
    ),
    List.of(
      Port.out("OutL", ConnColor.Blue_red,17,4),
      Port.out("OutR", ConnColor.Blue_red,19,4)
    ),
    List.of(
      ModParam.Level_100.mk("Lev1"),
      ModParam.Level_100.mk("Lev2"),
      ModParam.Level_100.mk("Lev3"),
      ModParam.Level_100.mk("Lev4"),
      ModParam.Level_100.mk("Lev5"),
      ModParam.Level_100.mk("Lev6"),
      ModParam.Bipolar_127.mk("Pan1"),
      ModParam.Bipolar_127.mk("Pan2"),
      ModParam.Bipolar_127.mk("Pan3"),
      ModParam.Bipolar_127.mk("Pan4"),
      ModParam.Bipolar_127.mk("Pan5"),
      ModParam.Bipolar_127.mk("Pan6"),
      ModParam.Level_100.mk("LevMaster")
    ),
    List.of()
  ),
  M_FltMulti(49,4,"Filter Multi-mode",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,19,0),
      Port.in("PitchVar", ConnColor.Blue_red,0,2),
      Port.in("Pitch", ConnColor.Blue_red,0,3)
    ),
    List.of(
      Port.out("LP", ConnColor.Red,19,1),
      Port.out("BP", ConnColor.Red,19,2),
      Port.out("HP", ConnColor.Red,19,3)
    ),
    List.of(
      ModParam.FltFreq.mk("Freq"),
      ModParam.Level_200.mk("PitchMod"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.GcOffOn.mk("GC"),
      ModParam.Res_1.mk("Res"),
      ModParam.FltSlope_1.mk("Slope"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_ConstSwT(50,2,"Constant Switch Toggling",
    ModPage.Level.ix(5),
    List.of(),
    List.of(
      Port.out("Out", ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.LevBipUni.mk("Lev"),
      ModParam.OffOn.mk("On","Switch"),
      ModParam.BipUni.mk("BipUni")
    ),
    List.of()
  ),
  M_FltNord(51,5,"Filter Nord",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,19,0),
      Port.in("PitchVar", ConnColor.Blue_red,0,4),
      Port.in("Pitch", ConnColor.Blue_red,0,3),
      Port.in("FMLin", ConnColor.Blue_red,3,4),
      Port.in("Res", ConnColor.Blue_red,9,4)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,4)
    ),
    List.of(
      ModParam.FltFreq.mk("Freq"),
      ModParam.Level_200.mk("PitchMod"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.GcOffOn.mk("GC"),
      ModParam.Res_1.mk("Res"),
      ModParam.FltSlope_2.mk("Slope"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Level_100.mk("FM_Lin"),
      ModParam.LpBpHpBr.mk("FilterType"),
      ModParam.Level_100.mk("ResMod")
    ),
    List.of()
  ),
  M_EnvMulti(52,6,"Envelope Multi",
    ModPage.Env.ix(5),
    List.of(
      Port.in("Gate", ConnColor.Yellow,0,2),
      Port.in("In", ConnColor.Blue_red,16,0),
      Port.in("AM", ConnColor.Blue,2,2)
    ),
    List.of(
      Port.out("Env", ConnColor.Blue,19,1),
      Port.out("Out", ConnColor.Blue_red,19,0)
    ),
    List.of(
      ModParam.EnvLevel.mk("Level1"),
      ModParam.EnvLevel.mk("Level2"),
      ModParam.EnvLevel.mk("Level3"),
      ModParam.EnvLevel.mk("Level4"),
      ModParam.EnvTime.mk("Time1"),
      ModParam.EnvTime.mk("Time2"),
      ModParam.EnvTime.mk("Time3"),
      ModParam.EnvTime.mk("Time4"),
      ModParam.EnvNR.mk("NR"),
      ModParam.SustainMode_2.mk("SustainMode"),
      ModParam.PosNegInvBip.mk("OutputType"),
      ModParam.OffOn.mk("KB"),
      ModParam.EnvShape_3.mk("Shape")
    ),
    List.of()
  ),
  M_S_and_H(53,2,"Sample & Hold",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,15,1),
      Port.in("Ctrl", ConnColor.Yellow_orange,12,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_FltStatic(54,3,"Filter Static",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,19,0)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.FltFreq.mk("Freq"),
      ModParam.Res_1.mk("Res"),
      ModParam.LpBpHp.mk("FilterType"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.GcOffOn.mk("GC")
    ),
    List.of()
  ),
  M_EnvD(55,2,"Envelope Decay",
    ModPage.Env.ix(5),
    List.of(
      Port.in("Trig", ConnColor.Yellow,0,1),
      Port.in("AM", ConnColor.Blue,3,1),
      Port.in("In", ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Env", ConnColor.Blue,15,1),
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.EnvTime.mk("Decay"),
      ModParam.PosNegInv.mk("OutputType")
    ),
    List.of()
  ),
  M_Automate(57,2,"MIDI Control Automate",
    ModPage.MIDI.ix(5),
    List.of(
      Port.in("In", ConnColor.Yellow,0,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Yellow,4,1)
    ),
    List.of(
      ModParam.MidiData.mk("Ctrl"),
      ModParam.MidiData.mk("Val"),
      ModParam.MidiCh_20.mk("Ch"),
      ModParam.OffOn.mk("Echo")
    ),
    List.of()
  ),
  M_DrumSynth(58,8,"Drum Synthesizer",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Trig", ConnColor.Yellow,0,2),
      Port.in("Vel", ConnColor.Blue,0,7),
      Port.in("Pitch", ConnColor.Blue,0,4)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,8)
    ),
    List.of(
      ModParam.DrumSynthFreq.mk("Masterfreq"),
      ModParam.DrumSynthRatio.mk("SlaveRatio"),
      ModParam.EnvTime.mk("MasterDecay"),
      ModParam.EnvTime.mk("SlaveDecay"),
      ModParam.Level_100.mk("MasterLevel"),
      ModParam.Level_100.mk("SlaveLevel"),
      ModParam.DrumSynthNoiseFlt.mk("NoiseFltFreq"),
      ModParam.Level_100.mk("NoiseFltRes"),
      ModParam.Level_100.mk("NoiseFltSweep"),
      ModParam.EnvTime.mk("NoiseFltDecay"),
      ModParam.LpBpHp.mk("NoiseFltMode"),
      ModParam.Level_100.mk("BendAmount"),
      ModParam.EnvTime.mk("BendDecay"),
      ModParam.Level_100.mk("Click"),
      ModParam.Level_100.mk("Noise"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_CompLev(59,2,"Compare to Level",
    ModPage.Level.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,9,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Yellow_orange,19,1)
    ),
    List.of(
      ModParam.Bipolar_127.mk("C")
    ),
    List.of()
  ),
  M_Mux8_1X(60,3,"Multiplexer 8-1 with variable X-Fade",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In1", ConnColor.Blue_red,5,1),
      Port.in("In2", ConnColor.Blue_red,7,1),
      Port.in("In3", ConnColor.Blue_red,9,1),
      Port.in("In4", ConnColor.Blue_red,10,1),
      Port.in("In5", ConnColor.Blue_red,12,1),
      Port.in("In6", ConnColor.Blue_red,14,1),
      Port.in("In7", ConnColor.Blue_red,16,1),
      Port.in("In8", ConnColor.Blue_red,17,1),
      Port.in("Ctrl", ConnColor.Blue,0,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("XFade")
    ),
    List.of()
  ),
  M_Clip(61,2,"Clip",
    ModPage.Shaper.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1),
      Port.in("Mod", ConnColor.Blue_red,5,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("ClipLevMod"),
      ModParam.Level_100.mk("ClipLev"),
      ModParam.ClipShape.mk("Shape"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Overdrive(62,2,"Overdrive",
    ModPage.Shaper.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1),
      Port.in("Mod", ConnColor.Blue_red,8,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("AmountMod"),
      ModParam.Level_100.mk("Amount"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.OverdriveType.mk("Type"),
      ModParam.ClipShape.mk("Shape")
    ),
    List.of()
  ),
  M_Scratch(63,3,"Scratch",
    ModPage.FX.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,19,0),
      Port.in("Mod", ConnColor.Blue,0,2)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.ScratchRatio.mk("Ratio"),
      ModParam.Level_100.mk("RatioMod"),
      ModParam.ScratchDelay.mk("Delay"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Gate(64,2,"Gate",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("In1_1", ConnColor.Yellow_orange,6,0),
      Port.in("In1_2", ConnColor.Yellow_orange,5,1),
      Port.in("In2_1", ConnColor.Yellow_orange,13,0),
      Port.in("In2_2", ConnColor.Yellow_orange,12,1)
    ),
    List.of(
      Port.out("Out1", ConnColor.Yellow_orange,11,1),
      Port.out("Out2", ConnColor.Yellow_orange,19,1)
    ),
    List.of(),
    List.of(
      ModParam.GateMode.mk("GateMode1"),
      ModParam.GateMode.mk("GateMode2")
    )
  ),
  M_Mix2_1B(66,2,"Scratch",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1", ConnColor.Blue_red,5,1),
      Port.in("In2", ConnColor.Blue_red,12,1),
      Port.in("Chain", ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.MixInvert.mk("Inv1"),
      ModParam.MixLevel.mk("Lev1"),
      ModParam.MixInvert.mk("Inv2"),
      ModParam.MixLevel.mk("Lev2"),
      ModParam.ExpLin_2.mk("ExpLin")
    ),
    List.of()
  ),
  M_ClkGen(68,4,"Clock Generator",
    ModPage.LFO.ix(5),
    List.of(
      Port.in("Rst", ConnColor.Yellow,0,1)
    ),
    List.of(
      Port.out("1/96", ConnColor.Yellow,19,1),
      Port.out("1/16", ConnColor.Yellow,19,2),
      Port.out("ClkActive", ConnColor.Yellow,19,0),
      Port.out("Sync", ConnColor.Yellow,19,3)
    ),
    List.of(
      ModParam.RateBpm.mk("Rate"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.InternalMaster.mk("Source"),
      ModParam.ClkGenBeatSync.mk("BeatSync"),
      ModParam.ClkGenSwing.mk("Swing")
    ),
    List.of()
  ),
  M_ClkDiv(69,2,"Clock Divider",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("Clk", ConnColor.Yellow_orange,8,1),
      Port.in("Rst", ConnColor.Yellow_orange,5,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Yellow_orange,19,1)
    ),
    List.of(
      ModParam.Range_128.mk("Divider")
    ),
    List.of(
      ModParam.ClkDivMode.mk("DivMode")
    )
  ),
  M_EnvFollow(71,2,"Envelope Follower",
    ModPage.Level.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.EnvFollowAttack.mk("Attack"),
      ModParam.EnvFollowRelease.mk("Release")
    ),
    List.of()
  ),
  M_NoteScaler(72,2,"Note Scaler",
    ModPage.Note.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.NoteRange.mk("Range")
    ),
    List.of()
  ),
  M_WaveWrap(74,2,"Wave Wrapper",
    ModPage.Shaper.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1),
      Port.in("Mod", ConnColor.Blue_red,8,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("AmountMod"),
      ModParam.Level_100.mk("Amount"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_NoteQuant(75,2,"Note Quantizer",
    ModPage.Note.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.NoteRange.mk("Range"),
      ModParam.NoteQuantNotes.mk("Notes")
    ),
    List.of()
  ),
  M_SwOnOffT(76,2,"Switch On/Off Toggle",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1),
      Port.out("Ctrl", ConnColor.Blue,0,1)
    ),
    List.of(
      ModParam.OffOn.mk("On","On")
    ),
    List.of()
  ),
  M_Sw1_8(78,4,"Switch 1-8",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,3,3)
    ),
    List.of(
      Port.out("Out1", ConnColor.Blue_red,6,2),
      Port.out("Out2", ConnColor.Blue_red,8,3),
      Port.out("Out3", ConnColor.Blue_red,10,2),
      Port.out("Out4", ConnColor.Blue_red,12,3),
      Port.out("Out5", ConnColor.Blue_red,13,2),
      Port.out("Out6", ConnColor.Blue_red,15,3),
      Port.out("Out7", ConnColor.Blue_red,17,2),
      Port.out("Out8", ConnColor.Blue_red,19,3),
      Port.out("Ctrl", ConnColor.Blue,0,3)
    ),
    List.of(
      ModParam.Sw_3.mk("Sel",
              "Out 1", "Out 2", "Out 3", "Out 4", "Out 5", "Out 6", "Out 7", "Out 8")
    ),
    List.of()
  ),
  M_Sw4_1(79,3,"Switch 4-1",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In1", ConnColor.Blue_red,6,1),
      Port.in("In2", ConnColor.Blue_red,9,1),
      Port.in("In3", ConnColor.Blue_red,13,1),
      Port.in("In4", ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,2),
      Port.out("Ctrl", ConnColor.Blue,0,2)
    ),
    List.of(
      ModParam.Sw_2.mk("Sel",
      "In 1", "In 2", "In 3", "In 4")
    ),
    List.of()
  ),
  M_LevAmp(81,2,"Level Amplifier",
    ModPage.Level.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.LevAmpGain.mk("Gain"),
      ModParam.LinDB.mk("Type")
    ),
    List.of()
  ),
  M_Rect(82,2,"Rectifier",
    ModPage.Shaper.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.RectMode.mk("Mode"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_ShpStatic(83,2,"Shape Static",
    ModPage.Shaper.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.ShpStaticMode.mk("Mode"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_EnvADR(84,3,"Envelope ADR",
    ModPage.Env.ix(5),
    List.of(
      Port.in("Gate", ConnColor.Yellow,0,2),
      Port.in("In", ConnColor.Blue_red,19,0),
      Port.in("AM", ConnColor.Blue,3,2)
    ),
    List.of(
      Port.out("Env", ConnColor.Blue,17,2),
      Port.out("Out", ConnColor.Blue_red,19,2),
      Port.out("End", ConnColor.Yellow,16,2)
    ),
    List.of(
      ModParam.EnvShape_3.mk("Shape"),
      ModParam.EnvTime.mk("Attack"),
      ModParam.EnvNR.mk("NR"),
      ModParam.EnvTime.mk("Release"),
      ModParam.TrigGate.mk("TG"),
      ModParam.PosNegInv.mk("OutputType"),
      ModParam.OffOn.mk("KB"),
      ModParam.AdAr.mk("DcyRel")
    ),
    List.of()
  ),
  M_WindSw(85,2,"Window Switch",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,16,1),
      Port.in("Ctrl", ConnColor.Blue_red,14,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Blue_red,19,1),
      Port.out("Gate", ConnColor.Yellow_orange,0,1)
    ),
    List.of(
      ModParam.Range_64.mk("ValFrom"),
      ModParam.Range_64.mk("ValTo")
    ),
    List.of()
  ),
  M_8Counter(86,2,"8 Counter",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("Clk", ConnColor.Yellow_orange,0,1),
      Port.in("Rst", ConnColor.Yellow_orange,3,1)
    ),
    List.of(
      Port.out("Out1", ConnColor.Yellow_orange,5,1),
      Port.out("Out2", ConnColor.Yellow_orange,7,1),
      Port.out("Out3", ConnColor.Yellow_orange,9,1),
      Port.out("Out4", ConnColor.Yellow_orange,11,1),
      Port.out("Out5", ConnColor.Yellow_orange,13,1),
      Port.out("Out6", ConnColor.Yellow_orange,15,1),
      Port.out("Out7", ConnColor.Yellow_orange,17,1),
      Port.out("Out8", ConnColor.Yellow_orange,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_FltLP(87,2,"Filter Lowpass",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,16,1),
      Port.in("Pitch", ConnColor.Blue_red,4,1)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.FltFreq.mk("Freq"),
      ModParam.Level_100.mk("FreqMod"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of(
      ModParam.HpLpSlopeMode.mk("SlopeMode")
    )
  ),
  M_Sw1_4(88,3,"Switch 1-4",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,3,2)
    ),
    List.of(
      Port.out("Out1", ConnColor.Blue_red,6,2),
      Port.out("Out2", ConnColor.Blue_red,10,2),
      Port.out("Out3", ConnColor.Blue_red,13,2),
      Port.out("Out4", ConnColor.Blue_red,17,2),
      Port.out("Ctrl", ConnColor.Blue,0,2)
    ),
    List.of(
      ModParam.Sw_2.mk("Sel",
              "Out 1", "Out 2", "Out 3", "Out 4")
    ),
    List.of()
  ),
  M_Flanger(89,3,"Flanger",
    ModPage.FX.ix(5),
    List.of(
      Port.in("In", ConnColor.Red,19,0)
    ),
    List.of(
      Port.out("Out", ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.FlangerRate.mk("Rate"),
      ModParam.Level_100.mk("Range"),
      ModParam.Level_100.mk("FeedBack"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Sw1_2(90,2,"Switch 1-2",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In", ConnColor.Blue_red,14,1)
    ),
    List.of(
      Port.out("Out2", ConnColor.Blue_red,16,1),
      Port.out("Out1", ConnColor.Blue_red,19,1),
      Port.out("Ctrl", ConnColor.Blue,0,1)
    ),
    List.of(
      ModParam.Sw_1.mk("Sel")
    ),
    List.of()
  ),
  M_FlipFlop(91,2,"Flip Flop",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,8,1),
      Port.in("In",ConnColor.Yellow_orange,11,0),
      Port.in("Rst",ConnColor.Yellow_orange,5,1)
    ),
    List.of(
      Port.out("NotQ",ConnColor.Yellow_orange,17,1),
      Port.out("Q",ConnColor.Yellow_orange,19,0)
    ),
    List.of(),
    List.of(
      ModParam.FlipFlopMode.mk("OperationMode")
    )
  ),
  M_FltClassic(92,4,"Filter Classic",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0),
      Port.in("PitchVar",ConnColor.Blue_red,0,3),
      Port.in("Pitch",ConnColor.Blue_red,0,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,3)
    ),
    List.of(
      ModParam.FltFreq.mk("Freq"),
      ModParam.Level_200.mk("PitchMod"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.Level_100.mk("Res"),
      ModParam.ClassicSlope.mk("Slope"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_StChorus(94,3,"Stereo Chorus",
    ModPage.FX.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,18,1)
    ),
    List.of(
      Port.out("OutL",ConnColor.Red,17,2),
      Port.out("OutR",ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.Level_100.mk("Detune"),
      ModParam.Level_100.mk("Amount"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_OscD(96,2,"Osc D",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Pitch",ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of(
      ModParam.OscWaveform_2.mk("Waveform")
    )
  ),
  M_OscA(97,3,"Osc A",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Pitch",ConnColor.Blue_red,0,1),
      Port.in("PitchVar",ConnColor.Blue_red,0,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.Level_100.mk("PitchMod"),
      ModParam.OscA_Waveform.mk("Waveform"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.FreqMode_3.mk("FreqMode")
    ),
    List.of()
  ),
  M_FreqShift(98,3,"Frequency Shifter",
    ModPage.FX.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,18,0),
      Port.in("Shift",ConnColor.Blue,1,2)
    ),
    List.of(
      Port.out("Dn",ConnColor.Red,17,1),
      Port.out("Up",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.FreqShiftFreq.mk("FreqShift"),
      ModParam.Level_100.mk("ShiftMod"),
      ModParam.FreqShiftRange.mk("Range"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Sw2_1(100,2,"Switch 2-1",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In1",ConnColor.Blue_red,14,0),
      Port.in("In2",ConnColor.Blue_red,16,0)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1),
      Port.out("Ctrl",ConnColor.Blue,0,1)
    ),
    List.of(
      ModParam.Sw_1.mk("Sel",
              "In 1", "In 2")
    ),
    List.of()
  ),
  M_FltPhase(102,5,"Filter Phase",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0),
      Port.in("PitchVar",ConnColor.Blue_red,0,4),
      Port.in("Spr",ConnColor.Blue,6,4),
      Port.in("FB",ConnColor.Blue,9,4),
      Port.in("Pitch",ConnColor.Blue_red,0,3)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,4)
    ),
    List.of(
      ModParam.Level_100.mk("PitchMod"),
      ModParam.Freq_2.mk("Freq"),
      ModParam.Level_100.mk("SpreadMod"),
      ModParam.Bipolar_127.mk("FB"),
      ModParam.FltPhaseNotchCount.mk("NotchCount"),
      ModParam.Level_100.mk("Spread"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Level_100.mk("Level"),
      ModParam.Level_100.mk("FBMod"),
      ModParam.FltPhaseType.mk("Type"),
      ModParam.Kbt_4.mk("Kbt")
    ),
    List.of()
  ),
  M_EqPeak(103,4,"Eq Peak",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,3)
    ),
    List.of(
      ModParam.Freq_3.mk("Freq"),
      ModParam.EqdB.mk("Gain"),
      ModParam.EqPeakBandwidth.mk("Bandwidth"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Level_100.mk("Level")
    ),
    List.of()
  ),
  M_ValSw2_1(105,2,"Value Switch 2-1",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In1",ConnColor.Blue_red,14,1),
      Port.in("In2",ConnColor.Blue_red,16,1),
      Port.in("Ctrl",ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.ValSwVal.mk("Val")
    ),
    List.of()
  ),
  M_OscNoise(106,3,"Oscillator Noise",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Pitch",ConnColor.Blue_red,0,1),
      Port.in("PitchVar",ConnColor.Blue_red,0,2),
      Port.in("Width",ConnColor.Blue_red,12,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.Level_100.mk("PitchMod"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.Level_100.mk("Width"),
      ModParam.Level_100.mk("WidthMod"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Vocoder(108,8,"Vocoder",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("Ctrl",ConnColor.Red,0,2),
      Port.in("In",ConnColor.Red,16,7)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,7)
    ),
    List.of(
      ModParam.VocoderBand.mk("Band1"),
      ModParam.VocoderBand.mk("Band2"),
      ModParam.VocoderBand.mk("Band3"),
      ModParam.VocoderBand.mk("Band4"),
      ModParam.VocoderBand.mk("Band5"),
      ModParam.VocoderBand.mk("Band6"),
      ModParam.VocoderBand.mk("Band7"),
      ModParam.VocoderBand.mk("Band8"),
      ModParam.VocoderBand.mk("Band9"),
      ModParam.VocoderBand.mk("Band10"),
      ModParam.VocoderBand.mk("Band11"),
      ModParam.VocoderBand.mk("Band12"),
      ModParam.VocoderBand.mk("Band13"),
      ModParam.VocoderBand.mk("Band14"),
      ModParam.VocoderBand.mk("Band15"),
      ModParam.VocoderBand.mk("Band16"),
      ModParam.OffOn.mk("Emphasis"),
      ModParam.ActiveMonitor.mk("Monitor")
    ),
    List.of()
  ),
  M_LevAdd(112,2,"Level Add",
    ModPage.Level.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.LevBipUni.mk("Level"),
      ModParam.BipUni.mk("BipUni")
    ),
    List.of()
  ),
  M_Fade1_2(113,2,"Fade 1-2",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,14,1),
      Port.in("Mod",ConnColor.Blue_red,6,1)
    ),
    List.of(
      Port.out("Out1",ConnColor.Blue_red,17,1),
      Port.out("Out2",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Fade12Mix.mk("Mix"),
      ModParam.Level_100.mk("MixMod")
    ),
    List.of()
  ),
  M_Fade2_1(114,2,"Fade 2-1",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1",ConnColor.Blue_red,14,1),
      Port.in("In2",ConnColor.Blue_red,16,1),
      Port.in("Mod",ConnColor.Blue_red,6,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Fade21Mix.mk("Mix"),
      ModParam.Level_100.mk("MixMod")
    ),
    List.of()
  ),
  M_LevScaler(115,3,"Level Scaler",
    ModPage.Note.ix(5),
    List.of(
      Port.in("Note",ConnColor.Blue,0,2),
      Port.in("In",ConnColor.Blue_red,19,0)
    ),
    List.of(
      Port.out("Level",ConnColor.Blue,16,2),
      Port.out("Out",ConnColor.Blue_red,19,2)
    ),
    List.of(
      ModParam.LevScaledB.mk("L"),
      ModParam.FreqCoarse.mk("BP"),
      ModParam.LevScaledB.mk("R"),
      ModParam.Kbt_1.mk("Kbt")
    ),
    List.of()
  ),
  M_Mix8_1A(116,2,"Mix 8-1 A",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1",ConnColor.Blue_red,5,1),
      Port.in("In2",ConnColor.Blue_red,7,1),
      Port.in("In3",ConnColor.Blue_red,8,1),
      Port.in("In4",ConnColor.Blue_red,10,1),
      Port.in("In5",ConnColor.Blue_red,12,1),
      Port.in("In6",ConnColor.Blue_red,13,1),
      Port.in("In7",ConnColor.Blue_red,15,1),
      Port.in("In8",ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Pad_3.mk("Pad")
    ),
    List.of()
  ),
  M_LevMod(117,3,"Level Modulator",
    ModPage.Level.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,16,2),
      Port.in("Mod",ConnColor.Blue_red,16,0),
      Port.in("ModDepth",ConnColor.Blue_red,6,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,2)
    ),
    List.of(
      ModParam.Level_100.mk("ModDepth"),
      ModParam.LevModAmRm.mk("ModType")
    ),
    List.of()
  ),
  M_Digitizer(118,3,"Digitizer",
    ModPage.FX.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,19,0),
      Port.in("Rate",ConnColor.Blue_red,19,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,1,2)
    ),
    List.of(
      ModParam.DigitizerBits.mk("Bits"),
      ModParam.DigitizerRate.mk("Rate"),
      ModParam.Level_100.mk("RateMod"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_EnvADDSR(119,5,"Envelope ADDSR",
    ModPage.Env.ix(5),
    List.of(
      Port.in("Gate",ConnColor.Yellow,0,2),
      Port.in("AM",ConnColor.Blue,0,3),
      Port.in("In",ConnColor.Blue_red,19,0)
    ),
    List.of(
      Port.out("Env",ConnColor.Blue,17,3),
      Port.out("Out",ConnColor.Blue_red,19,3)
    ),
    List.of(
      ModParam.OffOn.mk("KB"),
      ModParam.EnvShape_3.mk("Shape"),
      ModParam.EnvTime.mk("Attack"),
      ModParam.EnvTime.mk("Decay1"),
      ModParam.EnvLevel.mk("Level1"),
      ModParam.EnvTime.mk("Decay2"),
      ModParam.EnvLevel.mk("Level2"),
      ModParam.EnvTime.mk("Release"),
      ModParam.SustainMode_1.mk("SustainMode"),
      ModParam.PosNegInvBipInv.mk("OutputType"),
      ModParam.EnvNR.mk("NR")
    ),
    List.of()
  ),
  M_SeqNote(121,9,"Sequencer Note",
    ModPage.Seq.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,0,2),
      Port.in("Rst",ConnColor.Yellow_orange,0,4),
      Port.in("Loop",ConnColor.Yellow_orange,0,6),
      Port.in("Park",ConnColor.Yellow_orange,16,0),
      Port.in("Note",ConnColor.Blue_red,0,7),
      Port.in("Trig",ConnColor.Yellow_orange,0,8),
      Port.in("RecVal",ConnColor.Blue_red,7,0),
      Port.in("RecEnable",ConnColor.Yellow_orange,8,0)
    ),
    List.of(
      Port.out("Link",ConnColor.Yellow_orange,19,1),
      Port.out("Note",ConnColor.Blue_red,19,7),
      Port.out("Trig",ConnColor.Yellow_orange,19,8)
    ),
    List.of(
      ModParam.FreqCoarse.mk("Seq1Step1"),
      ModParam.FreqCoarse.mk("Seq1Step2"),
      ModParam.FreqCoarse.mk("Seq1Step3"),
      ModParam.FreqCoarse.mk("Seq1Step4"),
      ModParam.FreqCoarse.mk("Seq1Step5"),
      ModParam.FreqCoarse.mk("Seq1Step6"),
      ModParam.FreqCoarse.mk("Seq1Step7"),
      ModParam.FreqCoarse.mk("Seq1Step8"),
      ModParam.FreqCoarse.mk("Seq1Step9"),
      ModParam.FreqCoarse.mk("Seq1Step10"),
      ModParam.FreqCoarse.mk("Seq1Step11"),
      ModParam.FreqCoarse.mk("Seq1Step12"),
      ModParam.FreqCoarse.mk("Seq1Step13"),
      ModParam.FreqCoarse.mk("Seq1Step14"),
      ModParam.FreqCoarse.mk("Seq1Step15"),
      ModParam.FreqCoarse.mk("Seq1Step16"),
      ModParam.OffOn.mk("Seq2Step1"),
      ModParam.OffOn.mk("Seq2Step2"),
      ModParam.OffOn.mk("Seq2Step3"),
      ModParam.OffOn.mk("Seq2Step4"),
      ModParam.OffOn.mk("Seq2Step5"),
      ModParam.OffOn.mk("Seq2Step6"),
      ModParam.OffOn.mk("Seq2Step7"),
      ModParam.OffOn.mk("Seq2Step8"),
      ModParam.OffOn.mk("Seq2Step9"),
      ModParam.OffOn.mk("Seq2Step10"),
      ModParam.OffOn.mk("Seq2Step11"),
      ModParam.OffOn.mk("Seq2Step12"),
      ModParam.OffOn.mk("Seq2Step13"),
      ModParam.OffOn.mk("Seq2Step14"),
      ModParam.OffOn.mk("Seq2Step15"),
      ModParam.OffOn.mk("Seq2Step16"),
      ModParam.LoopOnce.mk("Loop"),
      ModParam.SeqLen.mk("Length"),
      ModParam.TrigGate.mk("TG"),
      ModParam.OffOn.mk("Clr_Or_Rnd"),
      ModParam.OffOn.mk("Rnd_Or_Clr")
    ),
    List.of()
  ),
  M_Mix4_1C(123,4,"Mixer 4-1 C",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1",ConnColor.Blue_red,3,1),
      Port.in("In2",ConnColor.Blue_red,7,1),
      Port.in("In3",ConnColor.Blue_red,11,1),
      Port.in("In4",ConnColor.Blue_red,15,1),
      Port.in("Chain",ConnColor.Blue_red,19,0)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,3)
    ),
    List.of(
      ModParam.MixLevel.mk("Lev1"),
      ModParam.MixLevel.mk("Lev2"),
      ModParam.MixLevel.mk("Lev3"),
      ModParam.MixLevel.mk("Lev4"),
      ModParam.OffOn.mk("On1","Ch 1"),
      ModParam.OffOn.mk("On2","Ch 2"),
      ModParam.OffOn.mk("On3","Ch 3"),
      ModParam.OffOn.mk("On4","Ch 4"),
      ModParam.Pad_2.mk("Pad"),
      ModParam.ExpLin_2.mk("ExpLin")
    ),
    List.of()
  ),
  M_Mux8_1(124,2,"Multiplexer 8-1",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In1",ConnColor.Blue_red,4,1),
      Port.in("In2",ConnColor.Blue_red,7,1),
      Port.in("In3",ConnColor.Blue_red,9,1),
      Port.in("In4",ConnColor.Blue_red,11,1),
      Port.in("In5",ConnColor.Blue_red,12,1),
      Port.in("In6",ConnColor.Blue_red,14,1),
      Port.in("In7",ConnColor.Blue_red,15,1),
      Port.in("In8",ConnColor.Blue_red,17,1),
      Port.in("Ctrl",ConnColor.Blue,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_WahWah(125,2,"Wah-Wah",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,16,1),
      Port.in("Sweep",ConnColor.Blue,7,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("SweepMod"),
      ModParam.Level_100.mk("Sweep"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Name(126,1,"Name Bar",
    ModPage.InOut.ix(5),
    List.of(),
    List.of(),
    List.of(),
    List.of()
  ),
  M_Fx_In(127,2,"Fx Input",
    ModPage.InOut.ix(5),
    List.of(),
    List.of(
      Port.out("OutL",ConnColor.Red,16,1),
      Port.out("OutR",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.Source_1.mk("Source"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Pad_4.mk("Pad")
    ),
    List.of()
  ),
  M_MinMax(128,2,"Min/Max Compare",
    ModPage.Level.ix(5),
    List.of(
      Port.in("A",ConnColor.Blue_red,9,1),
      Port.in("B",ConnColor.Blue_red,11,1)
    ),
    List.of(
      Port.out("Min",ConnColor.Blue_red,17,1),
      Port.out("Max",ConnColor.Blue_red,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_BinCounter(130,2,"Binary Counter",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,0,1),
      Port.in("Rst",ConnColor.Yellow_orange,2,1)
    ),
    List.of(
      Port.out("Out001",ConnColor.Yellow_orange,5,1),
      Port.out("Out002",ConnColor.Yellow_orange,7,1),
      Port.out("Out004",ConnColor.Yellow_orange,9,1),
      Port.out("Out008",ConnColor.Yellow_orange,11,1),
      Port.out("Out016",ConnColor.Yellow_orange,13,1),
      Port.out("Out032",ConnColor.Yellow_orange,15,1),
      Port.out("Out064",ConnColor.Yellow_orange,17,1),
      Port.out("Out128",ConnColor.Yellow_orange,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_ADConv(131,2,"A/D Converter",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("D0",ConnColor.Yellow_orange,5,1),
      Port.out("D1",ConnColor.Yellow_orange,7,1),
      Port.out("D2",ConnColor.Yellow_orange,9,1),
      Port.out("D3",ConnColor.Yellow_orange,11,1),
      Port.out("D4",ConnColor.Yellow_orange,13,1),
      Port.out("D5",ConnColor.Yellow_orange,15,1),
      Port.out("D6",ConnColor.Yellow_orange,17,1),
      Port.out("D7",ConnColor.Yellow_orange,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_DAConv(132,2,"D/A Converter",
    ModPage.Logic.ix(5),
    List.of(
      Port.in("D0",ConnColor.Yellow_orange,5,1),
      Port.in("D1",ConnColor.Yellow_orange,7,1),
      Port.in("D2",ConnColor.Yellow_orange,9,1),
      Port.in("D3",ConnColor.Yellow_orange,11,1),
      Port.in("D4",ConnColor.Yellow_orange,12,1),
      Port.in("D5",ConnColor.Yellow_orange,13,1),
      Port.in("D6",ConnColor.Yellow_orange,15,1),
      Port.in("D7",ConnColor.Yellow_orange,17,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_FltHP(134,2,"Filter Highpass",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,16,1),
      Port.in("Pitch",ConnColor.Blue,4,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.FltFreq.mk("Freq"),
      ModParam.Level_100.mk("FreqMod"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of(
      ModParam.HpLpSlopeMode.mk("SlopeMode")
    )
  ),
  M_T_and_H(139,2,"Track & Hold",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,15,1),
      Port.in("Ctrl",ConnColor.Yellow_orange,12,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_Mix4_1S(140,4,"Mixer 4-1 Stereo",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1L",ConnColor.Blue_red,0,1),
      Port.in("In1R",ConnColor.Blue_red,1,1),
      Port.in("In2L",ConnColor.Blue_red,4,1),
      Port.in("In2R",ConnColor.Blue_red,5,1),
      Port.in("In3L",ConnColor.Blue_red,8,1),
      Port.in("In3R",ConnColor.Blue_red,9,1),
      Port.in("In4L",ConnColor.Blue_red,13,1),
      Port.in("In4R",ConnColor.Blue_red,14,1),
      Port.in("ChainL",ConnColor.Blue_red,17,0),
      Port.in("ChainR",ConnColor.Blue_red,19,0)
    ),
    List.of(
      Port.out("OutL",ConnColor.Blue_red,17,3),
      Port.out("OutR",ConnColor.Blue_red,19,3)
    ),
    List.of(
      ModParam.MixLevel.mk("Lev1"),
      ModParam.MixLevel.mk("Lev2"),
      ModParam.MixLevel.mk("Lev3"),
      ModParam.MixLevel.mk("Lev4"),
            ModParam.OffOn.mk("On1","Ch 1"),
            ModParam.OffOn.mk("On2","Ch 2"),
            ModParam.OffOn.mk("On3","Ch 3"),
            ModParam.OffOn.mk("On4","Ch 4"),
      ModParam.ExpLin_2.mk("ExpLin")
    ),
    List.of()
  ),
  M_CtrlSend(141,2,"MIDI Control Send",
    ModPage.MIDI.ix(5),
    List.of(
      Port.in("Send",ConnColor.Yellow,1,1),
      Port.in("Value",ConnColor.Blue,11,1)
    ),
    List.of(
      Port.out("Send",ConnColor.Yellow,4,1)
    ),
    List.of(
      ModParam.MidiData.mk("Ctrl"),
      ModParam.MidiData.mk("Val"),
      ModParam.MidiCh_20.mk("Ch")
    ),
    List.of()
  ),
  M_PCSend(142,2,"MIDI Program Change Send",
    ModPage.MIDI.ix(5),
    List.of(
      Port.in("Send",ConnColor.Yellow,1,1),
      Port.in("Program",ConnColor.Blue,11,1)
    ),
    List.of(
      Port.out("Send",ConnColor.Yellow,4,1)
    ),
    List.of(
      ModParam.MidiData.mk("Program"),
      ModParam.MidiCh_16.mk("Ch")
    ),
    List.of()
  ),
  M_NoteSend(143,2,"MIDI Note Send",
    ModPage.MIDI.ix(5),
    List.of(
      Port.in("Gate",ConnColor.Yellow,0,1),
      Port.in("Vel",ConnColor.Blue,4,1),
      Port.in("Note",ConnColor.Blue,10,1)
    ),
    List.of(),
    List.of(
      ModParam.MidiData.mk("Vel"),
      ModParam.MidiData.mk("Note"),
      ModParam.MidiCh_20.mk("Ch")
    ),
    List.of()
  ),
  M_SeqEvent(144,5,"Seq Event",
    ModPage.Seq.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,0,1),
      Port.in("Rst",ConnColor.Yellow,0,2),
      Port.in("Loop",ConnColor.Yellow,0,4),
      Port.in("Park",ConnColor.Yellow,16,0),
      Port.in("Trig1",ConnColor.Yellow,1,3),
      Port.in("Trig2",ConnColor.Yellow,1,4)
    ),
    List.of(
      Port.out("Link",ConnColor.Yellow,19,1),
      Port.out("Trig1",ConnColor.Yellow_orange,19,3),
      Port.out("Trig2",ConnColor.Yellow_orange,19,4)
    ),
    List.of(
      ModParam.OffOn.mk("Seq1Step1"),
      ModParam.OffOn.mk("Seq1Step2"),
      ModParam.OffOn.mk("Seq1Step3"),
      ModParam.OffOn.mk("Seq1Step4"),
      ModParam.OffOn.mk("Seq1Step5"),
      ModParam.OffOn.mk("Seq1Step6"),
      ModParam.OffOn.mk("Seq1Step7"),
      ModParam.OffOn.mk("Seq1Step8"),
      ModParam.OffOn.mk("Seq1Step9"),
      ModParam.OffOn.mk("Seq1Step10"),
      ModParam.OffOn.mk("Seq1Step11"),
      ModParam.OffOn.mk("Seq1Step12"),
      ModParam.OffOn.mk("Seq1Step13"),
      ModParam.OffOn.mk("Seq1Step14"),
      ModParam.OffOn.mk("Seq1Step15"),
      ModParam.OffOn.mk("Seq1Step16"),
      ModParam.OffOn.mk("Seq2Step1"),
      ModParam.OffOn.mk("Seq2Step2"),
      ModParam.OffOn.mk("Seq2Step3"),
      ModParam.OffOn.mk("Seq2Step4"),
      ModParam.OffOn.mk("Seq2Step5"),
      ModParam.OffOn.mk("Seq2Step6"),
      ModParam.OffOn.mk("Seq2Step7"),
      ModParam.OffOn.mk("Seq2Step8"),
      ModParam.OffOn.mk("Seq2Step9"),
      ModParam.OffOn.mk("Seq2Step10"),
      ModParam.OffOn.mk("Seq2Step11"),
      ModParam.OffOn.mk("Seq2Step12"),
      ModParam.OffOn.mk("Seq2Step13"),
      ModParam.OffOn.mk("Seq2Step14"),
      ModParam.OffOn.mk("Seq2Step15"),
      ModParam.OffOn.mk("Seq2Step16"),
      ModParam.LoopOnce.mk("Loop"),
      ModParam.SeqLen.mk("Length"),
      ModParam.TrigGate.mk("TG1"),
      ModParam.TrigGate.mk("TG2")
    ),
    List.of()
  ),
  M_SeqVal(145,8,"Sequencer Values",
    ModPage.Seq.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,0,1),
      Port.in("Rst",ConnColor.Yellow,0,3),
      Port.in("Loop",ConnColor.Yellow,0,4),
      Port.in("Park",ConnColor.Yellow,16,0),
      Port.in("Val",ConnColor.Blue_red,0,5),
      Port.in("Trig",ConnColor.Yellow,0,7)
    ),
    List.of(
      Port.out("Link",ConnColor.Yellow,19,1),
      Port.out("Val",ConnColor.Blue_red,19,5),
      Port.out("Trig",ConnColor.Yellow_orange,19,7)
    ),
    List.of(
      ModParam.LevBipUni.mk("Seq1Step1"),
      ModParam.LevBipUni.mk("Seq1Step2"),
      ModParam.LevBipUni.mk("Seq1Step3"),
      ModParam.LevBipUni.mk("Seq1Step4"),
      ModParam.LevBipUni.mk("Seq1Step5"),
      ModParam.LevBipUni.mk("Seq1Step6"),
      ModParam.LevBipUni.mk("Seq1Step7"),
      ModParam.LevBipUni.mk("Seq1Step8"),
      ModParam.LevBipUni.mk("Seq1Step9"),
      ModParam.LevBipUni.mk("Seq1Step10"),
      ModParam.LevBipUni.mk("Seq1Step11"),
      ModParam.LevBipUni.mk("Seq1Step12"),
      ModParam.LevBipUni.mk("Seq1Step13"),
      ModParam.LevBipUni.mk("Seq1Step14"),
      ModParam.LevBipUni.mk("Seq1Step15"),
      ModParam.LevBipUni.mk("Seq1Step16"),
      ModParam.OffOn.mk("Seq2Step1"),
      ModParam.OffOn.mk("Seq2Step2"),
      ModParam.OffOn.mk("Seq2Step3"),
      ModParam.OffOn.mk("Seq2Step4"),
      ModParam.OffOn.mk("Seq2Step5"),
      ModParam.OffOn.mk("Seq2Step6"),
      ModParam.OffOn.mk("Seq2Step7"),
      ModParam.OffOn.mk("Seq2Step8"),
      ModParam.OffOn.mk("Seq2Step9"),
      ModParam.OffOn.mk("Seq2Step10"),
      ModParam.OffOn.mk("Seq2Step11"),
      ModParam.OffOn.mk("Seq2Step12"),
      ModParam.OffOn.mk("Seq2Step13"),
      ModParam.OffOn.mk("Seq2Step14"),
      ModParam.OffOn.mk("Seq2Step15"),
      ModParam.OffOn.mk("Seq2Step16"),
      ModParam.LoopOnce.mk("Loop"),
      ModParam.SeqLen.mk("Length"),
      ModParam.BipUni.mk("BipUni"),
      ModParam.TrigGate.mk("TG"),
      ModParam.OffOn.mk("Clr_Or_Rnd"),
      ModParam.OffOn.mk("Rnd_Or_Clr")
    ),
    List.of()
  ),
  M_SeqLev(146,8,"Sequencer Level",
    ModPage.Seq.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,0,1),
      Port.in("Rst",ConnColor.Yellow_orange,0,3),
      Port.in("Loop",ConnColor.Yellow_orange,0,4),
      Port.in("Park",ConnColor.Yellow_orange,16,0),
      Port.in("Val",ConnColor.Blue_red,0,6),
      Port.in("Trig",ConnColor.Yellow_orange,0,7)
    ),
    List.of(
      Port.out("Link",ConnColor.Yellow_orange,19,1),
      Port.out("Val",ConnColor.Blue_red,19,6),
      Port.out("Trig",ConnColor.Yellow_orange,19,7)
    ),
    List.of(
      ModParam.LevBipUni.mk("Seq1Step1"),
      ModParam.LevBipUni.mk("Seq1Step2"),
      ModParam.LevBipUni.mk("Seq1Step3"),
      ModParam.LevBipUni.mk("Seq1Step4"),
      ModParam.LevBipUni.mk("Seq1Step5"),
      ModParam.LevBipUni.mk("Seq1Step6"),
      ModParam.LevBipUni.mk("Seq1Step7"),
      ModParam.LevBipUni.mk("Seq1Step8"),
      ModParam.LevBipUni.mk("Seq1Step9"),
      ModParam.LevBipUni.mk("Seq1Step10"),
      ModParam.LevBipUni.mk("Seq1Step11"),
      ModParam.LevBipUni.mk("Seq1Step12"),
      ModParam.LevBipUni.mk("Seq1Step13"),
      ModParam.LevBipUni.mk("Seq1Step14"),
      ModParam.LevBipUni.mk("Seq1Step15"),
      ModParam.LevBipUni.mk("Seq1Step16"),
      ModParam.OffOn.mk("Seq2Step1"),
      ModParam.OffOn.mk("Seq2Step2"),
      ModParam.OffOn.mk("Seq2Step3"),
      ModParam.OffOn.mk("Seq2Step4"),
      ModParam.OffOn.mk("Seq2Step5"),
      ModParam.OffOn.mk("Seq2Step6"),
      ModParam.OffOn.mk("Seq2Step7"),
      ModParam.OffOn.mk("Seq2Step8"),
      ModParam.OffOn.mk("Seq2Step9"),
      ModParam.OffOn.mk("Seq2Step10"),
      ModParam.OffOn.mk("Seq2Step11"),
      ModParam.OffOn.mk("Seq2Step12"),
      ModParam.OffOn.mk("Seq2Step13"),
      ModParam.OffOn.mk("Seq2Step14"),
      ModParam.OffOn.mk("Seq2Step15"),
      ModParam.OffOn.mk("Seq2Step16"),
      ModParam.LoopOnce.mk("Loop"),
      ModParam.SeqLen.mk("Length"),
      ModParam.BipUni.mk("BipUni"),
      ModParam.TrigGate.mk("TG"),
      ModParam.OffOn.mk("Clr_Or_Rnd"),
      ModParam.OffOn.mk("Rnd_Or_Clr")
    ),
    List.of()
  ),
  M_CtrlRcv(147,2,"MIDI Control Receive",
    ModPage.MIDI.ix(5),
    List.of(),
    List.of(
      Port.out("Rcv",ConnColor.Yellow,16,1),
      Port.out("Val",ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.MidiData.mk("Ctrl"),
      ModParam.MidiCh_16.mk("Ch")
    ),
    List.of()
  ),
  M_NoteRcv(148,2,"MIDI Note Receive",
    ModPage.MIDI.ix(5),
    List.of(),
    List.of(
      Port.out("Gate",ConnColor.Yellow,14,1),
      Port.out("Vel",ConnColor.Blue,16,1),
      Port.out("RelVel",ConnColor.Blue,18,1)
    ),
    List.of(
      ModParam.MidiData.mk("Note"),
      ModParam.MidiCh_17.mk("Ch")
    ),
    List.of()
  ),
  M_NoteZone(149,3,"MIDI Note Zone",
    ModPage.MIDI.ix(5),
    List.of(),
    List.of(),
    List.of(
      ModParam.MidiCh_17.mk("RcvCh"),
      ModParam.MidiData.mk("RcvMin"),
      ModParam.MidiData.mk("RcvMax"),
      ModParam.Bipolar_127.mk("SendTrans"),
      ModParam.MidiCh_20.mk("SendCh"),
      ModParam.NoteZoneThru.mk("ThruMode")
    ),
    List.of()
  ),
  M_Compressor(150,5,"Compressor",
    ModPage.FX.ix(5),
    List.of(
      Port.in("InL",ConnColor.Red,18,1),
      Port.in("InR",ConnColor.Red,19,1),
      Port.in("SideChain",ConnColor.Red,5,0)
    ),
    List.of(
      Port.out("OutR",ConnColor.Red,18,4),
      Port.out("OutL",ConnColor.Red,19,4)
    ),
    List.of(
      ModParam.Threshold_42.mk("Threshold"),
      ModParam.CompressorRatio.mk("Ratio"),
      ModParam.CompressorAttack.mk("Attack"),
      ModParam.CompressorRelease.mk("Release"),
      ModParam.CompressorRefLevel.mk("RefLevel"),
      ModParam.OffOn.mk("SideChain"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_KeyQuant(152,2,"Key Quantizer",
    ModPage.Note.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue,16,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.NoteRange.mk("Range"),
      ModParam.KeyQuantCapture.mk("Capture"),
      ModParam.OffOn.mk("E"),
      ModParam.OffOn.mk("F"),
      ModParam.OffOn.mk("F#"),
      ModParam.OffOn.mk("G"),
      ModParam.OffOn.mk("G#"),
      ModParam.OffOn.mk("A"),
      ModParam.OffOn.mk("A#"),
      ModParam.OffOn.mk("B"),
      ModParam.OffOn.mk("C"),
      ModParam.OffOn.mk("C#"),
      ModParam.OffOn.mk("D"),
      ModParam.OffOn.mk("D#")
    ),
    List.of()
  ),
  M_SeqCtr(154,8,"Sequencer Controlled",
    ModPage.Seq.ix(5),
    List.of(
      Port.in("Ctrl",ConnColor.Blue_red,0,1),
      Port.in("Val",ConnColor.Blue_red,0,5),
      Port.in("Trig",ConnColor.Yellow_orange,0,7)
    ),
    List.of(
      Port.out("Val",ConnColor.Blue_red,19,5),
      Port.out("Trig",ConnColor.Yellow_orange,19,7)
    ),
    List.of(
      ModParam.LevBipUni.mk("Seq1Step1"),
      ModParam.LevBipUni.mk("Seq1Step2"),
      ModParam.LevBipUni.mk("Seq1Step3"),
      ModParam.LevBipUni.mk("Seq1Step4"),
      ModParam.LevBipUni.mk("Seq1Step5"),
      ModParam.LevBipUni.mk("Seq1Step6"),
      ModParam.LevBipUni.mk("Seq1Step7"),
      ModParam.LevBipUni.mk("Seq1Step8"),
      ModParam.LevBipUni.mk("Seq1Step9"),
      ModParam.LevBipUni.mk("Seq1Step10"),
      ModParam.LevBipUni.mk("Seq1Step11"),
      ModParam.LevBipUni.mk("Seq1Step12"),
      ModParam.LevBipUni.mk("Seq1Step13"),
      ModParam.LevBipUni.mk("Seq1Step14"),
      ModParam.LevBipUni.mk("Seq1Step15"),
      ModParam.LevBipUni.mk("Seq1Step16"),
      ModParam.OffOn.mk("Seq2Step1"),
      ModParam.OffOn.mk("Seq2Step2"),
      ModParam.OffOn.mk("Seq2Step3"),
      ModParam.OffOn.mk("Seq2Step4"),
      ModParam.OffOn.mk("Seq2Step5"),
      ModParam.OffOn.mk("Seq2Step6"),
      ModParam.OffOn.mk("Seq2Step7"),
      ModParam.OffOn.mk("Seq2Step8"),
      ModParam.OffOn.mk("Seq2Step9"),
      ModParam.OffOn.mk("Seq2Step10"),
      ModParam.OffOn.mk("Seq2Step11"),
      ModParam.OffOn.mk("Seq2Step12"),
      ModParam.OffOn.mk("Seq2Step13"),
      ModParam.OffOn.mk("Seq2Step14"),
      ModParam.OffOn.mk("Seq2Step15"),
      ModParam.OffOn.mk("Seq2Step16"),
      ModParam.TrigGate.mk("TG"),
      ModParam.BipUni.mk("BipUni"),
      ModParam.SeqCtrlXFade.mk("XFade"),
      ModParam.OffOn.mk("Clr_Or_Rnd"),
      ModParam.OffOn.mk("Rnd_Or_Clr")
    ),
    List.of()
  ),
  M_NoteDet(156,2,"Note Detector",
    ModPage.InOut.ix(5),
    List.of(),
    List.of(
      Port.out("Gate",ConnColor.Yellow,13,1),
      Port.out("Vel",ConnColor.Blue,16,1),
      Port.out("RelVel",ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.FreqCoarse.mk("Note")
    ),
    List.of()
  ),
  M_LevConv(157,2,"Level Converter",
    ModPage.Level.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.PosNegInvBipInv.mk("OutputType"),
      ModParam.BipPosNeg.mk("InputType")
    ),
    List.of()
  ),
  M_Glide(158,2,"Glide",
    ModPage.Note.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,16,1),
      Port.in("On",ConnColor.Yellow_orange,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.GlideTime.mk("Time"),
      ModParam.OffOn.mk("Glide"),
      ModParam.LogLin.mk("Shape")
    ),
    List.of()
  ),
  M_CompSig(159,2,"Compare to Signal",
    ModPage.Level.ix(5),
    List.of(
      Port.in("A",ConnColor.Blue_red,9,1),
      Port.in("B",ConnColor.Blue_red,11,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Yellow_orange,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_ZeroCnt(160,2,"Zero Crossing Counter",
    ModPage.Note.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_MixFader(161,9,"Mixer 8-1 Fader",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1",ConnColor.Blue_red,1,1),
      Port.in("In2",ConnColor.Blue_red,2,1),
      Port.in("In3",ConnColor.Blue_red,5,1),
      Port.in("In4",ConnColor.Blue_red,7,1),
      Port.in("In5",ConnColor.Blue_red,10,1),
      Port.in("In6",ConnColor.Blue_red,12,1),
      Port.in("In7",ConnColor.Blue_red,14,1),
      Port.in("In8",ConnColor.Blue_red,17,1),
      Port.in("Chain",ConnColor.Blue_red,19,0)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,8)
    ),
    List.of(
      ModParam.MixLevel.mk("Lev1"),
      ModParam.MixLevel.mk("Lev2"),
      ModParam.MixLevel.mk("Lev3"),
      ModParam.MixLevel.mk("Lev4"),
      ModParam.MixLevel.mk("Lev5"),
      ModParam.MixLevel.mk("Lev6"),
      ModParam.MixLevel.mk("Lev7"),
      ModParam.MixLevel.mk("Lev8"),
            ModParam.OffOn.mk("On1","Ch 1"),
            ModParam.OffOn.mk("On2","Ch 2"),
            ModParam.OffOn.mk("On3","Ch 3"),
            ModParam.OffOn.mk("On4","Ch 4"),
            ModParam.OffOn.mk("On5","Ch 5"),
            ModParam.OffOn.mk("On6","Ch 6"),
            ModParam.OffOn.mk("On7","Ch 7"),
            ModParam.OffOn.mk("On8","Ch 8"),

      ModParam.ExpLin_2.mk("ExpLin"),
      ModParam.Pad_3.mk("Pad")
    ),
    List.of()
  ),
  M_FltComb(162,4,"Filter Comb",
    ModPage.Filter.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0),
      Port.in("Pitch",ConnColor.Blue_red,0,2),
      Port.in("PitchVar",ConnColor.Blue_red,0,3),
      Port.in("FB",ConnColor.Blue,9,3)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.Freq_1.mk("Freq"),
      ModParam.Level_100.mk("PitchMod"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.Bipolar_127.mk("FB"),
      ModParam.Level_100.mk("FBMod"),
      ModParam.CombType.mk("Type"),
      ModParam.Level_100.mk("Lev"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_OscShpA(163,5,"Osc Shape A",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Pitch",ConnColor.Blue_red,0,3),
      Port.in("PitchVar",ConnColor.Blue_red,0,4),
      Port.in("Sync",ConnColor.Red,0,1),
      Port.in("FmMod",ConnColor.Red,8,3),
      Port.in("ShapeMod",ConnColor.Red,14,3)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,4)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.Level_100.mk("PitchMod"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.Level_100.mk("FmAmount"),
      ModParam.FmLinTrk.mk("FmMode"),
      ModParam.PW.mk("Shape"),
      ModParam.Level_100.mk("ShapeMod"),
      ModParam.OscShpA_Waveform.mk("Waveform"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_OscDual(164,5,"Osc Dual",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Pitch",ConnColor.Blue_red,0,3),
      Port.in("PitchVar",ConnColor.Blue_red,0,4),
      Port.in("Sync",ConnColor.Red,0,1),
      Port.in("PW",ConnColor.Red,9,1),
      Port.in("Phase",ConnColor.Red,9,4)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,4)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.Level_100.mk("PitchMod"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.Level_100.mk("SqrLevel"),
      ModParam.Level_100.mk("PWMod"),
      ModParam.Level_100.mk("SawLevel"),
      ModParam.Phase.mk("SawPhase"),
      ModParam.Level_100.mk("SubOctLevel"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.PW.mk("SqrPW"),
      ModParam.Level_100.mk("PhaseMod"),
      ModParam.OffOn.mk("Soft")
    ),
    List.of()
  ),
  M_DXRouter(165,6,"DX Style Router",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("In1",ConnColor.Red,0,5),
      Port.in("In2",ConnColor.Red,2,5),
      Port.in("In3",ConnColor.Red,5,5),
      Port.in("In4",ConnColor.Red,9,5),
      Port.in("In5",ConnColor.Red,12,5),
      Port.in("In6",ConnColor.Red,15,5)
    ),
    List.of(
      Port.out("Out1",ConnColor.Red,1,5),
      Port.out("Out2",ConnColor.Red,4,5),
      Port.out("Out3",ConnColor.Red,7,5),
      Port.out("Out4",ConnColor.Red,10,5),
      Port.out("Out5",ConnColor.Red,13,5),
      Port.out("Out6",ConnColor.Red,16,5),
      Port.out("Main",ConnColor.Red,19,5)
    ),
    List.of(
      ModParam.DxAlgorithm.mk("Algorithm"),
      ModParam.DxFeedback.mk("Feedback")
    ),
    List.of()
  ),
  M_PShift(167,3,"Pitch Shifter",
    ModPage.FX.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0),
      Port.in("Pitch",ConnColor.Blue,1,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.PShiftCoarse.mk("ShiftSemi"),
      ModParam.PShiftFine.mk("ShiftFine"),
      ModParam.Level_100.mk("ShiftMod"),
      ModParam.ScratchDelay.mk("Delay"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_ModAHD(169,5,"Envelope Modulation AHD",
    ModPage.Env.ix(5),
    List.of(
      Port.in("Trig",ConnColor.Yellow,0,2),
      Port.in("AttackMod",ConnColor.Blue,4,4),
      Port.in("HoldMod",ConnColor.Blue,8,4),
      Port.in("DecayMod",ConnColor.Blue,12,4),
      Port.in("In",ConnColor.Blue_red,19,0),
      Port.in("AM",ConnColor.Blue,0,4)
    ),
    List.of(
      Port.out("Env",ConnColor.Blue,18,4),
      Port.out("Out",ConnColor.Blue_red,19,4)
    ),
    List.of(
      ModParam.EnvTime.mk("Attack"),
      ModParam.EnvTime.mk("Hold"),
      ModParam.EnvTime.mk("Decay"),
      ModParam.Level_100.mk("AttackMod"),
      ModParam.Level_100.mk("HoldMod"),
      ModParam.Level_100.mk("DecayMod"),
      ModParam.PosNegInv.mk("OutputType"),
      ModParam.OffOn.mk("KB")
    ),
    List.of()
  ),
  M_2_In(170,2,"2 inputs",
    ModPage.InOut.ix(5),
    List.of(),
    List.of(
      Port.out("OutL",ConnColor.Red,17,1),
      Port.out("OutR",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.Source_2.mk("Source"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Pad_4.mk("Pad")
    ),
    List.of()
  ),
  M_4_In(171,2,"4 inputs",
    ModPage.InOut.ix(5),
    List.of(),
    List.of(
      Port.out("Out1",ConnColor.Red,12,1),
      Port.out("Out2",ConnColor.Red,14,1),
      Port.out("Out3",ConnColor.Red,17,1),
      Port.out("Out4",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.Source_3.mk("Source"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Pad_4.mk("Pad")
    ),
    List.of()
  ),
  M_DlySingleA(172,2,"Delay Static",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,16,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.DelayTime_3.mk("Time")
    ),
    List.of(
      ModParam.DelayRange_3.mk("DelayRange")
    )
  ),
  M_DlySingleB(173,2,"Delay Single",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,16,1),
      Port.in("Time",ConnColor.Red,7,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.DelayTime_3.mk("Time"),
      ModParam.Level_100.mk("TimeMod")
    ),
    List.of(
      ModParam.DelayRange_3.mk("DelayRange")
    )
  ),
  M_DelayDual(174,3,"Delay Dual",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0),
      Port.in("Time1",ConnColor.Red,7,2),
      Port.in("Time2",ConnColor.Red,13,2)
    ),
    List.of(
      Port.out("Out1",ConnColor.Red,10,2),
      Port.out("Out2",ConnColor.Red,17,2)
    ),
    List.of(
      ModParam.DelayTime_3.mk("Time1"),
      ModParam.Level_100.mk("Time1Mod"),
      ModParam.DelayTime_3.mk("Time2"),
      ModParam.Level_100.mk("Time2Mod")
    ),
    List.of(
      ModParam.DelayRange_3.mk("DelayRange")
    )
  ),
  M_DelayQuad(175,5,"Delay Quad",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0),
      Port.in("Time1",ConnColor.Red,3,4),
      Port.in("Time2",ConnColor.Red,7,4),
      Port.in("Time3",ConnColor.Red,11,4),
      Port.in("Time4",ConnColor.Red,15,4)
    ),
    List.of(
      Port.out("OutMain",ConnColor.Red,19,4),
      Port.out("Out1",ConnColor.Red,5,2),
      Port.out("Out2",ConnColor.Red,9,2),
      Port.out("Out3",ConnColor.Red,13,2),
      Port.out("Out4",ConnColor.Red,17,2)
    ),
    List.of(
      ModParam.DelayTime_3.mk("Time1"),
      ModParam.Level_100.mk("Time1Mod"),
      ModParam.DelayTime_3.mk("Time2"),
      ModParam.Level_100.mk("Time2Mod"),
      ModParam.DelayTime_3.mk("Time3"),
      ModParam.Level_100.mk("Time3Mod"),
      ModParam.DelayTime_3.mk("Time4"),
      ModParam.Level_100.mk("Time4Mod"),
      ModParam.TimeClk.mk("TimeClk")
    ),
    List.of(
      ModParam.DelayRange_3.mk("DelayRange")
    )
  ),
  M_DelayA(176,3,"Delay A",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.DelayTime_2.mk("Time"),
      ModParam.Level_100.mk("FB"),
      ModParam.Level_100.mk("Filter"),
      ModParam.Level_100.mk("DryWet"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.TimeClk.mk("TimeClk")
    ),
    List.of(
      ModParam.DelayRange_2.mk("DelayRange")
    )
  ),
  M_DelayB(177,4,"Delay B",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0),
      Port.in("FBMod",ConnColor.Blue_red,9,3),
      Port.in("DryWetMod",ConnColor.Blue_red,15,3)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,3)
    ),
    List.of(
      ModParam.DelayTime_2.mk("Time"),
      ModParam.Level_100.mk("FB"),
      ModParam.Level_100.mk("LP"),
      ModParam.Level_100.mk("DryWet"),
      ModParam.TimeClk.mk("TimeClk"),
      ModParam.Level_100.mk("FBMod"),
      ModParam.Level_100.mk("DryWetMod"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Level_100.mk("HP")
    ),
    List.of(
      ModParam.DelayRange_2.mk("DelayRange")
    )
  ),
  M_DlyClock(178,2,"Delay Clocked",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,16,1),
      Port.in("Clk",ConnColor.Yellow_orange,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Range_128.mk("Delay")
    ),
    List.of()
  ),
  M_DlyShiftReg(179,2,"Shift Register",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,5,1),
      Port.in("Clk",ConnColor.Yellow_orange,0,1)
    ),
    List.of(
      Port.out("Out1",ConnColor.Blue_red,7,1),
      Port.out("Out2",ConnColor.Blue_red,9,1),
      Port.out("Out3",ConnColor.Blue_red,11,1),
      Port.out("Out4",ConnColor.Blue_red,12,1),
      Port.out("Out5",ConnColor.Blue_red,14,1),
      Port.out("Out6",ConnColor.Blue_red,15,1),
      Port.out("Out7",ConnColor.Blue_red,17,1),
      Port.out("Out8",ConnColor.Blue_red,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_Operator(180,12,"FM Operator",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Freq",ConnColor.Blue,0,3),
      Port.in("FM",ConnColor.Red,19,1),
      Port.in("Gate",ConnColor.Yellow,0,5),
      Port.in("Note",ConnColor.Blue,0,6),
      Port.in("AMod",ConnColor.Blue,0,9),
      Port.in("Vel",ConnColor.Blue,0,11),
      Port.in("Pitch",ConnColor.Blue,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,11)
    ),
    List.of(
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.OffOn.mk("Sync"),
      ModParam.RatioFixed.mk("RatioFixed"),
      ModParam.OpFreqCoarse.mk("FreqCoarse"),
      ModParam.OpFreqFine.mk("FreqFine"),
      ModParam.OpFreqDetune.mk("FreqDetune"),
      ModParam.OpVel.mk("Vel"),
      ModParam.OpRateScale.mk("RateScale"),
      ModParam.OpTime.mk("R1"),
      ModParam.OpLevel.mk("L1"),
      ModParam.OpTime.mk("R2"),
      ModParam.OpLevel.mk("L2"),
      ModParam.OpTime.mk("R3"),
      ModParam.OpLevel.mk("L3"),
      ModParam.OpTime.mk("R4"),
      ModParam.OpLevel.mk("L4"),
      ModParam.OpAmod.mk("AMod"),
      ModParam.OpBrPpoint.mk("BrPoint"),
      ModParam.OpDepthMode.mk("LDepthMode"),
      ModParam.OpDepth.mk("LDepth"),
      ModParam.OpDepthMode.mk("RDepthMode"),
      ModParam.OpDepth.mk("RDepth"),
      ModParam.OpLevel.mk("OutLevel"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.OffOn.mk("EnvKB")
    ),
    List.of()
  ),
  M_DlyEight(181,3,"Delay 8 Tap",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0)
    ),
    List.of(
      Port.out("Out1",ConnColor.Red,7,2),
      Port.out("Out2",ConnColor.Red,9,2),
      Port.out("Out3",ConnColor.Red,10,2),
      Port.out("Out4",ConnColor.Red,12,2),
      Port.out("Out5",ConnColor.Red,14,2),
      Port.out("Out6",ConnColor.Red,16,2),
      Port.out("Out7",ConnColor.Red,17,2),
      Port.out("Out8",ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.DelayTime_3.mk("Time")
    ),
    List.of(
      ModParam.DelayRange_3.mk("DelayRange")
    )
  ),
  M_DlyStereo(182,5,"Delay Stereo",
    ModPage.Delay.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,19,0)
    ),
    List.of(
      Port.out("OutL",ConnColor.Red,17,4),
      Port.out("OutR",ConnColor.Red,19,4)
    ),
    List.of(
      ModParam.DelayTime_1.mk("TimeLeft"),
      ModParam.DelayTime_1.mk("TimeRight"),
      ModParam.Level_100.mk("FBLeft"),
      ModParam.Level_100.mk("FBRight"),
      ModParam.Level_100.mk("XFBLeft"),
      ModParam.Level_100.mk("XFBRight"),
      ModParam.TimeClk.mk("TimeClk"),
      ModParam.Level_100.mk("LP"),
      ModParam.Level_100.mk("DryWet"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Level_100.mk("HP")
    ),
    List.of(
      ModParam.DelayRange_1.mk("DelayRange")
    )
  ),
  M_OscPM(183,3,"Osc Phase Mod",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("PitchVar",ConnColor.Blue_red,0,2),
      Port.in("Sync",ConnColor.Red,12,2),
      Port.in("PhaseMod",ConnColor.Red,14,2),
      Port.in("Pitch",ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,2)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.Level_100.mk("PhaseMod"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.Level_100.mk("PitchVar")
    ),
    List.of(
      ModParam.OscWaveform_1.mk("Waveform")
    )
  ),
  M_Mix1_1A(184,2,"Mixer 1-1 A",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,16,1),
      Port.in("Chain",ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.MixLevel.mk("Lev"),
            ModParam.OffOn.mk("On","Ch 1"), // TODO check
      ModParam.ExpLin_2.mk("ExpLin")
    ),
    List.of()
  ),
  M_Mix1_1S(185,2,"Mixer 1-1 Stereo",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("InL",ConnColor.Blue_red,14,1),
      Port.in("InR",ConnColor.Blue_red,16,1),
      Port.in("LChain",ConnColor.Blue_red,0,1),
      Port.in("RChain",ConnColor.Blue_red,1,1)
    ),
    List.of(
      Port.out("OutL",ConnColor.Blue_red,17,1),
      Port.out("OutR",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.MixLevel.mk("Lev"),
            ModParam.OffOn.mk("On","Ch 2"), // TODO check
      ModParam.ExpLin_2.mk("ExpLin")
    ),
    List.of()
  ),
  M_Sw1_2M(186,2,"Switch 1-2 Momentary",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,14,1)
    ),
    List.of(
      Port.out("OutOn",ConnColor.Blue_red,19,1),
      Port.out("OutOff",ConnColor.Blue_red,16,1),
      Port.out("Ctrl",ConnColor.Blue,0,1)
    ),
    List.of(
      ModParam.OffOn.mk("Sel","Switch")
    ),
    List.of()
  ),
  M_Sw2_1M(187,2,"Switch 2-1 Momentary",
    ModPage.Switch.ix(5),
    List.of(
      Port.in("InOff",ConnColor.Blue_red,14,1),
      Port.in("InOn",ConnColor.Blue_red,16,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1),
      Port.out("Ctrl",ConnColor.Blue,0,1)
    ),
    List.of(
            ModParam.OffOn.mk("Sel","Switch")
    ),
    List.of()
  ),
  M_ConstSwM(188,2,"Constant Switch Momentary",
    ModPage.Level.ix(5),
    List.of(),
    List.of(
      Port.out("Out",ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.LevBipUni.mk("Lev"),
            ModParam.OffOn.mk("On","Switch"),
      ModParam.BipUni.mk("BipUni")
    ),
    List.of()
  ),
  M_NoiseGate(189,3,"Noise Gate",
    ModPage.Level.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,19,0)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,2),
      Port.out("Env",ConnColor.Blue,17,2)
    ),
    List.of(
      ModParam.Threshold_127.mk("Threshold"),
      ModParam.NoiseGateAttack.mk("Attack"),
      ModParam.NoiseGateRelease.mk("Release"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_LfoB(190,4,"LFO B",
    ModPage.LFO.ix(5),
    List.of(
      Port.in("Rate",ConnColor.Blue,1,3),
      Port.in("RateVar",ConnColor.Blue,3,3),
      Port.in("Rst",ConnColor.Blue,0,1),
      Port.in("Phase",ConnColor.Blue,10,3)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue,19,3),
      Port.out("Sync",ConnColor.Blue,0,3)
    ),
    List.of(
      ModParam.LfoRate_4.mk("Rate"),
      ModParam.Level_100.mk("RateMod"),
      ModParam.LfoRange_4.mk("Range"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.LfoB_Waveform.mk("Waveform"),
      ModParam.PolyMono.mk("PolyMono"),
      ModParam.Phase.mk("Phase"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.OutTypeLfo.mk("OutputType"),
      ModParam.Level_100.mk("PhaseMod")
    ),
    List.of()
  ),
  M_Phaser(192,2,"Phaser",
    ModPage.FX.ix(5),
    List.of(
      Port.in("In",ConnColor.Red,16,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,1)
    ),
    List.of(
      ModParam.PhaserType.mk("Type"),
      ModParam.PhaserFreq.mk("Freq"),
      ModParam.Level_100.mk("FeedBack"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Mix4_1A(193,2,"Mixer 4-1 A",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1",ConnColor.Blue_red,5,1),
      Port.in("In2",ConnColor.Blue_red,8,1),
      Port.in("In3",ConnColor.Blue_red,11,1),
      Port.in("In4",ConnColor.Blue_red,15,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(),
    List.of()
  ),
  M_Mix2_1A(194,2,"Mixer 2-1 A",
    ModPage.Mixer.ix(5),
    List.of(
      Port.in("In1",ConnColor.Blue_red,9,1),
      Port.in("In2",ConnColor.Blue_red,16,1),
      Port.in("Chain",ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.MixLevel.mk("Lev1"),
      ModParam.OffOn.mk("On1","Ch 1"),
      ModParam.MixLevel.mk("Lev2"),
      ModParam.OffOn.mk("On2","Ch 2"),
      ModParam.ExpLin_2.mk("ExpLin")
    ),
    List.of()
  ),
  M_ModAmt(195,2,"Modulation Amount",
    ModPage.Level.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,16,1),
      Port.in("Mod",ConnColor.Blue_red,15,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("ModDepth"),
      ModParam.OffOn.mk("On","On"),
      ModParam.ExpLin_1.mk("ExpLin"),
      ModParam.ModAmtInvert.mk("InvertMode")
    ),
    List.of()
  ),
  M_OscPerc(196,3,"Osc Percussion",
    ModPage.Osc.ix(5),
    List.of(
      Port.in("Pitch",ConnColor.Blue_red,0,1),
      Port.in("PitchVar",ConnColor.Blue_red,0,2),
      Port.in("Trig",ConnColor.Red,3,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Red,19,3)
    ),
    List.of(
      ModParam.FreqCoarse.mk("FreqCoarse"),
      ModParam.FreqFine.mk("FreqFine"),
      ModParam.FreqMode_3.mk("FreqMode"),
      ModParam.Kbt_1.mk("Kbt"),
      ModParam.Level_100.mk("PitchMod"),
      ModParam.Level_100.mk("Decay"),
      ModParam.Level_100.mk("Click"),
      ModParam.OffOn.mk("Punch"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_Status(197,2,"Status",
    ModPage.InOut.ix(5),
    List.of(),
    List.of(
      Port.out("PatchActive",ConnColor.Yellow,8,1),
      Port.out("VarActive",ConnColor.Yellow,13,1),
      Port.out("VoiceNo",ConnColor.Blue,18,1)
    ),
    List.of(),
    List.of()
  ),
  M_PitchTrack(198,2,"Pitch Tracker",
    ModPage.Note.ix(5),
    List.of(
      Port.in("In",ConnColor.Blue_red,0,1)
    ),
    List.of(
      Port.out("Period",ConnColor.Yellow_orange,13,1),
      Port.out("Gate",ConnColor.Yellow_orange,15,1),
      Port.out("Pitch",ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.Threshold_127.mk("Threshold")
    ),
    List.of()
  ),
  M_MonoKey(199,2,"Monophonic Keyboard",
    ModPage.InOut.ix(5),
    List.of(),
    List.of(
      Port.out("Pitch",ConnColor.Blue,14,1),
      Port.out("Gate",ConnColor.Yellow,16,1),
      Port.out("Vel",ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.MonoKeyMode.mk("Mode")
    ),
    List.of()
  ),
  M_RandomA(200,2,"Random A",
    ModPage.Rnd.ix(5),
    List.of(
      Port.in("Rate",ConnColor.Blue,0,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue,19,1)
    ),
    List.of(
      ModParam.LfoRate_3.mk("Rate"),
      ModParam.PolyMono.mk("PolyMono"),
      ModParam.BipPosNeg.mk("OutputType"),
      ModParam.LfoRange_3.mk("Range"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.RndEdge.mk("Edge"),
      ModParam.RandomAStepProb.mk("StepProb")
    ),
    List.of()
  ),
  M_RandomB(202,3,"Random B",
    ModPage.Rnd.ix(5),
    List.of(
      Port.in("Rate",ConnColor.Blue,0,1),
      Port.in("RateVar",ConnColor.Blue,0,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue,19,2)
    ),
    List.of(
      ModParam.LfoRate_3.mk("Rate"),
      ModParam.PolyMono.mk("PolyMono"),
      ModParam.Kbt_4.mk("Kbt"),
      ModParam.Level_100.mk("RateMod"),
      ModParam.Level_100.mk("StepProb"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.BipPosNeg.mk("OutputType"),
      ModParam.LfoRange_3.mk("Range"),
      ModParam.RndEdge.mk("Edge")
    ),
    List.of()
  ),
  M_RndClkA(204,2,"Random Clock A",
    ModPage.Rnd.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,0,1),
      Port.in("Rst",ConnColor.Yellow_orange,4,1),
      Port.in("Seed",ConnColor.Blue_red,7,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("StepProb"),
      ModParam.PolyMono.mk("PolyMono"),
      ModParam.OffOn.mk("Dice"),
      ModParam.BipPosNeg.mk("OutputType"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of()
  ),
  M_RndTrig(205,2,"Random Trig",
    ModPage.Rnd.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,0,1),
      Port.in("Rst",ConnColor.Yellow_orange,4,1),
      Port.in("Seed",ConnColor.Blue_red,7,1),
      Port.in("Prob",ConnColor.Blue_red,8,1)
    ),
    List.of(
      Port.out("Out",ConnColor.Yellow_orange,19,1)
    ),
    List.of(
      ModParam.Level_100.mk("StepProb"),
      ModParam.Level_100.mk("StepProbMod"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.PolyMono.mk("PolyMono")
    ),
    List.of()
  ),
  M_RndClkB(206,3,"Random Clock B",
    ModPage.Rnd.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,0,1),
      Port.in("Rst",ConnColor.Yellow_orange,0,2),
      Port.in("Seed",ConnColor.Blue_red,3,2),
      Port.in("Step",ConnColor.Blue,11,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,2)
    ),
    List.of(
      ModParam.Level_100.mk("StepProb"),
      ModParam.BipPosNeg.mk("OutputType"),
      ModParam.ActiveMonitor.mk("Active"),
      ModParam.PolyMono.mk("PolyMono"),
      ModParam.Level_100.mk("StepProbMod")
    ),
    List.of(
      ModParam.Rnd_1.mk("Character")
    )
  ),
  M_RndPattern(208,3,"Random Pattern",
    ModPage.Rnd.ix(5),
    List.of(
      Port.in("Clk",ConnColor.Yellow_orange,0,1),
      Port.in("Rst",ConnColor.Yellow_orange,0,2),
      Port.in("A",ConnColor.Blue,3,1),
      Port.in("B",ConnColor.Blue,3,2),
      Port.in("StepProb",ConnColor.Blue,9,2)
    ),
    List.of(
      Port.out("Out",ConnColor.Blue_red,19,2)
    ),
    List.of(
      ModParam.RangeBip_128.mk("PatternA"),
      ModParam.Level_100.mk("PatternB"),
      ModParam.RangeBip_128.mk("StepProb"),
      ModParam.Range_128.mk("LoopCount"),
      ModParam.Level_100.mk("StepProbMod"),
      ModParam.BipPosNeg.mk("OutputType"),
      ModParam.ActiveMonitor.mk("Active")
    ),
    List.of(
      ModParam.RndStepPulse.mk("Waveform")
    )
  );


  public final int ix;
  public final int height;
  public final String longName;
  public final ModPageIx modPageIx;
  public final List<Port> inPorts;
  public final List<Port> outPorts;
  public final List<ModParam.NamedParam> params;
  public final List<ModParam.NamedParam> modes;
  public final String shortName;

  ModuleType(int ix, int height, String longName, ModPageIx modPageIx, List<Port> inPorts,
             List<Port> outPorts, List<ModParam.NamedParam> params, List<ModParam.NamedParam> modes) {

    this.ix = ix;
    this.height = height;
    this.longName = longName;
    this.modPageIx = modPageIx;
    this.inPorts = inPorts;
    this.outPorts = outPorts;
    this.params = params;
    this.modes = modes;
    this.shortName = mkShortName(name());
  }

  private static String mkShortName(String name) {
    return name.substring(2).replace('_','-').replace("-and-","&");
  }

  public record ModPageIx(ModPage page,Integer ix) { }

  public enum ModPage {
    Rnd,
    InOut,
    Logic,
    Osc,
    FX,
    Switch,
    Mixer,
    Env,
    Level,
    Filter,
    MIDI,
    Shaper,
    LFO,
    Note,
    Seq,
    Delay;

    public ModPageIx ix(int i) {
      return new ModPageIx(this,i);
    }
  }

}
