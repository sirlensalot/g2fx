package org.g2fx.g2gui;

import g2lib.state.Devices;
import g2lib.state.Slot;
import g2lib.state.SynthSettings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.css.Styleable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.controls.Knob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class G2GuiApplication extends Application {

    public static final String TITLE = "g2fx nord modular g2 editor";
    public final Label welcomeText = new Label();
    private Stage stage;

    private Devices devices;

    private Node fontPane;

    @Override
    public void init() throws Exception {
        devices = new Devices();
        devices.addListener(d -> {
            Platform.runLater(() -> {
                SynthSettings ss = d.getSynthSettings();
                stage.setTitle(TITLE + ": " +
                        (ss == null ? "no settings" : ss.getDeviceName()));
            });
        });
    }

    @Override
    public void start(Stage stage) throws IOException {

        List<Tab> slots = new ArrayList<>();
        for (Slot slot : Slot.values()) {

            Tab t = withClass(new Tab(slot.name()),"slot-tab","gfont");
            VBox pb = mkPatchBox(slot,t);
            t.setContent(pb);
            t.setClosable(false);
            slots.add(t);

        }

        TabPane slotTabs = withClass(new TabPane(slots.toArray(new Tab[]{})), "slot-tabs","gfont");

        HBox editorBar = withClass(new HBox(new Label("editorBar")),"editor-bar","bar","gfont");

        TextField perfName = new TextField("perf name");

        Spinner<Integer> clockSpinner = new Spinner<>(30,240,120);
        ToggleButton runClockButton = withClass(new ToggleButton("Run"), "g2-toggle");

        TextField synthName = new TextField("synth name");

        ToggleButton perfModeButton = withClass(new ToggleButton("Perf"), "g2-toggle");

        HBox globalBar = withClass(new HBox(
                label("Perf\nName"),
                perfName,
                label("Master\nClock"),
                clockSpinner,
                runClockButton,
                synthName,
                perfModeButton
        ),"global-bar","bar","gfont");
        VBox topBox = withClass(
                new VBox(globalBar,editorBar,slotTabs),"top-box");
        VBox.setVgrow(slotTabs, Priority.ALWAYS);


        Scene scene = new Scene(topBox, 1280, 775);
        stage.setTitle(TITLE);
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("g2fx.css").toExternalForm());
        stage.show();
        this.stage = stage;
        devices.start();

        //slots.getFirst().getStyleClass().add("slot-enabled");
        //slots.getLast().getStyleClass().add("slot-keyboard");

    }


    private static <T extends Styleable> T withClass(T node, String... classes) {
        node.getStyleClass().addAll(classes);
        return node;
    }

    private static VBox mkPatchBox(Slot slot, Tab t) {

        TextField patchName = new TextField("patch name");
        ComboBox<String> patchCategory = new ComboBox<>(FXCollections.observableArrayList(
                "No Cat",
                "Acoustic",
                "Sequencer",
                "Bass",
                "Classic",
                "Drum",
                "Fantasy",
                "FX",
                "Lead",
                "Organ",
                "Pad",
                "Piano",
                "Synth",
                "Audio in",
                "User1",
                "User2"
        ));
        patchCategory.getSelectionModel().selectFirst();

        Spinner<String> voicesSpinner = new Spinner<>(FXCollections.observableArrayList("Legato","Mono","1 (1)","2 (2)"));

        List<ToggleButton> varButtons = new ArrayList<>();
        for (int i = 1; i < 9; i++) {
            varButtons.add(withClass(new ToggleButton(Integer.toString(i)),"var-button"));
        }
        SegmentedButton varSelector = new SegmentedButton(varButtons.toArray(new ToggleButton[] {}));

        Button initVar = new Button("Init");

        ToggleButton key = new ToggleButton("key");
        key.setOnAction(e -> {
            if (key.isSelected()) {
                t.getStyleClass().add("slot-keyboard");
            } else {
                t.getStyleClass().remove("slot-keyboard");
            }
        });
        ToggleButton enable = new ToggleButton("enable");
        enable.setOnAction(e -> {
            if (enable.isSelected()) {
                t.getStyleClass().add("slot-enabled");
            } else {
                t.getStyleClass().remove("slot-enabled");
            }
        });
        Pane voicePane = withClass(
                new FlowPane(new Label("voice"), key, enable),"voice-pane","area-pane","gfont"); // fixed-size area pane (although maybe no scroll unless modules are outside)
        ScrollPane voiceScroll =
                withClass(new ScrollPane(voicePane),"voice-scroll","area-scroll"); // scroll for area. investigate pannable. can prob use ctor instead of setContent

        Pane fxPane = withClass(new Pane(new Label("fx")),"fx-pane","area-pane","gfont");
        ScrollPane fxScroll = withClass(new ScrollPane(fxPane),"fx-scroll","area-scroll");

        SplitPane patchSplit =
                withClass(new SplitPane(voiceScroll,fxScroll),"patch-split"); // voice + fx
        patchSplit.setOrientation(Orientation.VERTICAL);
        HBox patchBar = withClass(new HBox(
                label("Patch\nName"),
                patchName,
                patchCategory,
                label("Voice\nMode"),
                voicesSpinner,
                new Label("Variation"),
                varSelector,
                initVar,
                label("Patch\nLevel"),
                new Knob()
        ),"patch-bar","bar","gfont");
        VBox patchBox = withClass(new VBox(patchBar,patchSplit),"patch-box"); // patch top bar + uis

        VBox.setVgrow(patchSplit,Priority.ALWAYS);
        return patchBox;
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setLineSpacing(-3);
        return l;
    }

    @Override
    public void stop() throws Exception {
        devices.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}
