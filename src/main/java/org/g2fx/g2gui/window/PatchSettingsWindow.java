package org.g2fx.g2gui.window;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
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
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.state.Device;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.g2fx.g2gui.FXUtil.*;
import static org.g2fx.g2gui.G2GuiApplication.addGlobalStylesheet;
import static org.g2fx.g2gui.controls.ButtonRadio.mkSelectedToggleIndexProperty;
import static org.g2fx.g2lib.model.ModParam.*;
import static org.g2fx.g2lib.model.SettingsModules.*;

public class PatchSettingsWindow implements G2Window {

    private final Stage stage;
    private final Slots slots;
    private final Bridges bridges;
    private final HBox root;
    private final Map<ModParam, ChangeListener<Integer>> fieldListeners = new HashMap<>();

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
                                        pfield(ArpTime),
                                        mkKnob(Arpeggiator, ArpTime),
                                        verticalRadio(Arpeggiator, ArpEnable)
                                )),
                                withClass1("pset-arp-col2", psetBox(
                                        pfield(ArpDir),
                                        mkKnob(Arpeggiator,ArpDir),
                                        verticalRadio(Arpeggiator, ArpOctaves)
                                ))
                        ))

                )),
                withClass1("pset-box",psetBox(
                        plabel("Vibrato"),
                        pfield(VibCents),
                        mkKnob(Vibrato,VibCents),
                        verticalRadio(Vibrato, VibratoControl),
                        withClass1("pset-vib-rate-box",psetVibRateBox(
                                plabel("Rate"),
                                mkKnob(Vibrato,VibRate)))
                )),
                withClass1("pset-box",psetBox(
                        plabel("Glide"),
                        pfield(GlideSpeed),
                        mkKnob(Glide,GlideSpeed),
                        verticalRadio(Glide, GlideControl)
                )),
                withClass1("pset-box",psetBox(
                        plabel("Bend"),
                        pfield(BendSemi),
                        mkKnob(Bend,BendSemi),
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
            bridges.bridge(p, d -> getSettingsValueProperty(module, param, sv, d));
            return p;
        });
    }

    private static LibProperty<Integer> getSettingsValueProperty(
            SettingsModules module, ModParam param, Slots.SlotAndVar sv, Device d) {
        return d.getPerf().getSlot(sv.slot()).getSettingsArea().getSettingsModule(module)
                .getSettingsValueProperty(param, sv.var());
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


    private Node mkKnob(SettingsModules module,ModParam param) {
        Knob knob = new Knob(module + ":" + param, 1.0,
                false, param.min, param.max);
        slots.bindSlotVarControl(knob.getValueProperty(),sv -> {
            Property<Integer> p = new SimpleObjectProperty<>(knob,sv.toString(),param.def);
            bridges.bridge(p,d -> getSettingsValueProperty(module,param,sv,d));
            return p;
        });
        ChangeListener<Integer> l = fieldListeners.get(param);
        if (l != null) {
            knob.getValueProperty().addListener(l);
            Platform.runLater(()->
                    l.changed(null,null,knob.getValueProperty().getValue()));
        }
        return knob;
    }

    /**
     * IMPORTANT: this must always be invoked before the associated knob for
     * the listener association to work.
     */
    private Label pfield(ModParam param) {
        Label l = withClass(label("--"), "pset-field", "module-text-field");
        fieldListeners.put(param,(c,o,n) -> l.setText(param.formatter.intFmt().apply(n)));
        return l;
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
