package org.g2fx.g2gui;

import g2lib.model.ModuleType;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.controls.Knob;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;


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

        Scene scene = mkScene();

        stage.setTitle(TITLE);
        stage.setScene(scene);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("g2fx.css")).toExternalForm());
        stage.show();

        this.stage = stage;
        devices.start();

    }

    private static Scene mkScene() {
        TabPane slotTabs = mkSlotTabs();

        HBox editorBar = mkEditorBar();

        HBox globalBar = mkGlobalBar();

        VBox topBox = withClass(
                new VBox(globalBar,editorBar,slotTabs),"top-box");
        VBox.setVgrow(slotTabs, Priority.ALWAYS);

        Scene scene = new Scene(topBox, 1280, 775);
        return scene;
    }

    public record ModuleButtonInfo (
            Integer pageIndex,
            Integer modIndex,
            Button button) {}

    private static HBox mkEditorBar() {
        Button newButton = withClass(new Button("New"),"new-patch-button","reset-patch-button");
        Button init1Button = withClass(new Button("Init1"),"init1-patch-button","reset-patch-button");
        Button init2Button = withClass(new Button("Init2"),"init2-patch-button","reset-patch-button");
        VBox resetButtons = withClass(new VBox(newButton,init1Button,init2Button),"reset-patch-buttons");

        ToggleGroup moduleSectionSelector = new ToggleGroup();

        Map<ModuleType.ModPage,List<ModuleButtonInfo>> modsByType = new TreeMap<>();
        Stream.of(ModuleType.values()).forEach(mt -> {
            modsByType.compute(mt.modPageIx.page(),(mp, l) -> {
                if (l == null) { l = new ArrayList<>(); }
                URL icon = G2GuiApplication.class.getResource("module-icons" +
                        File.separator + String.format("%03d.png", mt.ix));
                System.out.println(mt.shortName + ": " + mt.ix + ":" + icon);
                Button tb = withClass(new Button("",
                        new ImageView(new Image(
                                Objects.requireNonNull(icon).toExternalForm())))
                ,"module-select-button");
                l.add(new ModuleButtonInfo(mt.modPageIx.ix(),mt.ix,tb));
                return l;
            });
        });

        modsByType.values().forEach(l ->
                l.sort(Comparator.comparingInt(mt -> mt.pageIndex)));

        Map<ModuleType.ModPage,HBox> modBars = new TreeMap<>();
        modsByType.forEach((mp,mbis) -> {
            modBars.put(mp,new HBox(mbis.stream().map(
                    ModuleButtonInfo::button).toList().toArray(new Button[]{})));
        });
        StackPane modsPane = new StackPane(modBars.values().toArray(new HBox[]{}));

        List<ToggleButton> moduleSectButtons = Stream.of(ModuleType.ModPage.values()).map(n -> {
                    ToggleButton tb = withClass(new ToggleButton(n.name()),
                            "module-sect-toggle", "module-sect-" + n);
                    tb.setOnAction(e -> {
                        modBars.forEach((p,b) -> b.setVisible(p == n));
                    });
                    tb.setToggleGroup(moduleSectionSelector);
                    return tb;
                }).toList();

        List<VBox> modulePairs = new ArrayList<>();
        for (int i = 0; i < moduleSectButtons.size()/2; i++) {
            modulePairs.add(withClass(new VBox(moduleSectButtons.get(i*2),moduleSectButtons.get(i*2+1)),
                    "module-sect-pair"));
        }
        HBox moduleSectPairsBar = withClass(new HBox(modulePairs.toArray(new VBox[] {})),"module-sect-bar");

        VBox moduleSelectBox = withClass(new VBox(moduleSectPairsBar,modsPane),"module-select-box");

        HBox editorBar = withClass(new HBox(resetButtons,moduleSelectBox),"editor-bar","bar","gfont");
        return editorBar;
    }

    private static TabPane mkSlotTabs() {
        List<Tab> slots = new ArrayList<>();
        for (Slot slot : Slot.values()) {

            Tab t = withClass(new Tab(slot.name()),"slot-tab","gfont");
            VBox pb = mkPatchBox(slot,t);
            t.setContent(pb);
            t.setClosable(false);
            slots.add(t);

        }

        TabPane slotTabs = withClass(new TabPane(slots.toArray(new Tab[]{})), "slot-tabs","gfont");
        return slotTabs;
    }

    private static HBox mkGlobalBar() {
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
        return globalBar;
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

        SVGPath powerGraphic = withClass(new SVGPath(),"power-graphic");
        powerGraphic.setContent("M -3 -3 A 4.5 4.5 0 1 0 3 -3 M 0 0 L 0 -4");
        ToggleButton patchEnable = withClass(new ToggleButton("", powerGraphic),"power-button");

        CheckBox redCable = cableCheckbox("red");
        CheckBox blueCable = cableCheckbox("blue");
        CheckBox yellowCable = cableCheckbox("yellow");
        CheckBox orangeCable = cableCheckbox("orange");
        CheckBox purpleCable = cableCheckbox("purple");
        CheckBox whiteCable = cableCheckbox("white");
        ToggleButton hideCables = withClass(new ToggleButton("H"),"hide-cables","cable-button");
        Button shakeCables = withClass(new Button("S"),"shake-cables","cable-button");

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
                new Knob(),
                patchEnable,
                label("Visible\nLabels"),
                redCable, blueCable, yellowCable, orangeCable, purpleCable, whiteCable,
                hideCables, shakeCables
        ),"patch-bar","bar","gfont");
        VBox patchBox = withClass(new VBox(patchBar,patchSplit),"patch-box"); // patch top bar + uis

        VBox.setVgrow(patchSplit,Priority.ALWAYS);
        return patchBox;
    }

    private static CheckBox cableCheckbox(String color) {
        CheckBox cb = withClass(new CheckBox(),"cable-" + color,"cable-checkbox");
        cb.setSelected(true);
        return cb;
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
