package org.g2fx.g2gui.controls;

import javafx.animation.PauseTransition;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.g2fx.g2gui.ui.UIElements;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.panel.ModulePane.layout;

public class VuMeter {

    private static final int LEVELS = 10; // inclusive
    public static final double WIDTH = 5;
    public static final double HEIGHT = 13;
    public static final Color UNLIT_GREEN = Color.web("#005500");
    public static final Color LIT_GREEN = Color.web("#00ff00");
    public static final Color UNLIT_YELLOW = Color.web("#555500");
    public static final Color LIT_YELLOW = Color.web("#ffff00");
    public static final Color UNLIT_RED = Color.web("#550000");
    public static final Color LIT_RED = Color.web("#ff0000");
    public static final int GREEN_LIMIT = 7;  // inclusive
    private final Property<Integer> level = new SimpleObjectProperty<>(0);
    private final HBox control;
    private int peakLevel = 0;
    private final PauseTransition peakResetTimer = new PauseTransition(Duration.seconds(1));

    private final Canvas canvas = new Canvas(WIDTH, HEIGHT);

    public VuMeter(UIElements.MiniVU c) {
        level.addListener((obs, oldVal, newVal) -> {
            if (newVal > peakLevel) {
                peakLevel = newVal;
                peakResetTimer.stop();
            } else if (newVal < peakLevel) {
                peakResetTimer.playFromStart();
            }
            draw();
        });
        peakResetTimer.setOnFinished(e -> {
            peakLevel = 0;
            draw();
        });
        draw();
        control = withClass(new HBox(canvas),"vu-box");
        layout(c,control);
    }

    public void setLevel(int value) {
        if (value != level.getValue()) {
            level.setValue(value);
            if (value > peakLevel) {
                peakLevel = value;
                peakResetTimer.stop();
            } else if (value < peakLevel) {
                peakResetTimer.playFromStart();
            }
        }
    }

    /*
     * Osc -> LevAmp -> Out, LevAmp param (lin, db) to vu unit
     * 0 (x0,-inf) - 0
     * 64 (x1,0dB) - 7 (green limit)
     * (flashing yellow)
     * 68 (x1.09,0.8dB) - 8 (solid yellow)
     * 97 (x2.05,6.2dB) - 9 (solid)
     * 122 (x3.58,11.1dB) - red (solid)
     */

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.TRANSPARENT);

        double unitHeight = HEIGHT/(LEVELS+3);
        double y = canvas.getHeight();
        for (int i = 1; i <= LEVELS; i++) {
            Color base, lit;
            double h;
            if (i <= GREEN_LIMIT) { // green
                base = UNLIT_GREEN;
                lit = LIT_GREEN;
                h = unitHeight;
            } else if (i <= 9) { // yellow
                base = UNLIT_YELLOW;
                lit = LIT_YELLOW;
                h = unitHeight*2;
            } else { // red
                base = UNLIT_RED;
                lit = LIT_RED;
                h = unitHeight*2;
            }
            y -= h;

            int value = level.getValue();
            gc.setFill(i <= value ? lit : base);
            gc.fillRect(0, y, WIDTH, h);

            if (i == peakLevel && peakLevel > GREEN_LIMIT && value < peakLevel) {
                gc.setFill(lit);
                gc.fillRect(0, y, WIDTH, h);
            }

            gc.setFill(Color.TRANSPARENT);
            gc.setStroke(Color.rgb(0,0,0,.03));
            gc.setLineWidth(2);
            gc.beginPath();
            gc.moveTo(0,1);
            gc.lineTo(WIDTH,1);
            gc.lineTo(WIDTH,HEIGHT);
            gc.lineTo(0,HEIGHT);
            gc.stroke();
        }
    }

    public Property<Integer> level() { return level; }

    public Node getControl() { return control; }

}
