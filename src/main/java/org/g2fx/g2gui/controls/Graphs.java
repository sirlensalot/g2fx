package org.g2fx.g2gui.controls;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2lib.model.ModuleType;

import static org.g2fx.g2gui.controls.ParamListener.pBool;
import static org.g2fx.g2gui.controls.ParamListener.pInt;
import static org.g2fx.g2gui.panel.ModulePane.layout;
import static org.g2fx.g2lib.model.ModParam.*;

public class Graphs {

    public static double FREQ_MIN = 20;
    public static double FREQ_MAX = 20000;
    public static double DB_MIN = -24; // bottom of graph (dB)
    public static double DB_MAX = 18;   // top of graph (dB)
    public static double EPSILON = 1e-10;

    private final ParamListener paramListener;
    private final ModuleType type;

    public Graphs(ParamListener paramListener, ModuleType type) {
        this.paramListener = paramListener;
        this.type = type;
    }

    public Node mkGraph(UIElements.Graph c) {
        return switch (c.GraphFunc()) {
            case 20 -> mkFltClassicGraph(c);
            case 3 -> mkADSR(c);
            case 23 -> mkADDSR(c);
            case 1 -> mkADR(c);
            case 6 -> mkD(c);
            case 7  -> mkH(c);
            case 17 -> mkEnvMulti(c);
            case 28 -> type == ModuleType.M_EnvAHD ? mkAHD(c) : paramListener.empty(c, "Env_ModAHD");
            default -> emptyGraph(c);
        };
    }

    private Node emptyGraph(UIElements.Graph c) {
        Label l = new Label("Graph " + c.GraphFunc());
        l.setPrefWidth(c.Width());
        l.setPrefHeight(c.Height());
        l.setBorder(Border.stroke(Color.GREEN));
        layout(c,l);
        paramListener.empty(c,"Graph");
        return l;
    }

    /**
     * @param resonance 0..4-ish
     * @param order Filter order (e.g. 2 = 12dB/oct, 3 = 18db/oct, 4 = 24dB/oct)
     * @return Magnitude response at freqHz
     */
    private static double computeFiltLP(double freqHz, double cutoffHz, double resonance, int order) {

        freqHz = Math.max(freqHz, EPSILON);
        cutoffHz = Math.max(cutoffHz, EPSILON);

        double ratio = freqHz / cutoffHz;

        // lowpass N-pole filter w/o resonance
        double mag = 1.0 / Math.sqrt(1 + Math.pow(ratio, 2 * order));

        // add resonance
        mag *= (1 + resonance * Math.exp(-Math.abs(ratio - 1) * 3));

        double magDB = 20.0 * Math.log10(mag + EPSILON);

        return magDB;
    }

    private Node mkFltClassicGraph(UIElements.Graph c) {
        Canvas canvas = new Canvas(c.Width(), c.Height());
        paramListener.build(c, vs -> {

            double cutoff = computeFltFreq(vs.getInt(FltFreq));
            double resonance = vs.getInt(Res_1) * 4 / 127.0;
            int order = vs.getInt(ClassicSlope) + 2;

            drawFilterCurve(c,canvas.getGraphicsContext2D(), cutoff, resonance, order, vs.getBool(ActiveMonitor));

        }, pInt(FltFreq),pInt(Res_1),pInt(ClassicSlope),pBool(ActiveMonitor)); //Res_1 is NOT the param, but fine for param key here

        Pane pane = new Pane(canvas);
        pane.setPrefSize(c.Width(), c.Height());
        layout(c,pane);

        return pane;
    }

