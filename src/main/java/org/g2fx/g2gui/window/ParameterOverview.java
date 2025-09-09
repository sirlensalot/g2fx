package org.g2fx.g2gui.window;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2gui.panel.Slots;
import org.g2fx.g2lib.state.Slot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.g2fx.g2gui.FXUtil.*;
import static org.g2fx.g2gui.G2GuiApplication.addGlobalStylesheet;

public class ParameterOverview {
    public static enum PageId {
        Osc,LFO,Env,Filter,Effect;
            public char buttonChar() { return (char) ('A' + ordinal()); }
    }

    record POControl(Label name,Label value) {}

    record SlotOrGlobal(Slot slot) {
        boolean isGlobal() { return slot == null; }
    }

    private final Button assignMidi;
    private final Button clearMidi;
    private final ToggleButton viewMidi;
    private final ToggleButton viewButtons;
    private final ToggleButton globalPages;
    private final List<List<POControl>> controls = new ArrayList<>();
    private final Slots slots;
    private final Stage stage;

    public ParameterOverview(Slots slots) {
        this.slots = slots;
        Arrays.stream(PageId.values()).forEach(i -> controls.add(new ArrayList<>()));
        HBox buttonBar = withClass1("ppage-button-bar", new HBox(
                spacer(),
                assignMidi = withClass1("ppage-button", unfocusable(new Button("Assign MIDI"))),
                clearMidi = withClass1("ppage-button", unfocusable(new Button("Clear MIDI"))),
                viewMidi = withClass1("ppage-tbutton",unfocusable(new ToggleButton("View MIDI"))),
                viewButtons = withClass1("ppage-tbutton",unfocusable(new ToggleButton("View Buttons"))),
                globalPages = withClass1("ppage-tbutton",unfocusable(new ToggleButton("Global Pages")))));
        buttonBar.setSpacing(5);
        List<VBox> pages = Arrays.stream(PageId.values()).map(pid ->
                withClass1("ppage-block", new VBox(
                        withClass1("ppage-block-label", new HBox(label(pid.buttonChar()+" "+pid.name()))),
                        withClass1("ppage-block-box",addChildren(new VBox(), IntStream.rangeClosed(1,3).mapToObj(i -> {
                            VBox idbox = withClass1("ppage-row-id", new VBox(label(Integer.toString(i))));
                            VBox.setVgrow(idbox,Priority.ALWAYS);
                            idbox.setAlignment(Pos.CENTER);
                            ArrayList<POControl> cs = new ArrayList<>();
                            controls.add(cs);
                            return withClass1("ppage-row", addChildren(
                                    new HBox(idbox),
                                    IntStream.range(0,8).mapToObj(j -> {
                                        Label pname = withClass(label(""), "ppage-pname");
                                        Label pvalue = withClass(label(""), "ppage-pvalue", "module-text-field");
                                        cs.add(new POControl(pname,pvalue));
                                        return FXUtil.withClass(new VBox(pname, pvalue),
                                                "ppage-box",
                                                i % 2 == 0 ? "ppage-box-even" : "ppage-box-odd");
                                    }).toList()));
                        }).toList())
                )))).toList();
        VBox root = addChildren(new VBox(buttonBar),pages);
        stage = new Stage();
        stage.setTitle("Parameter Overview");
        stage.setScene(addGlobalStylesheet(new Scene(root)));
        stage.setWidth(542);
        stage.setHeight(780);

        stage.show();
    }

    private Node spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public void show() {
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }
}
