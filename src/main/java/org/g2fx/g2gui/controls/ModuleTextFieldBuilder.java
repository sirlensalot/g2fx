package org.g2fx.g2gui.controls;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2lib.model.ParamFormatter;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.panel.ModulePane.layout;
import static org.g2fx.g2lib.model.ModParam.*;
import static org.g2fx.g2lib.model.ParamConstants.*;
import static org.g2fx.g2lib.model.ParamFormatter.fmt01f;

public class ModuleTextFieldBuilder {

    private static final int TF_OSC_FREQ = 60;
    private static final int TF_LFO_FREQ = 103;
    private static final int TF_OPERATOR_FREQ = 198; // TODO very wrong
    private static final int TF_CONST_BIP = 96; // TODO doesn't honor BiP switch, has deps though
    private static final int TF_LEV_AMP = 147;
    private static final int TF_CLK_GEN = 110;
    private static final int TF_PULSE_TIME = 122;
    private static final int TF_MIX_LEV = 102; // TODO doesn't handle Exp
    private static final int TF_PSHIFT_FREQ = 201;
    private static final int TF_DELAY_TIME = 141;


    private final ModulePane parent;

    public ModuleTextFieldBuilder(ModulePane parent) {
        this.parent = parent;
    }

    public Node mkTextField(UIElements.TextField c) {

        Label l = layout(c,withClass(new Label("0"),"module-text-field"),new Point2D(0,1));
        l.setAlignment(Pos.CENTER);
        l.setPrefWidth(c.Width());


        ModulePane.IndexParam ip = parent.resolveParam(c.MasterRef());
        l.setUserData(parent.paramId(ip));

        ParamFormatter pf = ip.param().param().formatter;

        Property<Boolean> pb = parent.getBoolProp(ip.index());
        if (pb != null && pf != null && pf.boolFmt() != null) {
            return formatParam(l,pb,pf.boolFmt());
        }

        Property<Integer> pi = parent.getIntProp(ip.index());
        if (pi != null && pf != null && pf.intFmt() != null) {
            return formatParam(l,pi,pf.intFmt());
        }

        switch (c.TextFunc()) {
            case TF_OSC_FREQ: return formatOscFreq(c, l);
            case TF_LFO_FREQ: return formatLfoFreq(c, l);
            case TF_OPERATOR_FREQ: return formatOperatorFreq(c,l);
            case TF_CLK_GEN: return formatClkTempo(c,l);
            case TF_PULSE_TIME: return formatPulseTime(c,l);
            case TF_PSHIFT_FREQ: return formatPshiftFreq(c,l);
            case TF_LEV_AMP: return formatLevAmp(c,l);
            case TF_CONST_BIP: return formatConstBip(c,l);
            case TF_MIX_LEV: return formatMixLev(c,l);
            case TF_DELAY_TIME: return formatDelayTime(c,l);
        }

        System.out.format("%s, pi: %s, pb: %s\n",ip,pi,pb);
        return parent.empty(c,"mkTextField");

    }

    private Node formatDelayTime(UIElements.TextField c, Label l) {
        return fmtIntInt(l,c,0,1,(t,n) -> delayDispValue(0,t,n));
    }

    private Node formatMixLev(UIElements.TextField c, Label l) {
        return fmtIntInt(l,c,0,1,(t, n) ->
                t == 2 ? aref(n,MIX_LEV_DB, this::fmtNegInf) :
                        fmt01f(n==127?100:((double) n * 100) / 128));
    }

    private Node formatConstBip(UIElements.TextField c, Label l) {
        return fmtIntInt(l,c,0,1,(t, n) ->
                t == 0 ? (Integer.toString(n==127?64:n-64)) :
                        fmt01f(n==127?64.0:((double) n) / 2));
    }

    private Node formatLevAmp(UIElements.TextField c, Label l) {
        return fmtIntInt(l,c,0,1, (t, n) ->
                t == 0 ? String.format("x%.02f", 4 * ((double) n) / 127) :
                        aref(n, LEV_AMP_DB, this::fmtNegInf));
    }

