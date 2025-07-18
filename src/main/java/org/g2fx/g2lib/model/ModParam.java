package org.g2fx.g2lib.model;

import java.util.List;

public enum ModParam {

    Dst_2
    (0,
     "Out", "Fx", "Bus"),
    OffOn
    (0,
     "Off", "On"),
    Pad_1
    (0,
     "0 dB", "+6 dB"),
    Dst_1
    (0,
     "Out 1/2", "Out 3/4", "Fx 1/2", "Fx 3/4", "Bus 1/2", "Bus 3/4"),
    FreqCoarse
    (0,127,64),
    FreqFine
    (0,127,64),
    Level_100
    (0,127,0),
    FreqMode_3
    (0,
     "Semi", "Freq", "Fac", "Part"),
    PW
    (0,127,0),
    OscBWaveform
    (0,
     "Sine", "Tri", "Saw", "Pulse", "DualSaw"),
    FmLinTrk
    (0,
     "Lin", "Trk"),
    OscWaveform_3
    (0,
     "Sine1", "Sine2", "Sine3", "Sine4", "TriSaw", "DoubleSaw", "Pulse", "SymPulse"),
    OscWaveform_2
    (0,
     "Sine", "Tri", "Saw", "Square", "Pulse 25%", "Pulse 10%"),
    ReverbTime
    (0,127,0),
    RoomType
    (0,
     "Small", "Medium", "Large", "Hall"),
    Sw_3
    (0,
     "sw1", "sw2", "sw3", "sw4", "sw5", "sw6", "sw7", "sw8"),
    ValSwVal
    (0,63,0),
    Bipolar_127
    (0,127,0),
    LogLin
    (0,
     "Log", "Lin"),
    MixLevel
    (0,127,0),
    ExpLin_2
    (0,
     "Exp", "Lin", "dB"),
    EnvShape_3
    (0,
     "LogExp", "LinExp", "ExpExp", "LinLin"),
    EnvTime
    (0,127,0),
    EnvLevel
    (0,127,0),
    PosNegInvBipInv
    (0,
     "Pos", "PosInv", "Neg", "NegInv", "Bip", "BipInv"),
    EnvNR
    (0,
     "Normal", "Reset"),
    PartialRange
    (0,127,0),
    LfoRate_3
    (0,127,1),
    PolyMono
    (0,
     "Poly", "Mono"),
    OutTypeLfo
    (4,
     "Pos", "PosInv", "Neg", "NegInv", "Bip", "BipInv"),
    LfoRange_3
    (1,
     "Rate Sub", "Rate Lo", "Rate Hi", "BPM"),
    LfoWaveform_1
    (0,
     "Sine", "Tri", "Saw", "Square", "RndStep", "Rnd", "RndPulse", "RndRoundedPulse"),
    LfoRate_4
    (0,127,1),
    LfoRange_4
    (0,
     "Rate Sub", "Rate Lo", "Rate Hi", "BPM", "Clock"),
    Kbt_1
    (1, // TODO was 4 max in py
     "Off", "On"),
    Kbt_4
    (0,
     "Off", "25%", "50%", "75%", "100%"),
    LfoShpAPW
    (0,127,0),
    Phase
    (0,127,0),
    LfoShpA__Waveform
    (0,
     "Sine", "CosBell", "TriBell", "Saw2Tri", "Sqr2Tri", "Sqr"),
    LfoA_Waveform
    (0,
     "Sine", "Tri", "Saw", "Aqr", "RndStep", "Rnd"),
    FreqMode_2
    (0,
     "Semi", "Freq", "Fac"),
    SaturateCurve
    (0,
     "1", "2", "3", "4"),
    NoiseColor
    (0,127,0),
    EqdB
    (0,127,64),
    EqLoFreq
    (0,
     "80 Hz", "110 Hz", "160 Hz"),
    EqHiFreq
    (0,
     "6 kHz", "8 kHz", "12 kHz"),
    EqMidFreq
    (0,127,93),
    ShpExpCurve
    (0,
     "x2", "x3", "x4", "x5"),
    LogicTime
    (0,127,1),
    LogicRange
    (0,
     "Sub", "Lo", "Hi"),
    PulseMode
    (0,
     "Positive edge trigger", "Negative edge trigger"),
    Pad_3
    (0,
     "0 dB", "-6 dB", "-12 dB"),
    PosNegInv
    (0,
     "Pos", "PosInv", "Neg", "NegInv"),
    LogicDelayMode
    (0,
     "Positive edge delay", "Negative edge delay", "Cycle delay"),
    LevBipUni
    (0,127,0),
    BipUni
    (0,
     "Bip", "Uni"),
    Vowel
    (0,
     "A", "E", "I", "O", "U", "Y", "AA", "AE", "OE"),
    FltFreq
    (0,127,75),
    Level_200
    (0,127,0),
    GcOffOn
    (0,
     "GC Off", "GC On"),
    Res_1
    (0,127,0),
    FltSlope_1
    (1,
     "6 dB/Oct", "12 dB/Oct"),
    FltSlope_2
    (0,
     "12 dB/Oct", "24 dB/Oct"),
    LpBpHpBr
    (0,
     "LP", "BP", "HP", "BR"),
    SustainMode_2
    (2,
     "L1", "L2", "L3", "Trg"),
    PosNegInvBip
    (0,
     "Pos", "PosInv", "Neg", "NegInv", "Bip"),
    LpBpHp
    (0,
     "LP", "BP", "HP"),
    MidiData
    (0,127,0),
    MidiCh_20
    (0,
     "ch1", "ch 2", "ch3", "ch4", "ch5", "ch6", "ch7", "ch8",
     "ch9", "ch10","ch11", "ch12", "ch13", "ch14", "ch15", "ch16",
     "This", "Slot A", "Slot B", "Slot C", "Slot D"),
    DrumSynthFreq
    (0,127,42),
    DrumSynthRatio
    (0,127,15),
    DrumSynthNoiseFlt
    (0,127,57),
    ClipShape
    (0,
     "Asym", "Sym"),
    OverdriveType
    (0,
     "Soft", "Hard", "Fat", "Heavy"),
    ScratchRatio
    (0,127,80),
    ScratchDelay
    (2,
     "12.5m", "25m", "50m", "100m"),
    GateMode
    (0,
     "AND", "NAND", "OR", "NOR", "XOR", "XNOR"),
    MixInvert
    (0,
     "Normal", "Inverted"),
    RateBpm
    (0,127,64),
    InternalMaster
    (0,
     "Internal", "Master"),
    ClkGenBeatSync
    (2,
     "1", "2", "4", "8", "16", "32"),
    ClkGenSwing
    (0,127,0),
    Range_128
    (0,127,0),
    ClkDivMode
    (0,
     "Gated", "Toggled"),
    EnvFollowAttack
    (0,127,0),
    EnvFollowRelease
    (0,127,20),
    NoteRange
    (0,127,0),
    NoteQuantNotes
    (0,127,0),
    Sw_2
    (0,
     "sw1", "sw2", "sw3", "sw4"),
    LevAmpGain
    (0,127,64),
    LinDB
    (0,
     "Lin", "dB"),
    RectMode
    (0,
     "Half wave positive", "Half wave negative", "Full wave positive", "Full wave negative"),
    ShpStaticMode
    (1,
     "Inv x3", "Inv x2", "x2", "x3"),
    TrigGate
    (0,
     "Trig", "Gate"),
    AdAr
    (0,
     "AD", "AR"),
    Range_64
    (0,127,0),
    HpLpSlopeMode
    (0,
     "6dB/Oct", "12 dB/Oct", "18 dB/Oct", "24 dB/Oct", "30 dB/Oct", "36 dB/Oct"),
    FlangerRate
    (0,127,64),
    Sw_1
    (0,
     "sw1", "sw2"),
    FlipFlopMode
    (0,
     "D type", "SR type"),
    ClassicSlope
    (0,
     "12 dB/Oct", "18 dB/Oct", "24 dB/Oct"),
    OscA_Waveform
    (2,
     "Sine", "Tri", "Saw", "Square", "Pulse 25%", "Pulse 10%"),
    FreqShiftFreq
    (0,127,0),
    FreqShiftRange
    (0,
     "Sub", "Lo", "Hi"),
    Freq_2
    (0,127,64),
    FltPhaseNotchCount
    (2,
     "1", "2", "3", "4", "5", "6"),
    FltPhaseType
    (0,
     "Notch", "Peak", "Deep"),
    Freq_3
    (0,127,60),
    EqPeakBandwidth
    (0,127,64),
    VocoderBand
    (0,
     "Off", "1", "2", "3", "4", "5", "6", "7", "8",
     "9", "10", "11", "12", "13", "14", "15", "16"),
    ActiveMonitor
    (1,
     "Monitor","Active"),
    Fade12Mix
    (0,127,64),
    Fade21Mix
    (0,127,64),
    LevScaledB
    (0,127,64),
    LevModAmRm
    (0,127,64),
    DigitizerBits
    (11,
     "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "Off"),
    DigitizerRate
    (0,127,64),
    SustainMode_1
    (1,
     "L1", "L2"),
    LoopOnce
    (1,
     "Once", "Loop"),
    SeqLen
    (0,
     "1", "2", "3", "4", "5", "6", "7", "8",
     "9", "10", "11", "12", "13", "14", "15", "16"),
    Pad_2
    (0,
     "0 dB", "-6 dB"),
    Source_1
    (0,
     "FX 1/2", "FX 3/4"),
    Pad_4
    (1,
     "-12 dB", "-6 dB", "0 dB","+6 dB"), //TODO reversed from 2-In yaml
    MidiCh_16
    (0,
     "ch1", "ch 2", "ch3", "ch4", "ch5", "ch6", "ch7", "ch8",
     "ch9", "ch10", "ch11", "ch12", "ch13", "ch14", "ch15", "ch16", "This"),
    MidiCh_17
    (0,
     "ch1", "ch2", "ch3", "ch4", "ch5", "ch6", "ch7", "ch8",
     "ch9", "ch10", "ch11", "ch12", "ch13", "ch14", "ch15", "ch16", "This", "keyb"),
    NoteZoneThru
    (0,
     "Notes Only", "Note+Ctrls"),
    Threshold_42
    (0,42,18),
    CompressorRatio
    (0,66,20),
    CompressorAttack
    (0,127,1),
    CompressorRelease
    (0,127,20),
    CompressorRefLevel
    (0,42,30),
    KeyQuantCapture
    (0,
     "Closest", "Evenly"),
    SeqCtrlXFade
    (0,
     "Off", "25%", "50%", "100%"),
    BipPosNeg
    (0,
     "Bip", "Pos", "Neg"),
    GlideTime
    (0,127,64),
    Freq_1
    (0,127,64),
    CombType
    (0,
     "Notch", "Peak", "Deep"),
    OscShpA_Waveform
    (0,
     "Sine1", "Sine2", "Sine3", "Sine4", "TriSaw", "SymPulse"),
    DxAlgorithm
    (0,31,0),
    DxFeedback
    (0,
     "0", "1", "2", "3", "4", "5", "6", "7"),
    PShiftCoarse
    (0,127,64),
    PShiftFine
    (0,127,64),
    Source_2
    (0,
     "In 1/2", "In 3/4", "Bus 1/2", "Bus 3/4"),
    Source_3
    (0,
     "In", "Bus"),
    DelayTime_3
    (0,127,0),
    DelayRange_3
    (0,
     "5 m", "25 m", "100 m", "500 m", "1.0 s", "2.0 s", "2.7 s"),
    TimeClk
    (0,
     "Time", "Clk"), // NB these line up with yaml
    DelayTime_2
    (0,127,0),
    DelayRange_2
    (0,
     "500 m", "1.0 s", "2.0 s", "2.7 s"), // NB this lines up with images in yaml
    RatioFixed
    (0,
     "Ratio", "Fixed"),
    OpFreqCoarse
    (0,31,0),
    OpFreqFine
    (0,99,0),
    OpFreqDetune
    (0,14,0),
    OpVel
    (0,7,0),
    OpRateScale
    (0,7,0),
    OpTime
    (0,99,0),
    OpLevel
    (0,99,0),
    OpAmod
    (0,7,0),
    OpBrPpoint
    (0,99,0),
    OpDepthMode
    (0,3,0),
    OpDepth
    (0,99,0),
    DelayTime_1
    (0,127,0),
    DelayRange_1
    (0,
     "500 m", "1.0s", /*"2.0 s",*/ "1.35s"), //TODO check
    OscWaveform_1
    (0,
     "Sine", "Tri"),
    Threshold_127
    (0,127,0),
    NoiseGateAttack
    (0,127,0),
    NoiseGateRelease
    (0,127,64),
    LfoB_Waveform
    (0,
     "Sine", "Tri", "Saw", "Square"),
    PhaserType
    (0,
     "Type I", "Type II"),
    PhaserFreq
    (0,127,64),
    ExpLin_1
    (0,
     "Exp", "Lin"),
    ModAmtInvert
    (0,
     "m", "1-m"),
    MonoKeyMode
    (0,
     "Last", "Lo", "Hi"),
    RndEdge
    (0,
     "0%", "25%", "50%", "75%", "100%"),
    RandomAStepProb
    (0,
     "25%", "50%", "75%", "100%"),
    Rnd_1
    (0,
     "Rnd1", "Rnd2"),
    RangeBip_128
    (0,127,64),
    RndStepPulse
    (0,
     "Step", "Pulse"),

    /*
     * Pseudo-params for patch settings follow
     */

    GainVolume
    (0,127,100),
    GainActiveMuted
    (1,
     "Off","On"),
    Glide
    (2,
     "Auto","Normal","Off"),
    GlideSpeed
    (0,127,0),
    BendEnable
    (0,
     "Off","On"),
    BendSemi
    (0,23,2),
    Vibrato
    (2,
     "Wheel","AftTouch","Off"),
    VibCents
    (0,127,0),
    VibRate
    (0,127,0),
    ArpEnable
    (0,
     "Off","On"),
    ArpTime
    (0,
     "1/8","1/8T","1/16","1/16T"),
    ArpDir
    (0,
     "Up","Dn","UpDn","Rnd"),
    ArpOctaves
    (0,
     "1","2","3","4"),
    MiscOctShift
    (2,
     "-2","-1","0","1","2"),
    MiscSustain
    (1,
     "Off","On"),
    MorphDial
    (0,127,0),
    MorphMode
    (1,
     "Knob","Morph")




    ;


    public final int min;
    public final int max;
    public final int def;
    public final List<String> enums;

    ModParam(int min, int max, int def) {
        this.min = min;
        this.max = max;
        this.def = def;
        this.enums = null;
        if (def < min || def > max) {
            throw new IllegalArgumentException("Invalid default: " + def);
        }
    }

    ModParam(int def,String... enums) {
        this.min = 0;
        this.max = enums.length - 1;
        this.def = def;
        this.enums = List.of(enums);
        if (def < min || def > max) {
            throw new IllegalArgumentException("Invalid default: " + def);
        }
    }

    public NamedParam mk(String name) {
        return new NamedParam(this,name,List.of());
    }

    public NamedParam mk() {
        return new NamedParam(this,name(),List.of());
    }


}
