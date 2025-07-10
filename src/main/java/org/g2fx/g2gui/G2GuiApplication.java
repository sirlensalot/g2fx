package org.g2fx.g2gui;

import javafx.application.Application;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.controls.Knob;
import org.g2fx.g2gui.controls.LoadMeter;
import org.g2fx.g2gui.controls.ModuleControl;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.Devices;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.state.Slot;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.g2fx.g2gui.FXUtil.withClass;


public class G2GuiApplication extends Application {

    private Logger log = Logger.getLogger(getClass().getName());

    public static final String TITLE = "g2fx nord modular g2 editor";
    public final Label welcomeText = new Label();
    private Stage stage;

    private Devices devices;

    private FXQueue fxQueue;

    private Node fontPane;

    private List<PropertyBridge<?>> bridges = new ArrayList<>();

    @Override
    public void init() throws Exception {
        fxQueue = new FXQueue();
        devices = new Devices();
        devices.addListener(
                new Devices.DeviceListener() {
                    @Override
                    public void onDeviceInitialized(Device d) throws Exception {
                        initDevice(d);
                    }

                    @Override
                    public void onDeviceDisposal(Device d) throws Exception {
                        fxQueue.execute(() -> {
                            try {
                                disposeDevice(d);
                            } catch (Exception e) {
                                log.log(Level.SEVERE,"Error in device dispose",e);
                            }
                        });
                    }
                });

    }

    private void initDevice(Device d) throws Exception {

        //finalize bridges
        //on lib thread: finalize bridges to get fx init updates
        List<Runnable> fxUpdates = bridges.stream().map(b -> b.finalizeInit(d)).toList();
        //run all updates on fx thread
        fxQueue.execute(() -> fxUpdates.forEach(Runnable::run));

    }

    private void disposeDevice(Device d) throws Exception {
        for (PropertyBridge<?> bridge : bridges) {
            bridge.dispose();
        }
    }

    private <T> void bridge(Property<T> fxProperty, Function<Device,LibProperty<T>> libProperty) {
        bridges.add(new PropertyBridge<T>(libProperty, devices, fxProperty, fxQueue));
    }

    @Override
    public void start(Stage stage) throws IOException {

        Scene scene = mkScene();

        stage.setTitle(TITLE);
        stage.setScene(scene);
        scene.getStylesheets().add(FXUtil.getResource("g2fx.css").toExternalForm());
        stage.show();

        this.stage = stage;
        fxQueue.startPolling();
        devices.start();

    }

    private Scene mkScene() {
        TabPane slotTabs = mkSlotTabs();

        HBox editorBar = mkEditorBar();

        HBox globalBar = mkGlobalBar();

        VBox topBox = withClass(
                new VBox(globalBar,editorBar,slotTabs),"top-box");
        VBox.setVgrow(slotTabs, Priority.ALWAYS);

        return new Scene(topBox, 1280, 775);
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

        VBox moduleSelectBox = mkModuleSelectBox();

        VBox undoRedoModColorBox = mkUndoRedoModColorBox();

        VBox loadMeterBox = mkLoadMeterBox();

        VBox morphsBox = mkMorphsBox();

        return withClass(new HBox(
                resetButtons,
                moduleSelectBox,
                undoRedoModColorBox,
                loadMeterBox,
                morphsBox
        ),"editor-bar","bar","gfont");
    }