    private String fmtNegInf(Double v) {
        return v == Double.NEGATIVE_INFINITY ? "-∞" : fmt01f(v);
    }

    /**
     * parameterized as "val" and "type" but really an int bifunction
     */
    private Node fmtIntInt(Label l, UIElements.TextField tf, int valDep, int typeDep,
                           BiFunction<Integer, Integer, String> f) {
        ObservableValue<Integer> pValue = parent.resolveDepParam(tf,valDep);
        ObservableValue<Integer> pType = parent.resolveDepParam(tf,typeDep);
        ChangeListener<Integer> listener = (cc,oo,nn) -> {
            int n = pValue.getValue();
            int t = pType.getValue();
            l.setText(f.apply(t, n));
        };
        pValue.addListener(listener);
        pType.addListener(listener);
        return l;
    }

    private Node formatPshiftFreq(UIElements.TextField c, Label l) {
        ObservableValue<Integer> pCoarse = parent.resolveDepParam(c, 0);
        ObservableValue<Integer> pFine = parent.resolveDepParam(c, 1);
        ChangeListener<Integer> listener =
                mkFreqFormatListener(l, pCoarse, pFine, new SimpleObjectProperty<>(4));
        pCoarse.addListener(listener);
        pFine.addListener(listener);
        return l;
    }

    private Node formatPulseTime(UIElements.TextField c, Label l) {
        ObservableValue<Integer> pTime = parent.resolveDepParam(c,0);
        ObservableValue<Integer> pRange = parent.resolveDepParam(c,1);
        ChangeListener<Integer> listener = (cc, o, n) -> {
            double t = PULSE_DELAY_RANGE[pTime.getValue()];
            l.setText(formatMillisSecs(switch (pRange.getValue()) {
                case 0 -> t/100;
                case 1 -> t/10;
                default -> t;
            }));
        };
        pTime.addListener(listener);
        pRange.addListener(listener);
        return l;
    }

    private Node formatClkTempo(UIElements.TextField c, Label l) {
        ObservableValue<Integer> pRateBpm = parent.resolveDepParam(c,0);
        ObservableValue<Boolean> pActive = parent.resolveBoolDepParam(c,1);
        ObservableValue<Integer> pSource = parent.resolveDepParam(c,2);
        ChangeListener<Integer> listener = (cc, o, n) -> {
            l.setText(!pActive.getValue() ? "--" : pSource.getValue() == 1 ? "MASTER" :
                    (g2BPM(pRateBpm.getValue()) + " BPM"));
        };
        pRateBpm.addListener(listener);
        pActive.addListener((cc, o, n) -> listener.changed(null,0,0));
        pSource.addListener(listener);
        return l;
    }

    private Node formatOperatorFreq(UIElements.TextField c, Label l) {
        ObservableValue<Integer> pCoarse = parent.resolveDepParam(c,0);
        ObservableValue<Integer> pFine = parent.resolveDepParam(c,1);
        ObservableValue<Integer> pRatio = parent.resolveDepParam(c,2);
        ChangeListener<Integer> listener = (cc, o, n) -> {
            int aValue = pCoarse.getValue();
            int iValue1 = pFine.getValue();
            // TODO these are both bananas, port logic anew
            if (pRatio.getValue()==0) {
                double Fact = aValue == 0 ? 0.5 : aValue;
                l.setText(String.format("x%.01f",Fact + Fact * iValue1 / 100));
            } else {
                l.setText(formatHz(Math.pow(10, Math.divideExact(aValue,4))));
            }
        };
        pCoarse.addListener(listener);
        pFine.addListener(listener);
        pRatio.addListener(listener);
        return l;
    }

