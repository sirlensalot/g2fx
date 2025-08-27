package org.g2fx.g2gui.controls;

import com.google.common.collect.Streams;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import org.controlsfx.control.SegmentedButton;

import java.util.List;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.controls.ModulePane.layout;
import static org.g2fx.g2gui.controls.UIElements.Orientation.Vertical;

public class ButtonRadio {

    private final SegmentedButton button;
    private final Property<Integer> selectedToggleIndexProperty;

    public ButtonRadio(ModulePane parent, UIElements.ButtonRadio c, ModulePane.IndexParam ip) {
            List<TextOrImage> ss = c.Text().isEmpty() ?
                    TextOrImage.mkImages(c.Images()) : TextOrImage.mkTexts(c.Text());
            ObservableList<ToggleButton> buttons =
                    FXCollections.observableArrayList(Streams.mapWithIndex(ss.stream(),(si, ix) -> {
                        ToggleButton tb = withClass(new ToggleButton(),"button-radio-button","g2-toggle");
                        assert si != null; // sigh ... idea doesn't bug me for normal map?!?
                        Node g = withClass(switch (si) {
                            case TextOrImage.IsText t -> new Label(t.text());
                            case TextOrImage.IsImage i -> i.image();
                        },"button-radio-graphic");
                        if (c.Orientation()==Vertical) {
                            g.setRotate(-90);
                            tb.setPrefHeight(c.ButtonWidth()-1);
                            tb.setMaxWidth(13);
                        } else {
                            tb.setPrefWidth(c.ButtonWidth()-1);
                        }
                        tb.setGraphic(g);
                        tb.setUserData(Long.valueOf(ix).intValue());
                        tb.setPrefWidth(c.ButtonWidth()-1);
                        return tb;
                    }).toList());

        button = layout(c,withClass(new SegmentedButton(buttons), "module-button-radio"));
            if (c.Orientation() == Vertical) {
                button.setRotate(90);
                Platform.runLater(()-> {
                    Bounds bs = button.getBoundsInParent(); // run later to get actual location
                    button.setTranslateX(c.XPos()-bs.getMinX());
                    button.setTranslateY(c.YPos()-bs.getMinY());
                });
            }
        selectedToggleIndexProperty = new SimpleObjectProperty<>(button,
                parent.getType() + ":" + parent.getIndex() + ":" + ip.index() + ":" + ip.param().param().name(),
                ip.param().param().def);
            button.getToggleGroup().selectToggle(
                    buttons.get(selectedToggleIndexProperty.getValue()));
            Platform.runLater(()-> {
                button.getToggleGroup().selectToggle(
                        buttons.get(selectedToggleIndexProperty.getValue())); //TODO, this could potentially overlap with init value set?
            });
            button.getToggleGroup().selectedToggleProperty().addListener((cc, o, n) ->
                    selectedToggleIndexProperty.setValue((Integer) n.getUserData()));
            selectedToggleIndexProperty.addListener((cc, o, n) ->
                    buttons.get(n).setSelected(true));


    }

    public SegmentedButton getButton() {
        return button;
    }

    public Property<Integer> selectedToggleIndexProperty() {
        return selectedToggleIndexProperty;
    }
}
