package org.g2fx.g2gui.controls;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.ui.UIElements;

import static org.g2fx.g2gui.controls.ListenerBuilder.IntOrBool.Bool;
import static org.g2fx.g2gui.controls.ListenerBuilder.IntOrBool.Int;
import static org.g2fx.g2gui.panel.ModulePane.layout;
import static org.g2fx.g2lib.model.ModParam.computeFltFreq;

public interface Graphs {

    static final double FREQ_MIN = 20;
    static final double FREQ_MAX = 20000;
    static final double DB_MIN = -18; // bottom of graph (dB)
    static final double DB_MAX = 18;   // top of graph (dB)
    double EPSILON = 1e-10;

    /**
     * Approximate Moog Lowpass Filter magnitude response in dB.
     *
     * @param freqHz Frequency to evaluate (Hz)
     * @param cutoffHz Cutoff frequency (Hz)
     * @param resonance Resonance amount (0 to ~4+), controls self-oscillation peak
     * @param order Filter order, typical Moog = 4 (24dB/oct)
     * @return Magnitude response in dB centered so that 0 dB is near graph middle
     */
    public static double lowpassResponseDB2(double freqHz, double cutoffHz, double resonance, int order) {
        double epsilon = EPSILON;
        freqHz = Math.max(freqHz, epsilon);
        cutoffHz = Math.max(cutoffHz, epsilon);

        double ratio = freqHz / cutoffHz;

        // Base magnitude rolloff for N poles
        double mag = 1.0 / Math.sqrt(1 + Math.pow(ratio, 2 * order));

        // Moog-style resonance scaling: Q range ~0 to 4+
        double Q = 0.5 + resonance * 13.0;

        // Resonance peak factor (sharp exponential around cutoff)
        double peakFactor = 1.0 + Q * Math.exp(-5 * Math.abs(ratio - 1.0));
        mag *= peakFactor;

        // Passband attenuation simulating bass loss increasing with resonance
        double gainDrop = 1.0 - 0.15 * resonance;
        mag *= gainDrop;

        double magDB = 20.0 * Math.log10(mag + epsilon);

        // Clip dB to graph display range (-18 to +18 dB)
        double dbMin = -18.0;
        double dbMax = 18.0;
        if (magDB < dbMin) magDB = dbMin;
        if (magDB > dbMax) magDB = dbMax;

        return magDB;
    }

    /**
     * @param resonance 0..4-ish
     * @param order Filter order (e.g. 2 = 12dB/oct, 3 = 18db/oct, 4 = 24dB/oct)
     * @return Magnitude response at freqHz, in dB (attenuation: negative value)
     */
    public static double computeFiltLP(double freqHz, double cutoffHz, double resonance, int order) {

        freqHz = Math.max(freqHz, EPSILON);
        cutoffHz = Math.max(cutoffHz, EPSILON);

        double ratio = freqHz / cutoffHz;

        // Classic lowpass formula for N-pole filter (no resonance):
        double mag = 1.0 / Math.sqrt(1 + Math.pow(ratio, 2 * order));

        // Add resonance peak (approx. for 2 or 4 pole, classic synth style)
        // Q is 1 + resonance * 10 (typical analog scaling, tune for your UI)
        double Q = 1 + resonance * 10.0;
        //double Q = Math.exp(resonance * 2.5);
        //double Q = 0.5 + resonance * 14.0;
        if (order == 2) {
            // 2-pole formula with resonance
            double denom = Math.sqrt(1 + Math.pow(ratio, 4) - 2 * Q * ratio * ratio + Q * Q * Math.pow(ratio, 2));
            mag = 1.0 / denom;
        } else { // if (order == 4) {
            // Simple resonance peak modeling: boost at cutoff (approximation)
            mag *= (1 + resonance * Math.exp(-Math.abs(ratio - 1) * 3));
        }
        // Convert magnitude (linear) to decibels
        double magDB = 20.0 * Math.log10(mag + EPSILON);
        return magDB;
    }

    public static Node mkFltClassicGraph(UIElements.Graph c, ModulePane parent) {
        Canvas canvas = new Canvas(c.Width(), c.Height());
        ListenerBuilder.build(vs -> {
            double cutoff = computeFltFreq(vs.get(0).getInt());
            double resonance = (double) (vs.get(1).getInt() * 4) /127;
            int order = vs.get(2).getInt()+1;
            //TODO active monitor
            drawFilterCurve(c,canvas.getGraphicsContext2D(), cutoff, resonance, order); // 1.05kHz, no resonance, 24dB/oct
        },parent,c,Int,Int,Int,Bool);

        Pane pane = new Pane(canvas);
        pane.setPrefSize(c.Width(), c.Height());
        layout(c,pane);
        return pane;
    }

    private static void drawFilterCurve(UIElements.Graph c, GraphicsContext gc, double cutoff, double resonance, int order) {
        gc.setFill(Color.web("#13e55e")); // Custom green fill (approximate)
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

}
