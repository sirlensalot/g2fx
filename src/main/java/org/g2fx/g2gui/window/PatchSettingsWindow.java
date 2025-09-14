package org.g2fx.g2gui.window;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.controls.Knob;
import org.g2fx.g2gui.panel.Slots;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.SettingsModules;

import java.util.List;

import static org.g2fx.g2gui.FXUtil.*;
import static org.g2fx.g2gui.G2GuiApplication.addGlobalStylesheet;
import static org.g2fx.g2gui.controls.ButtonRadio.mkSelectedToggleIndexProperty;
import static org.g2fx.g2lib.model.ModParam.*;
import static org.g2fx.g2lib.model.SettingsModules.*;

public class PatchSettingsWindow implements G2Window {

    public static final double KNOB_SCALE = 1;
    private final Stage stage;
    private final Slots slots;
    private final Bridges bridges;
    private final HBox root;

    public PatchSettingsWindow(Slots slots, Bridges bridges) {
        this.slots = slots;
        this.bridges = bridges;

        root = withClass1("pset-root",new HBox(
                withClass1("pset-box",psetBox(
                        plabel("Sustain Pedal"),
                        withClass1("pset-sustain",
                                mkSegmentedButton(Misc, MiscSustain, "pset-sustain-button")),
                        withClass1("pset-oct-label",plabel("Octave Shift")),
                        withClass1("pset-octave",
                                mkSegmentedButton(Misc, MiscOctShift, "pset-oct-button"))
                )),
                withClass1("pset-box",psetBox(
                        plabel("Arpeggiator"),
                        withClass1("pset-arp-row",new HBox(
                                withClass1("pset-arp-col1", psetBox(
                                        pfield(),
                                        mkKnob(),
                                        verticalRadio(Arpeggiator, ArpEnable)
                                )),
                                withClass1("pset-arp-col2", psetBox(
                                        pfield(),
                                        mkKnob(),
                                        verticalRadio(Arpeggiator, ArpOctaves)
                                ))
                        ))

                )),
                withClass1("pset-box",psetBox(
                        plabel("Vibrato"),
                        pfield(),
                        mkKnob(),
                        verticalRadio(Vibrato, VibratoControl),
                        withClass1("pset-vib-rate-box",psetVibRateBox(
                                plabel("Rate"),
                                mkVibRate()))
                )),
                withClass1("pset-box",psetBox(
                        plabel("Glide"),
                        pfield(),
                        mkKnob(),
                        verticalRadio(Glide, GlideControl)
                )),
                withClass1("pset-box",psetBox(
                        plabel("Bend"),
                        pfield(),
                        mkKnob(),
                        verticalRadio(Bend, BendEnable)
                ))
        ));
        stage = new Stage();
        stage.setTitle("Patch Settings");
        stage.setScene(addGlobalStylesheet(new Scene(root)));
        stage.setWidth(410);
        stage.setHeight(210);
        trackWinSizeInTitle(stage,"Patch Settings");

        stage.show();
    }

    private Node psetBox(Node... children) {
        VBox box = new VBox(children);
        box.setAlignment(Pos.TOP_CENTER);
        box.setSpacing(4);
        return box;
    }

    private Node psetVibRateBox(Node... children) {
        VBox box = new VBox(children);
        box.setAlignment(Pos.TOP_CENTER);
        box.setSpacing(0);
        return box;
    }


    private void setupSettingsToggle(SettingsModules module, ModParam param, Node ctl, ToggleGroup toggleGroup, List<ToggleButton> buttons) {
        for (int i = 0; i < buttons.size(); i++) { buttons.get(i).setUserData(i); }
        Property<Integer> property = mkSelectedToggleIndexProperty(ctl,
                module.getIndexParam(param),
                toggleGroup, buttons);
        slots.bindSlotVarControl(property,sv -> {
            Property<Integer> p =
                    new SimpleObjectProperty<>(ctl, property.getName(), param.def);
            bridges.bridge(p, d -> d.getPerf().getSlot(sv.slot()).getSettingsArea().getSettingsModule(module)
                    .getSettingsValueProperty(param,sv.var()));
            return p;
        });
    }

    private Node verticalRadio(SettingsModules module, ModParam param) {
        ToggleGroup tg = new ToggleGroup();
        List<ToggleButton> buttons = param.enums.stream().map(n -> {
            ToggleButton tb = withClass1("pset-vert-radio", g2Toggle(n));
            tb.setToggleGroup(tg);
            return tb;
        }).toList();
        VBox ctl = addChildren(new VBox(), buttons.reversed());
        setupSettingsToggle(module, param,ctl,tg,buttons);
        return ctl;
    }


    private Node mkSegmentedButton(SettingsModules module, ModParam param, String css) {
        SegmentedButton ctl = new SegmentedButton(FXCollections.observableArrayList(
                param.enums.stream().map(n -> withClass1(css,g2Toggle(n))).toList()));
        setupSettingsToggle(module,param,ctl,ctl.getToggleGroup(),ctl.getButtons());
        return ctl;
    }


    private Node mkKnob() {
        return new Knob("",KNOB_SCALE);
    }

    private Node mkVibRate() {
        return new Knob("", KNOB_SCALE);
    }

    private Label pfield() {
        return withClass(label("--"), "pset-field", "module-text-field");
    }
    private Label plabel(String text) {
        return withClass1("pset-label",label(text));
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