    private static void drawFilterCurve(UIElements.Graph c, GraphicsContext gc, double cutoff, double resonance, int order, boolean active) {
        gc.setFill(Color.web("#377e7f"));
        gc.fillRect(0, 0, c.Width(), c.Height());

        double zeroDbNorm = (0 - DB_MIN) / (DB_MAX - DB_MIN);
        double yZeroDb = c.Height() * (1.0 - zeroDbNorm);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeLine(0, yZeroDb, c.Width(), yZeroDb);

        gc.setFill(Color.YELLOW);
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));
        String orderText = order * 6 + "";
        gc.fillText(orderText, c.Width() - 15, 10);

        if (!active) return;

        gc.setFill(Color.web("#75fb8e"));
        gc.setStroke(Color.BLACK); // Curve border
        gc.setLineWidth(1);
        double[] xs = new double[c.Width()];
        double[] ys = new double[c.Width()];

        for (int i = 0; i < c.Width(); i++) {
            // Logarithmic frequency scale
            double frac = i / (double)(c.Width() - 1);
            double freq = FREQ_MIN * Math.pow(FREQ_MAX / FREQ_MIN, frac);

            double dB = computeFiltLP(freq, cutoff, resonance, order);

            // Map dB to Y (0=top, c.Height()=bottom)
            double yNorm = (dB - DB_MAX) / (DB_MIN - DB_MAX); // 0...1 (top...bottom)
            yNorm = Math.min(Math.max(yNorm, 0.0), 1.0);
            xs[i] = i;
            ys[i] = yNorm * c.Height();
        }

        // Fill area under curve for the classic look
        gc.beginPath();
        gc.moveTo(xs[0], c.Height());
        for (int i = 0; i < c.Width(); i++)
            gc.lineTo(xs[i], ys[i]);
        gc.lineTo(xs[c.Width()-1], c.Height());
        gc.closePath();
        gc.fill();

        // Draw curve border
        gc.beginPath();
        for (int i = 0; i < c.Width(); i++)
            gc.lineTo(xs[i], ys[i]);
        gc.stroke();
    }


    private Node mkADSR(UIElements.Graph c) {
        // mod: shape is 0
        EnvGraphs.Graph g = EnvGraphs.mkEnvGraphPane(c);
        paramListener.build(c,vs ->
                        EnvGraphs.adsrGraph(g,
                                vs.getInt(EnvShape_3),
                                vs.getInt("A"),
                                vs.getInt("D"),
                                vs.getInt("S"),
                                vs.getInt("R"),
                                vs.getInt(PosNegInvBipInv)),
                pInt("A"),pInt("D"),pInt("S"),pInt("R"),pInt(EnvShape_3), pInt(PosNegInvBipInv));
        return g.control();

    }

    private Node mkADDSR(UIElements.Graph c) {
        EnvGraphs.Graph g = EnvGraphs.mkEnvGraphPane(c);
        paramListener.build(c,vs ->
                        EnvGraphs.addsrGraph(g,
                                vs.getInt(EnvShape_3),
                                vs.getInt("A"),
                                vs.getInt("D1"),
                                vs.getInt("L1"),
                                vs.getInt("D2"),
                                vs.getInt("L2"),
                                vs.getInt("R"),
                                vs.getInt(SustainMode_1),
                                vs.getInt(PosNegInvBipInv)),
                pInt("A"),
                pInt("D1"),pInt("L1"),
                pInt("D2"),pInt("L2"),pInt("R"),
                pInt(EnvShape_3),pInt(SustainMode_1), pInt(PosNegInvBipInv)
        );
        return g.control();

    }

    private Node mkEnvMulti(UIElements.Graph c) {
        EnvGraphs.Graph g = EnvGraphs.mkEnvGraphPane(c);
        paramListener.build(c,vs ->
                        EnvGraphs.multiEnvGraph(g,
                                vs.getInt("L1"),
                                vs.getInt("L2"),
                                vs.getInt("L3"),
                                vs.getInt("L4"),
                                vs.getInt("T1"),
                                vs.getInt("T2"),
                                vs.getInt("T3"),
                                vs.getInt("T4"),
                                vs.getInt(SustainMode_2),
                                vs.getInt(PosNegInvBip),
                                vs.getInt(EnvShape_3)),
                pInt("L1"),pInt("L2"),pInt("L3"),pInt("L4"),
                pInt("T1"),pInt("T2"),pInt("T3"),pInt("T4"),
                pInt(SustainMode_2),pInt(PosNegInvBip),pInt(EnvShape_3));
        return g.control();
    }

    private Node mkAHD(UIElements.Graph c) {
        //Mod: shape is 0
        EnvGraphs.Graph g = EnvGraphs.mkEnvGraphPane(c);
        paramListener.build(c,vs ->
                        EnvGraphs.ahdGraph(g,
                                vs.getInt("A"),
                                vs.getInt("H"),
                                vs.getInt("D"),
                                vs.getInt(EnvShape_3),
                                vs.getInt(PosNegInv)),
                pInt("A"),pInt("H"),pInt("D"),pInt(EnvShape_3), pInt(PosNegInv));
        return g.control();

    }

    private Node mkADR(UIElements.Graph c) {
        EnvGraphs.Graph g = EnvGraphs.mkEnvGraphPane(c);
        paramListener.build(c,vs ->
                        EnvGraphs.adrGraph(g,
                                vs.getInt(EnvShape_3),
                                vs.getInt("A"),
                                vs.getInt("R"),
                                vs.getInt(PosNegInv),
                                vs.getInt(AdAr)),
                pInt("A"),pInt("R"),pInt(EnvShape_3),pInt(AdAr), pInt(PosNegInv));
        return g.control();

    }

    private Node mkD(UIElements.Graph c) {
        EnvGraphs.Graph g = EnvGraphs.mkEnvGraphPane(c);
        paramListener.build(c,vs ->
                        EnvGraphs.denvGraph(g,
                                vs.getInt(EnvTime),
                                vs.getInt(PosNegInv)),
                pInt(EnvTime),pInt(PosNegInv));
        return g.control();
    }

    private Node mkH(UIElements.Graph c) {
        EnvGraphs.Graph g = EnvGraphs.mkEnvGraphPane(c);
        paramListener.build(c,vs ->
                        EnvGraphs.henvGraph(g,
                                vs.getInt(EnvTime),
                                vs.getInt(PosNegInv)),
                pInt(EnvTime),pInt(PosNegInv));
        return g.control();
    }
}
