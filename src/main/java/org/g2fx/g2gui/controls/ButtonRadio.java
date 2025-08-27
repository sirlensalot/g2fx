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
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.FXUtil;

import java.util.ArrayList;
import java.util.List;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.controls.ModulePane.layout;
import static org.g2fx.g2gui.controls.UIElements.Orientation.Vertical;

public class ButtonRadio {

    private final Node control;
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

        SegmentedButton button = layout(c,withClass(new SegmentedButton(buttons), "module-button-radio"));
        control = button;
        if (c.Orientation() == Vertical) {
            button.setRotate(90);
            Platform.runLater(()-> {
                Bounds bs = button.getBoundsInParent(); // run later to get actual location
                button.setTranslateX(c.XPos()-bs.getMinX());
                button.setTranslateY(c.YPos()-bs.getMinY());
            });
        }
        ToggleGroup toggleGroup = button.getToggleGroup();
        selectedToggleIndexProperty = setupProperty(parent, ip, toggleGroup, buttons);
    }

    public ButtonRadio(ModulePane parent, UIElements.ButtonRadioEdit c, ModulePane.IndexParam ip) {

        ObservableList<ToggleButton> buttons =
                FXCollections.observableArrayList(Streams.mapWithIndex(c.Text().stream(),(si, ix) -> {
                    ToggleButton tb = withClass(new ToggleButton(si),"button-radio-button","g2-toggle");
                    tb.setUserData(Long.valueOf(ix).intValue());
                    tb.setMinWidth(40);
                    return tb;
                }).toList());

        //editable TODO

        ToggleGroup toggleGroup;
        if (c.ButtonRows() == 1) {
            SegmentedButton button = layout(c, withClass(new SegmentedButton(buttons), "module-button-radio"));
            control = button;
            toggleGroup = button.getToggleGroup();
        } else {
            List<SegmentedButton> sbs = new ArrayList<>();
            toggleGroup = new ToggleGroup();
            for (int row = 0; row < c.ButtonRows(); row++) {
                ObservableList<ToggleButton> tbs = FXCollections.observableArrayList();
                for (int col = 0; col < c.ButtonColumns(); col++) {
                    ToggleButton tb = buttons.get(row * c.ButtonColumns() + col);
                    tbs.add(tb);
                    toggleGroup.getToggles().add(tb);
                }
                SegmentedButton sb = layout(c, withClass(new SegmentedButton(tbs), "module-button-radio"));
                sb.setToggleGroup(toggleGroup);
                sbs.add(sb);
            }
            control = withClass(FXUtil.addChildren(new VBox(),sbs),"module-button-radio-box");
        }
        layout(c,control);
        selectedToggleIndexProperty = setupProperty(parent, ip, toggleGroup, buttons);
    }

    private Property<Integer> setupProperty(ModulePane parent, ModulePane.IndexParam ip, ToggleGroup toggleGroup,
                                            ObservableList<ToggleButton> buttons) {
        Property<Integer> property = new SimpleObjectProperty<>(control,
                parent.getType() + ":" + parent.getIndex() + ":" + ip.index() + ":" + ip.param().param().name(),
                ip.param().param().def);
        toggleGroup.selectToggle(
                buttons.get(property.getValue()));
        Platform.runLater(()-> {
            toggleGroup.selectToggle(
                    buttons.get(property.getValue())); //TODO, this could potentially overlap with init value set?
        });
        toggleGroup.selectedToggleProperty().addListener((cc, o, n) ->
                property.setValue((Integer) n.getUserData()));
        property.addListener((cc, o, n) ->
                buttons.get(n).setSelected(true));
        return property;
    }

    public Node getControl() {
        return control;
    }

    public Property<Integer> selectedToggleIndexProperty() {
        return selectedToggleIndexProperty;
    }
}
