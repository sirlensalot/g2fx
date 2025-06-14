package org.g2fx.g2gui.controls;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import static org.g2fx.g2gui.FXUtil.withClass;


public class LoadMeter extends Control {

    private final SimpleDoubleProperty value;
    private final Pane meterIndicator;
    private final Label valueLabel;

    public LoadMeter(String name) {
        super();
        this.value = new SimpleDoubleProperty(this,name,0.0);
        valueLabel = withClass(new Label("0.0"),"load-meter-value");
        Pane meterBkgd = withClass(new Pane(),"load-meter-background");
        meterIndicator = withClass(new Pane(),"load-meter-indicator");
        StackPane meterPane = withClass(
                new StackPane(meterBkgd, meterIndicator),"load-meter-pane");
        VBox meterBox = withClass(new VBox(valueLabel,meterPane),"load-meter-box");
        getChildren().add(meterBox);
        setValue(50.345);
    }

    @Override
    protected Skin<LoadMeter> createDefaultSkin() {
        return new SkinBase<>(this) {};
    }

    public double getValue() {
        return value.get();
    }

    public void setValue(double value) {
        this.value.set(value);
        double pctg = value/100.0;
        double width = Math.max(0.0,Math.min(30.0,pctg*30));
        meterIndicator.setMaxWidth(width);
        valueLabel.setText(String.format("%.1f",value));
    }

    public SimpleDoubleProperty getValueProperty() {
        return value;
    }
}