    private Label formatLfoFreq(UIElements.TextField c, Label l) {
        ObservableValue<Integer> pRate = parent.resolveDepParam(c,0);
        ObservableValue<Integer> pRange = parent.resolveDepParam(c,1);
        ChangeListener<Integer> listener = (cc, o, n) -> {
            int r = pRate.getValue();
            l.setText(switch (pRange.getValue()) {
                case 0 -> String.format("%.02f",699/(double)(r+1)); //Rate Sub
                case 1 -> r < 32 ? // Rate Lo
                        String.format("%.02fs",1/(0.0159 * Math.pow(2, (double) r / 12))) :
                        String.format("%.02fHz",0.0159 * Math.pow(2, (double) r / 12));
                case 2 -> String.format("%.01fHz",0.2555 * Math.pow(2, (double) r / 12)); // Rate Hi
                case 3 -> Integer.toString(g2BPM(r));
                default -> LFO_CLOCK_VALS[r/4];
            });
        };
        pRange.addListener(listener);
        pRate.addListener(listener);
        return l;
    }

    private static int g2BPM(int rateParam) {
        return rateParam <= 32 ? 24 + 2 * rateParam :
                rateParam <= 96 ? 88 + rateParam - 32 :
                        152 + (rateParam - 96) * 2;
    }

    private Label formatOscFreq(UIElements.TextField c, Label l) {
        ObservableValue<Integer> pCoarse = parent.resolveDepParam(c, 0);
        ObservableValue<Integer> pFine = parent.resolveDepParam(c, 1);
        ObservableValue<Integer> pMode = parent.resolveDepParam(c, 2);
        ChangeListener<Integer> listener = mkFreqFormatListener(l, pCoarse, pFine, pMode);
        pCoarse.addListener(listener);
        pFine.addListener(listener);
        pMode.addListener(listener);
        return l;
    }

