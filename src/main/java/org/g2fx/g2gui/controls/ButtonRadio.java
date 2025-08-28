package org.g2fx.g2gui.controls;

import com.google.common.collect.Streams;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.bridge.FxProperty;
import org.g2fx.g2gui.bridge.Iso;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2gui.panel.ModulePane;

import java.util.ArrayList;
import java.util.List;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.panel.ModulePane.layout;
import static org.g2fx.g2gui.ui.UIElements.Orientation.Vertical;

public class ButtonRadio {

    public static final int EDIT_WIDTH = 41;
    public static final int EDIT_HEIGHT = 12;
    private final Node control;
    private final Property<Integer> selectedToggleIndexProperty;

    /**
     * Ctor for "ButtonRadio": not editable, image or text, single row
     */
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

    /**
     * Ctor for "ButtonRadioEdit": text only, editable, multiple rows
     */
    public ButtonRadio(ModulePane parent, UIElements.ButtonRadioEdit c, ModulePane.IndexParam ip) {

        ToggleGroup toggleGroup = new ToggleGroup();
        Pane box = new Pane();
        List<ToggleButton> buttons = new ArrayList<>(c.Text().size());
        for (int row = 0; row < c.ButtonRows(); row++) {
            for (int col = 0; col < c.ButtonColumns(); col++) {
                int ix = row * c.ButtonColumns() + col;
                String text = c.Text().get(ix);
                ToggleButton tb = withClass(new ToggleButton(text), "button-radio-button", "g2-toggle",
                        col == 0 ? "left-pill" : col + 1 < c.ButtonColumns() ? "center-pill" : "right-pill");
                tb.setUserData(Long.valueOf(ix).intValue());
                tb.setPrefWidth(EDIT_WIDTH);
                tb.setToggleGroup(toggleGroup);
                tb.setViewOrder(5);
                buttons.add(tb);
                TextField editor = parent.makeButtonEditField(tb);
                double x = EDIT_WIDTH * col;
                tb.setLayoutX(x);
                editor.setLayoutX(x);
                double y = EDIT_HEIGHT * row;
                tb.setLayoutY(y);
                editor.setLayoutY(y);
                box.getChildren().addAll(tb,editor);
                parent.addBridge(parent.getBridges().bridge(d ->
                                parent.getPatchModule().getModuleLabels(ip.index()).get(ix),
                        FxProperty.adaptReadOnly(tb.textProperty(), tb::setText),
                        Iso.id()
                ));
            }
        }
        layout(c,box,new Point2D(1,2));
        control = box;
        selectedToggleIndexProperty = setupProperty(parent, ip, toggleGroup, buttons);
    }

    private Property<Integer> setupProperty(ModulePane parent, ModulePane.IndexParam ip, ToggleGroup toggleGroup,
                                            List<ToggleButton> buttons) {
        Property<Integer> property = new SimpleObjectProperty<>(control,
                parent.paramId(ip),
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
