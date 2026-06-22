package org.g2fx.g2gui.controls;

import javafx.scene.paint.Color;
import org.g2fx.g2lib.util.SafeLookup;

public enum CableColor {

    Red(355),
    Blue(210),
    Yellow(60),
    Orange(30),
    Green(120),
    Purple(270),
    White(0,0,1,0.6);

    private final double hue;
    private final double sat;
    private final double fillB;
    private final double edgeB;

    CableColor(double hue) {
        this(hue,0.6,1,0.6);
    }
    CableColor(double hue, double sat, double fillB, double edgeB) {
        this.hue = hue;
        this.sat = sat;
        this.fillB = fillB;
        this.edgeB = edgeB;
    }

    public static final SafeLookup<Integer,CableColor> LOOKUP = SafeLookup.makeEnumOrdLookup(values());

    public Color getFill() {
        return getColor(fillB);
    }

    public Color getEdge() {
        return getColor(edgeB);
    }

    public Color getColor(double b) {
        return Color.hsb(hue, sat, b);
    }

}
