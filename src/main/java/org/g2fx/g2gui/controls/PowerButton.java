package org.g2fx.g2gui.controls;

import javafx.scene.control.ToggleButton;
import javafx.scene.shape.SVGPath;

import static org.g2fx.g2gui.FXUtil.withClass;

public class PowerButton {

    private final ToggleButton button;

    public PowerButton() {
        SVGPath powerGraphic = withClass(new SVGPath(),"power-graphic");
        powerGraphic.setContent("M -3 -3 A 4.5 4.5 0 1 0 3 -3 M 0 0 L 0 -4");
        button = withClass(new ToggleButton("", powerGraphic),"power-button");
    }

    public ToggleButton getButton() {
        return button;
    }

}
