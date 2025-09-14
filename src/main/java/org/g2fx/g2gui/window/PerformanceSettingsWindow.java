package org.g2fx.g2gui.window;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2gui.G2GuiApplication;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.bridge.FxProperty;
import org.g2fx.g2gui.bridge.Iso;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ParamConstants;
import org.g2fx.g2lib.state.Slot;
import org.g2fx.g2lib.state.SlotSettings;

import java.util.function.Function;

import static org.g2fx.g2gui.FXUtil.*;
import static org.g2fx.g2gui.G2GuiApplication.addGlobalStylesheet;

public class PerformanceSettingsWindow implements G2Window {

    public static final TextFieldFocusListener NULL_FOCUS_LISTENER = b -> {
    };
    private final Stage stage;
    private final Pane root;
    private final Bridges bridges;

    public PerformanceSettingsWindow(Bridges bridges) {

        this.bridges = bridges;
        GridPane grid = new GridPane(8, 4);
        root = withClass1("perfs-root",new VBox(4,
                withClass1("perfs-topbar",new HBox(4,
                        withClass1("perfs-name-box",new VBox(
                                plabel("Name"),
                                perfNameField()
                        )),
                        withClass1("perfs-clock-pane", titledPane("Master Clock",withClass1("perfs-clock-bar",new HBox(4,
                                plabel("Rate (BPM)"),
                                withClass1("perfs-clock-spinner",G2GuiApplication.mkClockSpinner(bridges)),
                                withClass1("perfs-clock-run-button",G2GuiApplication.mkClockRunButton(bridges))
                        ))))
                )),
                withClass1("perfs-slot-pane",titledPane("Slots",
                        withClass1("perfs-slot-grid", grid)))
                ));
        grid.add(withClass1("perfs-keyrange-bar",new HBox(4,
                keybRange(checkbox()),
                plabel("Keyboard Range")
        )),3,0,3,1);
        grid.addRow(1,plabel("Enable"),plabel("Keyboard"),plabel("Hold"),plabel("Lower"),plabel("Upper"));
        for (Slot slot : Slot.values()) {
            TextField rangeLow = new TextField();
            TextField rangeHi = new TextField();
            grid.addRow(slot.ordinal()+2,
                    new HBox(4,
                            bridgeSlotBox(checkbox(),slot,SlotSettings::enabled),
                            plabel("Slot " + slot)),
                    bridgeSlotBox(checkbox(),slot,SlotSettings::keyboard),
                    bridgeSlotBox(checkbox(),slot,SlotSettings::hold),
                    withClass1("perfs-range-textfield",
                            bridgeRangeField(rangeLow,slot,SlotSettings::keyboardRangeFrom)),
                    withClass1("perfs-range-textfield",
                            bridgeRangeField(rangeHi,slot,SlotSettings::keyboardRangeTo)),
                    rangeField(plabel("C-1 - B3"),rangeLow,rangeHi)
                    );
        }

        stage = new Stage();
        stage.setTitle("Performance Settings");
        stage.setScene(addGlobalStylesheet(new Scene(root)));
        stage.setWidth(330);
        stage.setHeight(280);

        trackWinSizeInTitle(stage,"Performance Settings");

        stage.show();
    }

    private Node rangeField(Label l, TextField rangeLow, TextField rangeHi) {
        ChangeListener<String> listener = (c,o,n) -> {
            l.setText(String.format("%s - %s",toMidi(rangeLow.getText()),toMidi(rangeHi.getText())));
        };
        rangeLow.textProperty().addListener(listener);
        rangeHi.textProperty().addListener(listener);
        return l;
    }

    private String toMidi(String s) {
        int i = parseInt(s);
        int oct = i/12 - 1;
        String kn = ParamConstants.KEY_NAMES[i % 12];
        return String.format("%s%d",kn,oct);
    }

    private Node bridgeRangeField(TextField textField, Slot slot, Function<SlotSettings, LibProperty<Integer>> f) {
        SimpleStringProperty property = mkTextFieldCommitProperty(textField, NULL_FOCUS_LISTENER, 3);
        bridges.bridge(d -> f.apply(d.getPerf().getPerfSettings().getSlotSettings(slot)),
                new FxProperty.SimpleFxProperty<>(property),
                new Iso<>() {
                    @Override
                    public String to(Integer i) {
                        return i.toString();
                    }

                    @Override
                    public Integer from(String s) {
                        return parseInt(s);
                    }
                });
        return textField;
    }

    private static int parseInt(String s) {
        if (s == null || s.isEmpty()) { return 0; }
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            System.out.println("TODO need better range text field validation: " + s + ", " + e);
            return 0;
        }
    }

    private Node bridgeSlotBox(CheckBox checkbox, Slot slot, Function<SlotSettings, LibProperty<Boolean>> f) {
        bridges.bridge(checkbox.selectedProperty(),d ->
                f.apply(d.getPerf().getPerfSettings().getSlotSettings(slot)));
        return checkbox;
    }

    private Node keybRange(CheckBox checkbox) {
        bridges.bridge(checkbox.selectedProperty(),d ->
                d.getPerf().getPerfSettings().keyboardRangeEnabled());
        return checkbox;
    }

    private TextField perfNameField() {
        TextField perfName = withClass(new TextField(),"perfs-name-field","gfont");
        bridges.bridge(FXUtil.mkTextFieldCommitProperty(perfName, NULL_FOCUS_LISTENER, 16),
                d -> d.getPerf().perfName());
        return perfName;
    }

    private TitledPane titledPane(String name, Node content) {
        TitledPane p = new TitledPane(name, content);
        HBox.setHgrow(p, Priority.ALWAYS);
        p.setCollapsible(false);
        return p;
    }

    private CheckBox checkbox() {
        return withClass1("perfs-checkbox", new CheckBox());
    }

    private static Label plabel(String text) {
        return withClass1("perfs-label",label(text));
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public Pane getRoot() {
        return root;
    }
}
