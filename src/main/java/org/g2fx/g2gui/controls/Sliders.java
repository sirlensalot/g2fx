package org.g2fx.g2gui.controls;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2lib.model.ModParam;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.panel.ModulePane.IndexParam;
import static org.g2fx.g2gui.panel.ModulePane.layout;

public interface Sliders {

    public record ModuleSlider(Property<Integer> property, Slider slider, Node control) {}

    public static ModuleSlider mkSlider(UIElements.Knob c, IndexParam ip, ModulePane parent) {
        ModParam mp = ip.param().param();
        Slider slider = withClass(new Slider(mp.min,mp.max,mp.def),"module-slider");
        slider.setUserData(parent.paramId(ip));
        slider.setMajorTickUnit(1.0);
        slider.setOrientation(Orientation.VERTICAL);
        Button left = withClass(new Button("◀"),"module-slider-button");
        Button right = withClass(new Button("▶"),"module-slider-button");
        VBox control = withClass(layout(c,new VBox(slider, new HBox(left, right)),new Point2D(-2,-3)),"module-slider-box");
        control.setAlignment(Pos.CENTER);
        left.setOnAction(e -> slider.decrement());
        right.setOnAction(e -> slider.increment());
        var pi = new SimpleObjectProperty<>(slider, parent.paramId(ip), mp.def);
        pi.addListener((cc,o,n) -> slider.valueProperty().set(n));
        slider.valueProperty().addListener((cc,o,n)->pi.setValue(n.intValue()));
        slider.valueProperty().set(pi.getValue());
        Platform.runLater(()-> {
            slider.valueProperty().set(pi.getValue());
        });
        return new ModuleSlider(pi,slider,control);
    }
}
