package org.g2fx.g2gui.controls;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2lib.model.ParamFormatter;
import org.g2fx.g2lib.util.Util;

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
    private static final int TF_DELAY_TIME_CLK = 143;
    private static final int TF_DELAY_TIME_STEREO = 146;

    private static final int TF_REVERB_TIME = 107;



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
            case TF_DELAY_TIME_CLK: return formatDelayTimeClk(c,l);
            case TF_DELAY_TIME_STEREO: return formatDelayTimeStereo(c,l);
            case TF_REVERB_TIME: return formatReverbTime(c,l);
        }

        System.out.format("%s, pi: %s, pb: %s\n",ip,pi,pb);
        return parent.empty(c,"mkTextField");

    }

    private Node formatReverbTime(UIElements.TextField c, Label l) {
        return fmtInt2(l,c, (n, t)-> {
            double m = (t + 1) * 3.0;
            double v = m / 127 * n;
            return v < 1 ? ParamTimes.fmtDoubleFixed(v*1000, 4) + "ms" :
                    ParamTimes.fmtDoubleFixed(v, 5) + 's';
        });
    }

    private Node formatDelayTimeStereo(UIElements.TextField c, Label l) {
        return fmtInt3(l,c,(val,type,range) ->
                type == 0 ? ParamTimes.fmtDelayRange3(range,val) :
                        ParamTimes.formatClkDelay(val));
    }
    private Node formatDelayTimeClk(UIElements.TextField c, Label l) {
        return fmtInt3(l,c,(val,type,range) ->
                type == 0 ? ParamTimes.fmtDelayRange4(range,val) :
                        ParamTimes.formatClkDelay(val));
    }

    private Node formatDelayTime(UIElements.TextField c, Label l) {
        return fmtInt2(l,c, ParamTimes::formatDelayRange7);
    }

    private Node formatMixLev(UIElements.TextField c, Label l) {
        return fmtInt2(l,c, (n, t) ->
                t == 2 ? aref(n,MIX_LEV_DB, this::fmtNegInf) :
                        fmt01f(n==127?100:((double) n * 100) / 128));
    }

    private Node formatConstBip(UIElements.TextField c, Label l) {
        return fmtInt2(l,c, (n, t) ->
                t == 0 ? (Integer.toString(n==127?64:n-64)) :
                        fmt01f(n==127?64.0:((double) n) / 2));
    }

    private Node formatLevAmp(UIElements.TextField c, Label l) {
        return fmtInt2(l,c, (n, t) ->
                t == 0 ? String.format("x%.02f", 4 * ((double) n) / 127) :
                        aref(n, LEV_AMP_DB, this::fmtNegInf));
    }

    private String fmtNegInf(Double v) {
        return v == Double.NEGATIVE_INFINITY ? "-∞" : fmt01f(v);
    }

    private Node fmtInt2(Label l, UIElements.TextField tf,
                         BiFunction<Integer, Integer, String> f) {
        ObservableValue<Integer> p0 = parent.resolveDepParam(tf, 0);
        ObservableValue<Integer> p1 = parent.resolveDepParam(tf, 1);
        ChangeListener<Integer> listener = (cc,oo,nn) ->
                l.setText(f.apply(p0.getValue(), p1.getValue()));
        p0.addListener(listener);
        p1.addListener(listener);
        return l;
    }

    private Node fmtInt3(Label l, UIElements.TextField tf,
                         Util.TriFunction<Integer, Integer, Integer, String> f) {
        ObservableValue<Integer> p0 = parent.resolveDepParam(tf,0);
        ObservableValue<Integer> p1 = parent.resolveDepParam(tf,1);
        ObservableValue<Integer> p2 = parent.resolveDepParam(tf,2);
        ChangeListener<Integer> listener = (cc,oo,nn) ->
                l.setText(f.apply(p0.getValue(), p1.getValue(), p2.getValue()));
        p0.addListener(listener);
        p1.addListener(listener);
        p2.addListener(listener);
        return l;
    }
    private Node formatPshiftFreq(UIElements.TextField c, Label l) {
        return fmtInt2(l,c,(coarse,fine) -> formatFreq(4,coarse,fine));
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

    private Node formatOscFreq(UIElements.TextField c, Label l) {
        return fmtInt3(l,c,(coarse,fine,mode) -> formatFreq(mode,coarse,fine));
    }


    private static String formatFreq(int mode, int coarse, int fine) {
        StringBuilder result = new StringBuilder();
        switch (mode) {
            case 0 -> { // Semi
                fmtPosNeg(coarse - 64, result);
                result.append("  ");
                fmtPosNeg((fine - 64) * 100 / 128, result);
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
                fmtPosNeg((fine - 64) * 100 / 128, result);
            }
            case 4 -> { // Semi PShift
                int cv = coarse - 64;
                if (cv < 0) {
                    result.append(String.format("%.02f", (double) cv / 4));
                } else {
                    result.append("+").append(String.format("%.02f", (double) cv / 4));
                }
                result.append("  ");
                fmtPosNeg((fine - 64) * 100 / 128, result);
            }
        }
        String s = result.toString();
        return s;
    }

    private static void fmtPosNeg(int iValue1, StringBuilder result) {
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
