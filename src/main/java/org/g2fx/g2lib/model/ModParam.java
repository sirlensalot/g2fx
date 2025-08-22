package org.g2fx.g2lib.model;

import java.util.List;
import java.util.function.Function;

import static org.g2fx.g2lib.model.ModParam.ParamFormatter.intF;

public enum ModParam {

    Dst_2
    (0,
     "Out", "Fx", "Bus"),
    OffOn
    (0,
     ParamFormatter.boolF(b -> b ? "4" : "0"),
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
    (0,127,0, ParamFormatter.ID),
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
    (0,127,64, intF(n -> String.format("%.01fdB",
            (n == 127 ? 64 : n - 64) / 3.55555))),
    EqLoFreq
    (0,
     "80 Hz", "110 Hz", "160 Hz"),
    EqHiFreq
    (0,
     "6 kHz", "8 kHz", "12 kHz"),
    EqMidFreq
    (0,127,93, intF(n -> formatHz(100 * Math.pow(2, n / 20.089)))),
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
    (0,127,75, intF(n ->
            formatHz(440.0 * Math.pow(2, (double) (n - 60) / 12)))), //TODO lo shd be 13.76hz but is 13.8hz
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
    (0,127,60, intF(n -> formatHz(20 * Math.pow(2, n / 13.169)))),
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
    (0,42,18, intF(n -> n == 42 ? "Off" : (n-30)+"dB")),
    CompressorRatio
    (0,66,20,
            intF(n -> String.format("%.01f:1",
            n < 10 ? 1 + n/10 :
                    n < 25 ? n/5 :
                            n < 35 ? 5 + (n-25)/2 :
                                    n < 45 ? (float) 10 + (n-35) :
                                            n < 60 ? 20 + (n-45)*2 :
                                                    50 + (n-60)*5))),
    CompressorAttack
    (0,127,1, intF(n -> ParamConstants.COMPR_ATTACK_TIMES[n])),
    CompressorRelease
    (0,127,20, intF(n -> ParamConstants.COMPR_RELEASE_TIMES[n])),
    CompressorRefLevel
    (0,42,30, intF(n -> (n - 30) + "dB")),
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
     "5m", "25m", "100m", "500m", "1.0s", "2.0s", "2.7s"),
    TimeClk
    (0,
     "Time", "Clk"), // NB these line up with yaml
    DelayTime_2
    (0,127,0),
    DelayRange_2
    (0,
     "500m", "1.0s", "2.0s", "2.7s"), // NB this lines up with images in yaml
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
     "500m", "1.0s", /*"2.0s",*/ "1.35s"), //TODO check
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

    private static String formatHz(double f) {
        return f >= 1000 ?
                String.format("%.01fkHz", f / 1000) :
                String.format("%.01fHz", f);
    }


    public final int min;
    public final int max;
    public final int def;
    public final ParamFormatter formatter;
    public final List<String> enums;

    ModParam(int min, int max, int def) {
        this(min,max,def,null);
    }
    ModParam(int min, int max, int def, ParamFormatter formatter) {
        this.min = min;
        this.max = max;
        this.def = def;
        this.formatter = formatter;
        this.enums = null;
        if (def < min || def > max) {
            throw new IllegalArgumentException("Invalid default: " + def);
        }
    }

    ModParam(int def,String... enums) {
        this(def, null, enums);
    }

    ModParam(int def,ParamFormatter formatter,String... enums) {
        this.min = 0;
        this.max = enums.length - 1;
        this.def = def;
        this.enums = List.of(enums);
        if (def < min || def > max) {
            throw new IllegalArgumentException("Invalid default: " + def);
        }
        this.formatter = formatter;
    }

    public NamedParam mk(String name) {
        return new NamedParam(this,name,List.of());
    }

    public NamedParam mk() {
        return new NamedParam(this,name(),List.of());
    }

    public record ParamFormatter(Function<Integer,String> intFmt,Function<Boolean,String> boolFmt) {
        public static ParamFormatter intF(Function<Integer,String> f) {
            return new ParamFormatter(f,null);
        }
        public static ParamFormatter boolF(Function<Boolean,String> f) {
            return new ParamFormatter(null,f);
        }
        public static ParamFormatter ID =
                new ParamFormatter(n -> Integer.toString(n),n -> Boolean.toString(n));
    }
    /**
     * Can't access constants in enum constructor calls, but can access a static class ...
     */
    interface ParamConstants {

        String[] COMPR_ATTACK_TIMES = new String[]{
                "Fast", "0.53m", "0.56m", "0.59m", "0.63m", "0.67m", "0.71m", "0.75m",
                "0.79m", "0.84m", "0.89m", "0.94m", "1.00m", "1.06m", "1.12m", "1.19m",
                "1.26m", "1.33m", "1.41m", "1.50m", "1.59m", "1.68m", "1.78m", "1.89m",
                "2.00m", "2.12m", "2.24m", "2.38m", "2.52m", "2.67m", "2.83m", "3.00m",
                "3.17m", "3.36m", "3.56m", "3.78m", "4.00m", "4.24m", "4.49m", "4.76m",
                "5.04m", "5.34m", "5.66m", "5.99m", "6.35m", "6.73m", "7.13m", "7.55m",
                "8.00m", "8.48m", "8.98m", "9.51m", "10.1m", "10.7m", "11.3m", "12.0m",
                "12.7m", "13.5m", "14.3m", "15.1m", "16.0m", "17.0m", "18.0m", "19.0m",
                "20.2m", "21.4m", "22.6m", "24.0m", "25.4m", "26.9m", "28.5m", "30.2m",
                "32.0m", "33.9m", "35.9m", "38.1m", "40.3m", "42.7m", "45.3m", "47.9m",
                "50.8m", "53.8m", "57.0m", "60.4m", "64.0m", "67.8m", "71.8m", "76.1m",
                "80.6m", "85.4m", "90.5m", "95.9m", " 102m", " 108m", " 114m", " 121m",
                " 128m", " 136m", " 144m", " 152m", " 161m", " 171m", " 181m", " 192m",
                " 203m", " 215m", " 228m", " 242m", " 256m", " 271m", " 287m", " 304m",
                " 323m", " 342m", " 362m", " 384m", " 406m", " 431m", " 456m", " 483m",
                " 512m", " 542m", " 575m", " 609m", " 645m", " 683m", " 724m", " 767m"};

        String[] COMPR_RELEASE_TIMES = new String[]{
                " 125m", " 129m", " 134m", " 139m", " 144m", " 149m", " 154m", " 159m",
                " 165m", " 171m", " 177m", " 183m", " 189m", " 196m", " 203m", " 210m",
                " 218m", " 225m", " 233m", " 241m", " 250m", " 259m", " 268m", " 277m",
                " 287m", " 297m", " 308m", " 319m", " 330m", " 342m", " 354m", " 366m",
                " 379m", " 392m", " 406m", " 420m", " 435m", " 451m", " 467m", " 483m",
                " 500m", " 518m", " 536m", " 555m", " 574m", " 595m", " 616m", " 637m",
                " 660m", " 683m", " 707m", " 732m", " 758m", " 785m", " 812m", " 841m",
                " 871m", " 901m", " 933m", " 966m", "1.00s", "1.04s", "1.07s", "1.11s",
                "1.15s", "1.19s", "1.23s", "1.27s", "1.32s", "1.37s", "1.41s", "1.46s",
                "1.52s", "1.57s", "1.62s", "1.68s", "1.74s", "1.80s", "1.87s", "1.93s",
                "2.00s", "2.07s", "2.14s", "2.22s", "2.30s", "2.38s", "2.46s", "2.55s",
                "2.64s", "2.73s", "2.83s", "2.93s", "3.03s", "3.14s", "3.25s", "3.36s",
                "3.48s", "3.61s", "3.73s", "3.86s", "4.00s", "4.14s", "4.29s", "4.44s",
                "4.59s", "4.76s", "4.92s", "5.10s", "5.28s", "5.46s", "5.66s", "5.86s",
                "6.06s", "6.28s", "6.50s", "6.73s", "6.96s", "7.21s", "7.46s", "7.73s",
                "8.00s", "8.28s", "8.57s", "8.88s", "9.19s", "9.51s", "9.85s", "10.2s"};


    }


}
