package org.g2fx.g2lib.model;

import org.g2fx.g2lib.util.Util;

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
    (0,127,0, ParamFormatter.ID), //TODO!! not sure this is really 0-127 (FltClassic Res yes, Reverb brightness???, RndClkA StepProb BZZT)
    FreqMode_3
    (0,
     "Semi", "Freq", "Fac", "Part"),
    PW
    (0,127,0, intF(n->String.format("%.00f%%",Math.floor(50 + 50.0 * n / 128)))),
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
    (0,127,0,intF(n -> aref(n,ParamConstants.ENV_TIMES,v ->
            v < 0.001 ? String.format("%.01fm",v*1000) :
                v < 1 ? String.format("%.00fm",v*1000) :
                        String.format("%.01fs",v)))),
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
    (0,127,0,ParamFormatter.intMapper(359)),
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
    (0,127,0,intF(n->aref(n,ParamConstants.FILTER_RESONANCE,v->
            String.format(v < 10 ? "%.02f" : "%.00f",v)))),
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
    (0,127,0, ParamFormatter.ID),
    MidiCh_20
    (0,
     "ch1", "ch 2", "ch3", "ch4", "ch5", "ch6", "ch7", "ch8",
     "ch9", "ch10","ch11", "ch12", "ch13", "ch14", "ch15", "ch16",
     "This", "Slot A", "Slot B", "Slot C", "Slot D"),
    DrumSynthFreq
    (0,127,42,intF(n->String.format("%.01fHz",20.02 * Math.pow(2, (double) n /24)))),
    DrumSynthRatio
    (0,127,15,intF(n->switch(n) {
        case 0 -> "1:1";
        case 48 -> "2:1";
        case 96 -> "4:1";
        default -> String.format("x%.02f",Math.pow(2, (double) n /48));
    })),
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
    (0,127,64, intF(n -> formatHz(440 * Math.pow(2, (double) (n - 45) /12)))),
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
    (0,99,0, ParamFormatter.intMapper(99)),
    OpLevel
    (0,99,0, ParamFormatter.intMapper(99)),
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
    (0,127,0, intF(n -> aref(n,ParamConstants.NOISEGATE_PITCHTRACK_THRESHOLD, v ->
         v == 0 ? "Inf." : String.format("%.01fdB",v)))),
    NoiseGateAttack
    (0,127,0, intF(n -> String.format("%.01fm",ParamConstants.NOISE_GATE_ATTACK[n]))),
    NoiseGateRelease
    (0,127,64, intF(n -> aref(n,ParamConstants.NOISE_GATE_RELEASE, v ->
         v == 1000 ? "1s" : String.format(v >= 100 ? "%.00fm" : "%.01fm",v)))),
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
    RndProb
    (0,100,50,intF(n->String.format("%d%%",Util.mapRange(n,0,127,1,100)))),
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

    private static <T> T aref(int idx, double[] vals, Function<Double,T> f) {
        return f.apply(vals[idx]);
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

        public static ParamFormatter intMapper(int max) {
            return intF(n -> String.format("%d", Util.mapRange(n,0,127,0,max)));
        }
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

        double[] ENV_TIMES = new double[]{
                0.0005, 0.0006, 0.0007, 0.0009, 0.0011, 0.0013, 0.0015, 0.0018,
                0.0021, 0.0025, 0.0030, 0.0035, 0.0040, 0.0047, 0.0055, 0.0063,
                0.0073, 0.0084, 0.0097, 0.0111, 0.0127, 0.0145, 0.0165, 0.0187,
                0.0212, 0.0240, 0.0271, 0.0306, 0.0344, 0.0387, 0.0434, 0.0486,
                0.0543, 0.0606, 0.0676, 0.0752, 0.0836, 0.0928, 0.1030, 0.1140,
                0.1260, 0.1390, 0.1530, 0.1690, 0.1860, 0.2040, 0.2240, 0.2460,
                0.2690, 0.2950, 0.3220, 0.3520, 0.3840, 0.4190, 0.4560, 0.4960,
                0.5400, 0.5860, 0.6360, 0.6900, 0.7480, 0.8100, 0.8760, 0.9470,
                1.0200, 1.1000, 1.1900, 1.2800, 1.3800, 1.4900, 1.6000, 1.7200,
                1.8500, 1.9900, 2.1300, 2.2800, 2.4600, 2.6200, 2.8100, 3.0000,
                3.2100, 3.4300, 3.6600, 3.9100, 4.1700, 4.4500, 4.7400, 5.0500,
                5.3700, 5.7200, 6.0800, 6.4700, 6.8700, 7.3000, 7.7500, 8.2200,
                8.7200, 9.2500, 9.8000, 10.400, 11.000, 11.600, 12.300, 13.000,
                13.800, 14.600, 15.400, 16.200, 17.100, 18.100, 19.100, 20.100,
                21.200, 22.400, 23.500, 24.800, 26.100, 27.500, 28.900, 30.400,
                32.000, 33.600, 35.300, 37.100, 38.900, 40.900, 42.900, 45.000};

        double[] NOISEGATE_PITCHTRACK_THRESHOLD = new double[] {
                -100.0, -42.1, -36.1, -32.5, -30.0, -28.1, -26.5, -25.2,
                -24.0, -23.0, -22.1, -21.2, -20.5, -19.8, -19.2, -18.6,
                -18.0, -17.5, -17.0, -16.5, -16.1, -15.6, -15.2, -14.8,
                -14.5, -14.1, -13.8, -13.4, -13.1, -12.8,  -12.5, -12.2,
                -12.0, -11.7, -11.4, -11.2, -11.0, -10.7, -10.5, -10.3,
                -10.0, -9.8, -9.6, -9.4, -9.2, -9.0, -8.8, -8.6,
                -8.5, -8.3, -8.1, -7.9, -7.8, -7.6, -7.4, -7.3,
                -7.1, -7.0, -6.8, -6.7, -6.5, -6.4, -6.2, -6.1,
                -6.0, -5.8, -5.7, -5.6, -5.4, -5.3, -5.2, -5.1,
                -4.9, -4.8, -4.7, -4.6, -4.5, -4.3, -4.2, -4.1,
                -4.0, -3.9, -3.8, -3.7, -3.6, -3.5, -3.4, -3.3,
                -3.2, -3.1, -3.0, -2.9, -2.8, -2.7, -2.6, -2.5,
                -2.4, -2.3, -2.3, -2.2, -2.1, -2.0, -1.9, -1.8,
                -1.7, -1.7, -1.6, -1.5, -1.4, -1.3, -1.2, -1.2,
                -1.1, -1.0, -0.9, -0.9, -0.8, -0.7, -0.6, -0.6,
                -0.5, -0.4, -0.3, -0.3, -0.2, -0.1, -0.1, -0.0};

        double[] NOISE_GATE_ATTACK  = new double[] {
                0.2, 0.3, 0.4, 0.5, 0.6, 0.8, 0.9, 1.0,
                1.2, 1.4, 1.6, 1.7, 2.0, 2.2, 2.4, 2.6,
                2.9, 3.1, 3.4, 3.7, 4.0, 4.3, 4.6, 4.9,
                5.3, 5.6, 6.0, 6.3, 6.7, 7.1, 7.5, 7.9,
                8.4, 8.8, 9.3, 9.7, 10.2, 10.7, 11.2, 11.7,
                12.2, 12.7, 13.3, 13.8, 14.4, 14.9, 15.5, 16.1,
                16.7, 17.4, 18.0, 18.6, 19.3, 19.9, 20.6, 21.3,
                22.0, 22.7, 23.4, 24.1, 24.9, 25.6, 26.4, 27.2,
                28.0, 28.8, 29.6, 30.4, 31.2, 32.1, 32.9, 33.8,
                34.6, 35.5, 36.4, 37.3, 38.3, 39.2, 40.1, 41.1,
                42.0, 43.0, 44.0, 45.0, 46.0, 47.0, 48.1, 49.1,
                50.2, 51.2, 52.3, 53.4, 54.5, 55.6, 56.7, 57.9,
                59.0, 60.2, 61.3, 62.5, 63.7, 64.9, 66.1, 67.3,
                68.6, 69.8, 71.1, 72.3, 73.6, 74.9, 76.2, 77.5,
                78.8, 80.2, 81.5, 82.9, 84.2, 85.6, 87.0, 88.4,
                89.8, 91.2, 92.7, 94.1, 95.6, 97.0, 98.5, 100};

        double[] NOISE_GATE_RELEASE  = new double[] {
                0.5, 0.59, 0.67, 0.76, 0.87, 0.98, 1.11, 1.25,
                1.40, 1.57, 1.75, 1.95, 2.17, 2.41, 2.66, 2.94,
                3.25, 3.57, 3.92, 4.30, 4.71, 5.15, 5.62, 6.12,
                6.66, 7.24, 7.85, 8.51, 9.21, 9.96, 10.7, 11.6,
                12.5, 13.4, 14.4, 15.5, 16.6, 17.8, 19.1, 20.4,
                21.8, 23.3, 24.9, 26.5, 28.2, 30.0, 32.0, 34.0,
                36.1, 38.3, 40.6, 43.0, 45.5, 48.2, 51.0, 53.9,
                56.9, 60.1, 63.4, 66.8, 70.4, 74.2, 78.1, 82.2,
                86.4, 90.9, 95.5, 100, 105, 110, 116, 121,
                127, 133, 139, 146, 153, 160, 167, 174,
                182, 190, 198, 207, 216, 225, 234, 244,
                254, 265, 275, 286, 298, 310, 322, 335,
                348, 361, 375, 389, 404, 419, 434, 450,
                467, 484, 501, 519, 537, 556, 578, 596,
                616, 638, 659, 682, 705, 728, 752, 777,
                802, 828, 855, 883, 911, 940, 970, 1000};

        double[] FILTER_RESONANCE = new double[] {
                0.50, 0.51, 0.51, 0.52, 0.53, 0.54, 0.55, 0.55,
                0.56, 0.57, 0.58, 0.59, 0.60, 0.61, 0.62, 0.63,
                0.64, 0.64, 0.66, 0.67, 0.68, 0.69, 0.70, 0.71,
                0.73, 0.74, 0.75, 0.76, 0.78, 0.79, 0.81, 0.82,
                0.84, 0.84, 0.87, 0.88, 0.90, 0.92, 0.94, 0.95,
                0.97, 0.99, 1.01, 1.03, 1.06, 1.08, 1.10, 1.12,
                1.15, 1.17, 1.20, 1.23, 1.25, 1.28, 1.31, 1.34,
                1.37, 1.41, 1.44, 1.48, 1.51, 1.55, 1.59, 1.63,
                1.67, 1.72, 1.76, 1.81, 1.86, 1.91, 1.97, 2.03,
                2.08, 2.15, 2.21, 2.28, 2.35, 2.42, 2.50, 2.58,
                2.67, 2.76, 2.85, 2.95, 3.05, 3.16, 3.28, 3.40,
                3.53, 3.67, 3.81, 3.96, 4.13, 4.30, 4.49, 4.68,
                4.89, 5.12, 5.36, 5.61, 5.89, 6.19, 6.51, 6.85,
                7.23, 7.64, 8.08, 8.56, 9.08, 9.66, 10, 11,
                12, 13, 14, 15, 16, 17, 19, 20,
                22, 25, 27, 30, 34, 38, 44, 50};

    }


}
