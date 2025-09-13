package org.g2fx.g2gui.window;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.bridge.Bridger;
import org.g2fx.g2gui.controls.Knob;
import org.g2fx.g2gui.panel.Slots;

import java.util.List;
import java.util.stream.Stream;

import static org.g2fx.g2gui.FXUtil.*;
import static org.g2fx.g2gui.G2GuiApplication.addGlobalStylesheet;

public class PatchSettingsWindow {

    public static final double KNOB_SCALE = 1;
    private final Stage stage;
    private final Slots slots;
    private final Bridger bridges;

    public PatchSettingsWindow(Slots slots, Bridger bridges) {
        this.slots = slots;
        this.bridges = bridges;
        HBox root = withClass1("pset-root",new HBox(
                withClass1("pset-box",psetBox(
                        plabel("Sustain Pedal"),
                        withClass1("pset-sustain",mkSustain()),
                        withClass1("pset-oct-label",plabel("Octave Shift")),
                        withClass1("pset-octave",mkOctShift())
                )),
                withClass1("pset-box",psetBox(
                        plabel("Arpeggiator"),
                        withClass1("pset-arp-row",new HBox(
                                withClass1("pset-arp-col1", psetBox(
                                        pfield(),
                                        mkKnob(),
                                        mkArpActive()
                                )),
                                withClass1("pset-arp-col2", psetBox(
                                        pfield(),
                                        mkKnob(),
                                        mkArpOct()
                                ))
                        ))

                )),
                withClass1("pset-box",psetBox(
                        plabel("Vibrato"),
                        pfield(),
                        mkKnob(),
                        mkVibType(),
                        withClass1("pset-vib-rate-box",psetVibRateBox(
                                plabel("Rate"),
                                mkVibRate()))
                )),
                withClass1("pset-box",psetBox(
                        plabel("Glide"),
                        pfield(),
                        mkKnob(),
                        mkGlide()
                )),
                withClass1("pset-box",psetBox(
                        plabel("Bend"),
                        pfield(),
                        mkKnob(),
                        mkBend()
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

    private Node mkSustain() {
        return new SegmentedButton(
                withClass1("pset-sustain-button", g2Toggle("off")),
                withClass1("pset-sustain-button", g2Toggle("on"))
        );
    }
    private Node mkOctShift() {
        return new SegmentedButton(FXCollections.observableArrayList(Stream.of(-2,-1,0,1,2).map(i ->
                withClass1("pset-oct-button", g2Toggle(Integer.toString(i)))).toList()));
    }
    private Node mkKnob() {
        return new Knob("",KNOB_SCALE);
    }
    private Node mkArpActive() {
        return verticalRadio(List.of("On","Off"));
    }
    private Node verticalRadio(String... names) {
        return verticalRadio(List.of(names));
    }

    private Node verticalRadio(List<String> names) {
        ToggleGroup tg = new ToggleGroup();
        return addChildren(new VBox(),names.stream().map(n -> {
            ToggleButton tb = withClass1("pset-vert-radio", g2Toggle(n));
            tb.setToggleGroup(tg);
            return tb;
        }).toList());
    }

    private Node mkArpOct() {
        return verticalRadio(Stream.of(4,3,2,1).map(i -> i + " oct").toList());

    }
    private Node mkVibType() {
        return verticalRadio("Wheel","AfTouch","Off");
    }
    private Node mkVibRate() {
        return new Knob("", KNOB_SCALE);
    }
    private Node mkGlide() {
        return verticalRadio("Auto","Normal","Off");

    }
    private Node mkBend() {
        return verticalRadio("On","Off");
    }
    private Label pfield() {
        return withClass(label("--"), "pset-field", "module-text-field");
    }
    private Label plabel(String text) {
        return withClass1("pset-label",label(text));
    }


    public void show() {
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }
}