    public static String delayDispValue(int aType, int aRange, int aValue) {
        float DlyRange, DlyMin, DlyMax;
        switch (aType) {
            case 0:
                switch (aRange) {
                    case 0:
                        DlyMin = 0.05f;
                        DlyMax = 5.3f;
                        if (aValue == 0)
                            return "0.01m";
                        else
                            return g2FloatToStr(DlyMin + (DlyMax - DlyMin) * (aValue - 1) / 126, 4) + "m";
                    case 1:
                        DlyMin = 0.21f;
                        DlyMax = 25.1f;
                        if (aValue == 0)
                            return "0.01m";
                        else
                            return g2FloatToStr(DlyMin + (DlyMax - DlyMin) * (aValue - 1) / 126, 4) + "m";
                    case 2:
                        DlyMin = 0.8f;
                        DlyMax = 100f;
                        if (aValue == 0)
                            return "0.01m";
                        else
                            return g2FloatToStr(DlyMin + (DlyMax - DlyMin) * (aValue - 1) / 126, 4) + "m";
                    case 3:
                        DlyMin = 3.95f;
                        DlyMax = 500f;
                        if (aValue == 0)
                            return "0.01m";
                        else
                            return g2FloatToStr(DlyMin + (DlyMax - DlyMin) * (aValue - 1) / 126, 4) + "m";
                    case 4:
                        DlyMin = 7.89f;
                        DlyMax = 1000f;
                        if (aValue == 0)
                            return "0.01m";
                        else if (aValue == 127)
                            return "1.000s";
                        else
                            return g2FloatToStr(DlyMin + (DlyMax - DlyMin) * (aValue - 1) / 126, 4) + "m";
                    case 5:
                        DlyMin = 15.8f;
                        DlyMax = 2000f;
                        if (aValue == 0)
                            return "0.01m";
                        else if (aValue >= 64)
                            return g2FloatToStr((DlyMin + (DlyMax - DlyMin) * (aValue - 1) / 126) / 1000, 5) + "s";
                        else
                            return g2FloatToStr(DlyMin + (DlyMax - DlyMin) * (aValue - 1) / 126, 4) + "m";
                    case 6:
                        DlyMin = 21.3f;
                        DlyMax = 2700f;
                        if (aValue == 0)
                            return "0.01m";
                        else if (aValue >= 48)
                            return g2FloatToStr((DlyMin + (DlyMax - DlyMin) * (aValue - 1) / 126) / 1000, 5) + "s";
                        else
                            return g2FloatToStr(DlyMin + (DlyMax - DlyMin) * (aValue - 1) / 126, 5) + "m";
                }
                break;
            case 1:
                switch (aRange) {
                    case 0:
                        DlyMin = 0f;
                        DlyMax = 0.66f;
                        break;
                    case 1:
                        DlyMin = 0f;
                        DlyMax = 3.14f;
                        break;
                    case 2:
                        DlyMin = 0f;
                        DlyMax = 12.6f;
                        break;
                    case 3:
                        DlyMin = 0f;
                        DlyMax = 62.5f;
                        break;
                    case 4:
                        DlyMin = 0f;
                        DlyMax = 125f;
                        break;
                    case 5:
                        DlyMin = 0f;
                        DlyMax = 250f;
                        break;
                    case 6:
                        DlyMin = 0f;
                        DlyMax = 338f;
                        break;
                    default:
                        DlyMin = 0f; // fallback
                        DlyMax = 0f;
                }
                return g2FloatToStr(DlyMin + (DlyMax - DlyMin) * aValue / 127, 4) + "m";
            case 2:
                switch (aRange) {
                    case 0:
                        DlyRange = 500f;
                        if (aValue == 0)
                            return "0.01m";
                        else
                            return g2FloatToStr(DlyRange * aValue / 127, 4) + "m";
                    case 1:
                        DlyRange = 1000f;
                        if (aValue == 0)
                            return "0.01m";
                        else if (aValue == 127)
                            return "1.00s";
                        else
                            return g2FloatToStr(DlyRange * aValue / 127, 4) + "m";
                    case 2:
                        DlyRange = 2000f;
                        if (aValue == 0)
                            return "0.01m";
                        else if (aValue >= 64)
                            return g2FloatToStr(DlyRange * aValue / 127000, 5) + "s";
                        else
                            return g2FloatToStr(DlyRange * aValue / 127, 4) + "m";
                    case 3:
                        DlyRange = 2700f;
                        if (aValue == 0)
                            return "0.01m";
                        else if (aValue >= 48)
                            return g2FloatToStr(DlyRange * aValue / 127000, 5) + "s";
                        else
                            return g2FloatToStr(DlyRange * aValue / 127, 4) + "m";
                }
                break;
            case 3:
                switch (aRange) {
                    case 0:
                        DlyRange = 500f;
                        if (aValue == 0)
                            return "0.01m";
                        else
                            return g2FloatToStr(DlyRange * aValue / 127, 4) + "m";
                    case 1:
                        DlyRange = 1000f;
                        if (aValue == 0)
                            return "0.01m";
                        else if (aValue == 127)
                            return "1.00s";
                        else
                            return g2FloatToStr(DlyRange * aValue / 127, 4) + "m";
                    case 2:
                        DlyRange = 1351f;
                        if (aValue == 0)
                            return "0.01m";
                        else if (aValue >= 95)
                            return g2FloatToStr(DlyRange * aValue / 127000, 5) + "s";
                        else
                            return g2FloatToStr(DlyRange * aValue / 127, 4) + "m";
                }
                break;
            case 4:
                if (aValue >= 0 && aValue <= 3) return "1/64T";
                if (aValue >= 4 && aValue <= 7) return "1/64";
                if (aValue >= 8 && aValue <= 11) return "1/32T";
                if (aValue >= 12 && aValue <= 15) return "1/64D";
                if (aValue >= 16 && aValue <= 19) return "1/32";
                if (aValue >= 20 && aValue <= 23) return "1/16T";
                if (aValue >= 24 && aValue <= 27) return "1/32D";
                if (aValue >= 28 && aValue <= 35) return "1/16";
                if (aValue >= 36 && aValue <= 43) return "1/8T";
                if (aValue >= 44 && aValue <= 51) return "1/16D";
                if (aValue >= 52 && aValue <= 59) return "1/8";
                if (aValue >= 60 && aValue <= 67) return "1/4T";
                if (aValue >= 68 && aValue <= 75) return "1/8D";
                if (aValue >= 76 && aValue <= 83) return "1/4";
                if (aValue >= 84 && aValue <= 91) return "1/2T";
                if (aValue >= 92 && aValue <= 99) return "1/4D";
                if (aValue >= 100 && aValue <= 107) return "1/2";
                if (aValue >= 108 && aValue <= 111) return "1/1T";
                if (aValue >= 112 && aValue <= 115) return "1/2D";
                if (aValue >= 116 && aValue <= 119) return "1/1";
                if (aValue >= 120 && aValue <= 123) return "1/1D";
                if (aValue >= 124 && aValue <= 127) return "2/1";
                break;
        }
        throw new IllegalArgumentException("Invalid range: " + aRange);
    }