    private static VBox mkMorphsBox() {
        List<VBox> morphs = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String morphCtl = PatchModule.MORPH_LABELS[i];
            ToggleButton tb = withClass(new ToggleButton(morphCtl), "morph-mode-toggle");
            tb.setOnAction(e -> {
                tb.setText(tb.isSelected() ? "Knob": morphCtl);
            });
            TextField tf =
                    withClass(new TextField("Group " + i), "morph-name");
            tf.setPadding(new Insets(1));
            morphs.add(withClass(new VBox(
                    tf,
                    FXUtil.withClass(new Knob("Morph" + i),"morph-knob"),
                    tb
            ),"morph-box"));
        }
        VBox morphsBox = withClass(new VBox(
                label("Morph Groups"),
                withClass(new HBox(morphs.toArray(new VBox[]{})),"morphs-bar")
        ),"morphs-box");
        return morphsBox;
    }

    private static VBox mkLoadMeterBox() {
        LoadMeter voiceCycles = FXUtil.withClass(
                new LoadMeter("voice-cycles"),"load-meter-voice-cycles");
        LoadMeter voiceMem = FXUtil.withClass(
                new LoadMeter("voice-mem"),"load-meter-voice-mem");
        LoadMeter fxCycles = FXUtil.withClass(
                new LoadMeter("fx-cycles"),"load-meter-fx-cycles");
        LoadMeter fxMem = FXUtil.withClass(
                new LoadMeter("fx-mem"),"load-meter-fx-mem");

        VBox loadMeterBox = withClass(new VBox(
               withClass(new HBox(
                       withClass(new Pane(),"load-meter-empty-0"),
                       withClass(label("Patch Load"),"load-label-patch-load")
                       ),"load-meter-bar"),
                withClass(new HBox(
                        withClass(new Pane(),"load-meter-empty-1"),
                        withClass(label("Cycles"),"load-label-cols"),
                        withClass(label("Memory"),"load-label-cols")
                        ),"load-meter-bar"),
                withClass(new HBox(
                        withClass(label("VA"),"load-label-rows"),
                        voiceCycles,
                        voiceMem
                        ),"load-meter-bar"),
                withClass(new Pane(),"load-meter-empty-2"),
                withClass(new HBox(
                        withClass(label("FX"),"load-label-rows"),
                        fxCycles,
                        fxMem
                        ),"load-meter-bar")
        ),"load-meter-grid");
        return loadMeterBox;
    }

    private static VBox mkUndoRedoModColorBox() {
        Button undoButton = withClass(new Button("Undo"),"undo-redo-button");
        Button redoButton = withClass(new Button("Redo"),"undo-redo-button");
        HBox undoRedoBar = withClass(new HBox(undoButton,redoButton),"undo-redo-bar");

        ObservableList<String> moduleColors = FXCollections.observableArrayList(
                "#C0C0C0",
                "#BABACC", // 1
                "#BACCBA", // 2
                "#CCBAB0", // 3
                "#AACBD0", // 4
                "#D4A074", // 5
                "#7A77E5", // 6 R
                "#BDC17B", // 7
                "#80B982", // 8
                "#48D1E7", // 9
                "#62D193", // 10
                "#7DC7DE", // 11
                "#C29A8F", // 12
                "#817DBA", // 13
                "#8D8DCA", // 14
                "#A5D1DE", // 15
                "#9CCF94", // 16
                "#C7D669", // 17
                "#C8D2A0", // 18
                "#D2D2BE", // 19
                "#C08C80", // 20
                "#C773D6", // 21
                "#BE82BE", // 22
                "#D2A0CD", // 23
                "#D2BED2" // 24
        );
        ComboBox<String> moduleColorsCombo = FXUtil.withClass(
                new ComboBox<>(moduleColors),"module-colors-combo");

        Callback<ListView<String>, ListCell<String>> cf = e -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setBackground(new Background(new BackgroundFill(Color.web(item),null,null)));
                }
            }
        };
        moduleColorsCombo.setCellFactory(e -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
            }
        });
        moduleColorsCombo.setButtonCell(cf.call(null));

        VBox undoRedoModColorBox = withClass(new VBox(
                undoRedoBar,
                label("Module Color"),
                moduleColorsCombo
        ),"undo-redo-mod-colors-box");
        return undoRedoModColorBox;
    }

    private static VBox mkModuleSelectBox() {
        ToggleGroup moduleSectionSelector = new ToggleGroup();

        Map<ModuleType.ModPage,List<ModuleButtonInfo>> modsByType = new TreeMap<>();
        ModuleType.BY_PAGE.forEach((mp,l) -> {
            modsByType.put(mp,l.stream().map(mt -> {
                URL icon = FXUtil.getResource("module-icons" +
                        File.separator + String.format("%03d.png", mt.ix));
                Button tb = withClass(new Button("",
                                new ImageView(new Image(icon.toExternalForm())))
                        ,"module-select-button");
                return new ModuleButtonInfo(mt.modPageIx.ix(),mt.ix,tb);
            }).toList());
        });

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
        return moduleSelectBox;
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

    private HBox mkGlobalBar() {
        TextField perfName = new TextField("perf name");
        bridge(perfName.textProperty(),d -> d.getPerf().perfName());

        Spinner<Integer> clockSpinner = new Spinner<>(30,240,120);
        bridge(clockSpinner.getValueFactory().valueProperty(),
                d -> d.getPerf().getPerfSettings().masterClock());

        ToggleButton runClockButton = withClass(new ToggleButton("Run"), "g2-toggle");

        TextField synthName = new TextField("synth name");
        bridge(synthName.textProperty(),d -> d.getSynthSettings().deviceName());

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
        ModuleControl mc = new ModuleControl(1,"ClkGen1", ModuleType.M_ClkGen);
        Pane voicePane = withClass(
                new FlowPane(new Label("voice"), key, enable,mc.getPane()),"voice-pane","area-pane","gfont"); // fixed-size area pane (although maybe no scroll unless modules are outside)
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
                new Knob("patch-volume"),
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
        fxQueue.shutdown();
        devices.shutdown();
    }

    public static void main(String[] args) {
        Application.launch();
    }
}
