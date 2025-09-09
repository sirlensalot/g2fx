package org.g2fx.g2gui.window;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
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
import org.g2fx.g2gui.bridge.Bridger;
import org.g2fx.g2gui.controls.RebindableControl;
import org.g2fx.g2gui.controls.RebindableControls;
import org.g2fx.g2gui.panel.Slots;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.KnobAssignment;
import org.g2fx.g2lib.state.Slot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.g2fx.g2gui.FXUtil.*;
import static org.g2fx.g2gui.G2GuiApplication.addGlobalStylesheet;

public class ParameterOverview {
    public static enum PageRow {
        Osc,LFO,Env,Filter,Effect;
            public char buttonChar() { return (char) ('A' + ordinal()); }
    }

    record PageControl(Label module, Label param) {}

    record SlotOrGlobal(Slot slot) {
        public boolean isGlobal() { return slot == null; }
        public int index() { return isGlobal() ? 4 : slot.ordinal(); }
        public static SlotOrGlobal mkGlobal() { return new SlotOrGlobal(null); }
    }

    private final RebindableControls<SlotOrGlobal> pageControls = new RebindableControls<>();

    private final Button assignMidi;
    private final Button clearMidi;
    private final ToggleButton viewMidi;
    private final ToggleButton viewButtons;
    private final ToggleButton globalPages;
    private final List<List<PageControl>> controls = new ArrayList<>();
    private final Slots slots;
    private final Bridger bridges;
    private final Stage stage;

    public ParameterOverview(Slots slots, Bridger bridges) {
        this.slots = slots;
        this.bridges = bridges;
        Arrays.stream(PageRow.values()).forEach(i -> controls.add(new ArrayList<>()));
        HBox buttonBar = withClass1("ppage-button-bar", new HBox(
                spacer(),
                assignMidi = withClass1("ppage-button", unfocusable(new Button("Assign MIDI"))),
                clearMidi = withClass1("ppage-button", unfocusable(new Button("Clear MIDI"))),
                viewMidi = withClass1("ppage-tbutton",unfocusable(new ToggleButton("View MIDI"))),
                viewButtons = withClass1("ppage-tbutton",unfocusable(new ToggleButton("View Buttons"))),
                globalPages = withClass1("ppage-tbutton",unfocusable(new ToggleButton("Global Pages")))));
        buttonBar.setSpacing(5);
        List<VBox> pages = Arrays.stream(PageRow.values()).map(pid ->
                withClass1("ppage-block", new VBox(
                        withClass1("ppage-block-label", new HBox(label(pid.buttonChar()+" "+pid.name()))),
                        withClass1("ppage-block-box",addChildren(new VBox(), IntStream.rangeClosed(1,3).mapToObj(col -> {
                            VBox idbox = withClass1("ppage-row-id", new VBox(label(Integer.toString(col))));
                            VBox.setVgrow(idbox,Priority.ALWAYS);
                            idbox.setAlignment(Pos.CENTER);
                            ArrayList<PageControl> cs = new ArrayList<>();
                            controls.add(cs);
                            return withClass1("ppage-row", addChildren(
                                    new HBox(idbox),
                                    IntStream.range(0,8).mapToObj(ix -> {
                                        Label pname = withClass(label(""), "ppage-pname");
                                        Label pvalue = withClass(label(""), "ppage-pvalue", "module-text-field");
                                        PageControl c = new PageControl(pname, pvalue);
                                        bindPageControl(c,pid,col,ix);
                                        cs.add(c);
                                        return FXUtil.withClass(new VBox(c.module(), c.param()),
                                                "ppage-box",
                                                col % 2 == 0 ? "ppage-box-even" : "ppage-box-odd");
                                    }).toList()));
                        }).toList())
                )))).toList();
        VBox root = addChildren(new VBox(buttonBar),pages);
        stage = new Stage();
        stage.setTitle("Parameter Overview");
        stage.setScene(addGlobalStylesheet(new Scene(root)));
        stage.setWidth(542);
        stage.setHeight(780);

        slots.addListener((c,o,n) -> {
            if (!globalPages.isSelected()) {
                pageControls.updateBinds(new SlotOrGlobal(n));
            }
        });
        globalPages.setOnAction(e -> {
            if (globalPages.isSelected()) {
                pageControls.updateBinds(SlotOrGlobal.mkGlobal());
            } else {
                pageControls.updateBinds(new SlotOrGlobal(slots.getSelectedSlotPane().getSlot()));
            }
        });

        stage.show();
    }

    private void bindPageControl(PageControl ctl, PageRow pid, int col, int ix) {
        int idx = pid.ordinal()*24 + (col-1)*8 + ix;
        List<Property<KnobAssignment>> props = new ArrayList<>();
        for (int i = 0 ; i < 5 ; i++) {
            Property<KnobAssignment> prop = new SimpleObjectProperty<KnobAssignment>(KnobAssignment.unassigned());
            boolean isGlobal = i == 4;
            int ii = i;
            Function<Device, LibProperty<KnobAssignment>> f = isGlobal ?
                    d -> d.getPerf().getGlobalKnobAssignments().assignments().get(idx) :
                    d -> d.getPerf().getSlot(Slot.fromIndex(ii)).getKnobAssignments().assignments().get(idx);
            bridges.bridge(prop,f);
            props.add(prop);
        }
        Property<KnobAssignment> prop = new SimpleObjectProperty<>();
        pageControls.add(new RebindableControl<>(prop,sog -> props.get(sog.index())));
        prop.addListener((c,o,n) -> {
            ctl.module().setText(n.assigned() ? n.loc().module()+"" : "");
            ctl.param().setText(n.assigned() ? n.loc().param()+"" : "");
        });
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