    public static String g2FloatToStr(float aValue, int aLen) {
        int intDigits = String.valueOf(Math.abs((int)aValue)).length();
        int decimals = Math.max(0, aLen - intDigits);

        String formatStr = "%." + decimals + "f";
        String result = String.format(formatStr, aValue);

        if (decimals > 0) {
            result = result.replaceAll("\\.0+$", "");
        }
        return result;
    }


    private static ChangeListener<Integer> mkFreqFormatListener(
            Label l, ObservableValue<Integer> pCoarse, ObservableValue<Integer> pFine, ObservableValue<Integer> pMode) {
        return (cc, o, n) -> {
            StringBuilder result = new StringBuilder();
            final int coarse = pCoarse.getValue();
            final int fine = pFine.getValue();
            switch (pMode.getValue()) {
                case 0 -> { // Semi
                    formatFreq(coarse - 64, result);
                    result.append("  ");
                    formatFreq((fine - 64) * 100 / 128, result);
                }
                case 1 -> { // Freq
                    double exponent = (double) ((coarse - 69) + (fine - 64) / 128) / 12;
                    result.append(formatHz(440.0 * Math.pow(2, exponent)));
                }
                case 2 -> { // Fac
                    double exponent = (double) ((coarse - 64) + (fine - 64) / 128) / 12;
                    result.append(String.format("x%.03f", Math.pow(2, exponent)));
                }
                case 3 -> { // Part
                    if (coarse <= 32) {
                        double Exponent = (double) -(((32 - coarse) * 4) + 77 - (fine - 64) / 128) / 12;
                        result.append(String.format("x%.02fHz", 440.0 * Math.pow(2, Exponent)));
                    } else if (coarse <= 64) {
                        result.append("1:").append(64 - coarse + 1);
                    } else {
                        result.append(coarse - 64 + 1).append(":1");
                    }
                    result.append("  ");
                    formatFreq((fine - 64) * 100 / 128, result);
                }
                case 4 -> { // Semi PShift
                    int cv = coarse - 64;
                    if (cv < 0) {
                        result.append(String.format("%.02f", (double) cv / 4));
                    } else {
                        result.append("+").append(String.format("%.02f", (double) cv / 4));
                    }
                    result.append("  ");
                    formatFreq((fine - 64) * 100 / 128, result);
                }
            }
            l.setText(result.toString());
        };
    }

    private static void formatFreq(int iValue1, StringBuilder result) {
        if (iValue1 < 0) {
            result.append(iValue1);
        } else {
            result.append("+").append(iValue1);
        }
    }
    private <T> Label formatParam(Label l, ObservableValue<T> p, Function<T,String> f) {
        ChangeListener<T> cl = (c, o, n) ->
            l.setText(n != null ? f.apply(n) : "");
        p.addListener(cl);
        Platform.runLater(() -> cl.changed(null,null,p.getValue())); //force UI update
        return l;
    }



}
