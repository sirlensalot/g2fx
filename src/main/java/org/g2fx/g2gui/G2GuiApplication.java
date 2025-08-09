package org.g2fx.g2gui;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.controls.Knob;
import org.g2fx.g2gui.controls.LoadMeter;
import org.g2fx.g2gui.controls.UIElement;
import org.g2fx.g2gui.controls.UIModule;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.Devices;
import org.g2fx.g2lib.state.Slot;
import org.g2fx.g2lib.util.Util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.g2fx.g2gui.FXUtil.withClass;


public class G2GuiApplication extends Application {

    private final Logger log = Logger.getLogger(getClass().getName());

    public static final String TITLE = "g2fx nord modular g2 editor";
    private Stage stage;

    private Devices devices;

    private FXQueue fxQueue;

    private Bridges bridges;
    private TabPane slotTabs;

    private final Undos undos = new Undos();
    private FXUtil.TextFieldFocusListener textFocusListener;
    private SegmentedButton slotBar;


    private final List<RebindableControl<Integer,?>> slotControls = new ArrayList<>();


    public record SlotAndVar(Slot slot,Integer var) {}

    private final List<RebindableControl<SlotAndVar,?>> morphControls = new ArrayList<>();



    private Map<ModuleType, UIModule<UIElement>> uiModules;

    private final List<SlotPane> slotPanes = new ArrayList<>();

    @Override
    public void init() throws Exception {
        Util.configureLogging(Level.WARNING);
        uiModules = UIModule.readModuleUIs();
        fxQueue = new FXQueue();
        devices = new Devices();
        bridges = new Bridges(devices,fxQueue,undos);
        devices.addListener(new Devices.DeviceListener() {
                    @Override
                    public void onDeviceInitialized(Device d) throws Exception {
                        G2GuiApplication.this.onDeviceInitialized(d);
                    }
                    @Override
                    public void onDeviceDisposal(Device d) throws Exception {
                        G2GuiApplication.this.onDeviceDisposal(d);
                    }});

    }

    private void onDeviceInitialized(Device d) throws Exception {
        //on lib thread: finalize bridges to get fx init updates
        List<Runnable> fxUpdates = new ArrayList<>(bridges.initialize(d));
        for (SlotPane slotPane : slotPanes) {
            slotPane.initModules(d,uiModules,fxUpdates);
        }

        //run all updates on fx thread
        fxQueue.execute(() -> {
            fxUpdates.forEach(Runnable::run);
        });
        d.sendStartStopComm(true);
    }

    private void onDeviceDisposal(Device d) throws Exception {
        //on lib thread: dispose lib listeners, get fx disposals
        List<Runnable> fxDisposals = bridges.dispose();
        //run all disposals on fx thread
        fxQueue.execute(() -> fxDisposals.forEach(Runnable::run));
    }





    @Override
    public void start(Stage stage) throws IOException {

        setupKeyBindings();
        Scene scene = mkScene();

        stage.setTitle(TITLE);
        stage.setScene(scene);
        scene.getStylesheets().add(FXUtil.getResource("g2fx.css").toExternalForm());
        stage.show();

        this.stage = stage;
        fxQueue.startPolling();
        devices.start();

        textFocusListener.focusChange(false);


    }

    private void setupKeyBindings() {
        EventHandler<? super KeyEvent> globalKeyListener = event -> {
            KeyCode code = event.getCode();
            switch (code) {
                case F:
                case V:
                    AreaId area = code == KeyCode.F ? AreaId.Fx : AreaId.Voice;
                    getSelectedSlotPane().maximizeAreaPane(area);
                    event.consume();
                    return;
                case A:
                case B:
                case C:
                case D:
                    slotBar.getToggleGroup().getToggles().get(
                            code.getCode() - KeyCode.A.getCode()).setSelected(true);
                    event.consume();
                    return;
                case KeyCode.DIGIT1:
                case KeyCode.DIGIT2:
                case KeyCode.DIGIT3:
                case KeyCode.DIGIT4:
                case KeyCode.DIGIT5:
                case KeyCode.DIGIT6:
                case KeyCode.DIGIT7:
                case KeyCode.DIGIT8:
                    getSelectedSlotPane().selectVar(
                            code.getCode() - KeyCode.DIGIT1.getCode());
                    event.consume();
                    return;

            }
        };

        textFocusListener = acquired -> {
            if (acquired) {
                stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, globalKeyListener);
            } else {
                stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, globalKeyListener);
            }
        };
    }

    private SlotPane getSelectedSlotPane() {
        return slotPanes.get(slotTabs.getSelectionModel().getSelectedIndex());
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

    private HBox mkEditorBar() {
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

    private VBox mkMorphsBox() {
        List<VBox> morphs = new ArrayList<>();
        for (int i = 0; i < FXUtil.UI_MAX_VARIATIONS; i++) {
            final int ii = i;
            String morphCtl = SettingsModules.MORPH_LABELS[i];
            ToggleButton tb = mkMorphToggle(i,i == 4 ? List.of(morphCtl,SettingsModules.MORPH_GW1) : List.of(morphCtl));
            TextField tf = withClass(new TextField(morphCtl), "morph-name");
            bindSlotControl(FXUtil.mkTextFieldCommitProperty(tf,textFocusListener), s -> {
                SimpleStringProperty gn = new SimpleStringProperty(morphCtl);
                bridges.bridge(gn, d -> d.getPerf().getSlot(s).getSettingsArea().getSettingsModule(SettingsModules.Morphs).getMorphLabel(ii));
                return gn;
            });

            tf.setPadding(new Insets(1));
            Knob dial = withClass(new Knob("Morph" + i), "morph-knob");
            bindMorphControl(dial.getValueProperty(),sv -> {
                SimpleObjectProperty<Integer> p = new SimpleObjectProperty<>(dial,"morphDial:"+sv,0);
                bridges.bridge(d -> d.getPerf().getSlot(sv.slot).getSettingsArea().getSettingsModule(SettingsModules.Morphs)
                                .getParamValueProperty(sv.var,ii),
                        new FxProperty.SimpleFxProperty<>(p,dial.valueChangingProperty()),
                        PropertyBridge.id());
                return p;
            });
            morphs.add(withClass(new VBox(
                    tf,
                    dial,
                    tb
            ),"morph-box"));
        }
        VBox morphsBox = withClass(new VBox(
                FXUtil.label("Morph Groups"),
                withClass(new HBox(morphs.toArray(new VBox[]{})),"morphs-bar")
        ),"morphs-box");
        return morphsBox;
    }

    public ToggleButton mkMorphToggle(int index, List<String> g2Controls) {
        var state = new SimpleObjectProperty<>(1);


        List<String> statuses = new ArrayList<>();
        statuses.add("Knob");
        statuses.addAll(g2Controls);
        ToggleButton btn = withClass(new ToggleButton(statuses.get(1)),"morph-mode-toggle");
        btn.setFocusTraversable(false);
        bindMorphControl(state,sv -> {
            Property<Integer> p = new SimpleObjectProperty<>(btn,"morphMode:"+sv,1);
            bridges.bridge(p,d->d.getPerf().getSlot(sv.slot).getSettingsArea().getSettingsModule(SettingsModules.Morphs)
                    .getParamValueProperty(sv.var,index+ 8));
            return p;
        });

        btn.setOnAction(event -> {
            int next = (state.get() + 1) % statuses.size();
            state.set(next);
            btn.setText(statuses.get(next));
            btn.setSelected(false); // Always unselected UI
        });

        state.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal >= 0 && newVal < statuses.size())
                btn.setText(statuses.get(newVal));
        });

        return btn;
    }


    private <T> void bindSlotControl(Property<T> control, Function<Slot,Property<T>> slotPropBuilder) {
        List<Property<T>> l = Arrays.stream(Slot.values()).map(slotPropBuilder).toList();
        slotControls.add(new RebindableControl<>(control, l::get));
    }

    private <T> void bindMorphControl(Property<T> control, Function<SlotAndVar,Property<T>> propBuilder) {
        List<List<Property<T>>> props = Arrays.stream(Slot.values()).map(s ->
                IntStream.range(0, FXUtil.UI_MAX_VARIATIONS).mapToObj(v -> propBuilder.apply(new SlotAndVar(s,v))).toList()).toList();
        morphControls.add(new RebindableControl<>(control,sv -> props.get(sv.slot.ordinal()).get(sv.var)));
    }

    private void bindLoadMeter(LoadMeter m, Function<Slot, Function<Device, LibProperty<Double>>> slotPropBuilder) {
        bindSlotControl(m.getValueProperty(), s -> {
            Property<Double> p = new SimpleObjectProperty<>((double) 0);
            Function<Device, LibProperty<Double>> b = slotPropBuilder.apply(s);
            bridges.bridge(p, b);
            return p;
        });
    }


    private VBox mkLoadMeterBox() {
        LoadMeter voiceCycles = FXUtil.withClass(
                new LoadMeter("voice-cycles"),"load-meter-voice-cycles");
        bindLoadMeter(voiceCycles, s -> d ->
                d.getPerf().getSlot(s).getArea(AreaId.Voice).getPatchLoadData().cycles());
        LoadMeter voiceMem = FXUtil.withClass(
                new LoadMeter("voice-mem"),"load-meter-voice-mem");
        bindLoadMeter(voiceMem, s -> d ->
                d.getPerf().getSlot(s).getArea(AreaId.Voice).getPatchLoadData().mem());
        LoadMeter fxCycles = FXUtil.withClass(
                new LoadMeter("fx-cycles"),"load-meter-fx-cycles");
        bindLoadMeter(fxCycles, s -> d ->
                d.getPerf().getSlot(s).getArea(AreaId.Fx).getPatchLoadData().cycles());
        LoadMeter fxMem = FXUtil.withClass(
                new LoadMeter("fx-mem"),"load-meter-fx-mem");
        bindLoadMeter(fxMem, s -> d ->
                d.getPerf().getSlot(s).getArea(AreaId.Fx).getPatchLoadData().mem());

        VBox loadMeterBox = withClass(new VBox(
               withClass(new HBox(
                       withClass(new Pane(),"load-meter-empty-0"),
                       withClass(FXUtil.label("Patch Load"),"load-label-patch-load")
                       ),"load-meter-bar"),
                withClass(new HBox(
                        withClass(new Pane(),"load-meter-empty-1"),
                        withClass(FXUtil.label("Cycles"),"load-label-cols"),
                        withClass(FXUtil.label("Memory"),"load-label-cols")
                        ),"load-meter-bar"),
                withClass(new HBox(
                        withClass(FXUtil.label("VA"),"load-label-rows"),
                        voiceCycles,
                        voiceMem
                        ),"load-meter-bar"),
                withClass(new Pane(),"load-meter-empty-2"),
                withClass(new HBox(
                        withClass(FXUtil.label("FX"),"load-label-rows"),
                        fxCycles,
                        fxMem
                        ),"load-meter-bar")
        ),"load-meter-grid");
        return loadMeterBox;
    }


    private VBox mkUndoRedoModColorBox() {
        Button undoButton = withClass(new Button("Undo"),"undo-redo-button");
        Button redoButton = withClass(new Button("Redo"),"undo-redo-button");
        undoButton.setOnAction(e -> { if (undos.canUndo()) undos.undo(); });
        redoButton.setOnAction(e -> { if (undos.canRedo()) undos.redo(); });
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
                FXUtil.label("Module Color"),
                moduleColorsCombo
        ),"undo-redo-mod-colors-box");
        return undoRedoModColorBox;
    }

    private static VBox mkModuleSelectBox() {
        ToggleGroup moduleSectionSelector = new ToggleGroup();

        Map<ModuleType.ModPage,List<ModuleButtonInfo>> modsByType = new TreeMap<>();
        ModuleType.BY_PAGE.forEach((mp,l) -> modsByType.put(mp,l.stream().map(mt -> {
            URL icon = FXUtil.getResource("module-icons" +
                    File.separator + String.format("%03d.png", mt.ix));
            Button tb = withClass(new Button("",
                            new ImageView(new Image(icon.toExternalForm())))
                    ,"module-select-button");
            return new ModuleButtonInfo(mt.modPageIx.ix(),mt.ix,tb);
        }).toList()));

        Map<ModuleType.ModPage,HBox> modBars = new TreeMap<>();
        modsByType.forEach((mp,mbis) -> modBars.put(mp,new HBox(mbis.stream().map(
                ModuleButtonInfo::button).toList().toArray(new Button[]{}))));
        StackPane modsPane = new StackPane(modBars.values().toArray(new HBox[]{}));

        List<ToggleButton> moduleSectButtons = Stream.of(ModuleType.ModPage.values()).map(n -> {
                    ToggleButton tb = FXUtil.radioToToggle(withClass(new RadioButton(n.name()),
                            "module-sect-toggle", "module-sect-" + n));
                    tb.setOnAction(e -> modBars.forEach((p, b) -> b.setVisible(p == n)));
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
        modBars.forEach((p,b) -> b.setVisible(false));
        return moduleSelectBox;
    }

    private TabPane mkSlotTabs() {
        List<Tab> slots = new ArrayList<>();
        for (Slot slot : Slot.values()) {
            SlotPane slotPane = new SlotPane(bridges,textFocusListener,slot,morphControls);
            slotPanes.add(slotPane);
            Tab t = withClass(new Tab(slot.name()),"slot-tab","gfont");
            t.setUserData(slot.ordinal());
            VBox pb = slotPane.mkPatchBox();
            t.setContent(pb);
            t.setClosable(false);
            slots.add(t);

        }

        slotTabs = withClass(new TabPane(slots.toArray(new Tab[]{})), "slot-tabs","gfont");


        return slotTabs;
    }


    private HBox mkGlobalBar() {
        TextField perfName = new TextField("perf name");
        bridges.bridge(FXUtil.mkTextFieldCommitProperty(perfName,textFocusListener),d -> d.getPerf().perfName());

        Spinner<Integer> clockSpinner = new Spinner<>(30,240,120);
        bridges.bridge(clockSpinner.getValueFactory().valueProperty(),
                d -> d.getPerf().getPerfSettings().masterClock());

        ToggleButton runClockButton = withClass(new ToggleButton("Run"), "g2-toggle");
        bridges.bridge(runClockButton.selectedProperty(),d->d.getPerf().getPerfSettings().masterClockRun());

        List<ToggleButton> sbs = Arrays.stream(Slot.values()).map(s -> {
            ToggleButton b = FXUtil.radioToToggle(withClass(new RadioButton(s.name()), "slot-button", "slot-none", "slot-disabled"));
            b.setFocusTraversable(false);
            b.setUserData(s.ordinal());
            BooleanProperty keyboard = new SimpleBooleanProperty(false);
            bridges.bridge(keyboard,d -> d.getPerf().getPerfSettings().getSlotSettings(s).keyboard());
            keyboard.addListener((v,o,n) -> {
                if (n) {
                    b.getStyleClass().remove("slot-none");
                    b.getStyleClass().add("slot-keyb");
                } else {
                    b.getStyleClass().remove("slot-keyb");
                    b.getStyleClass().add("slot-none");
                }
            });
            BooleanProperty enabled = new SimpleBooleanProperty(false);
            bridges.bridge(enabled,d -> d.getPerf().getPerfSettings().getSlotSettings(s).enabled());
            enabled.addListener((v,o,n) -> {
                if (n) {
                    b.getStyleClass().remove("slot-disabled");
                    b.getStyleClass().add("slot-enabled");
                } else {
                    b.getStyleClass().remove("slot-enabled");
                    b.getStyleClass().add("slot-disabled");
                }
            });
            return b;
        }).toList();
        sbs.getFirst().setSelected(true);

        slotBar = withClass(new SegmentedButton(sbs.toArray(new ToggleButton[]{})),"slot-bar");
        bridges.bridgeSegmentedButton(slotBar, d -> d.getPerf().getPerfSettings().selectedSlot());

        slotBar.getToggleGroup().selectedToggleProperty().addListener((v, o, n) ->
                slotChanged(o == null ? null : (Integer) o.getUserData(),
                        n == null ? null : (Integer) n.getUserData()));

        TextField synthName = new TextField("synth name");
        bridges.bridge(FXUtil.mkTextFieldCommitProperty(synthName,textFocusListener),d -> d.getSynthSettings().deviceName());

        ToggleButton perfModeButton = withClass(new ToggleButton("Perf"), "g2-toggle");
        bridges.bridge(perfModeButton.selectedProperty(),d -> d.getSynthSettings().perfMode());

        Button testFileButton = new Button("Test file");
        String testFile = "data/perf-20240802.prf2";
        //String testFile = "data/begintomind.prf2";
        testFileButton.setOnAction(e ->
                devices.invoke(true,() -> devices.loadFile(testFile)));
        HBox globalBar = withClass(new HBox(
                FXUtil.label("Perf\nName"),
                perfName,
                FXUtil.label("Master\nClock"),
                clockSpinner,
                runClockButton,
                slotBar,
                synthName,
                perfModeButton,
                testFileButton
        ),"global-bar","bar","gfont");
        return globalBar;
    }


    private void slotChanged(Integer oldSlot, Integer newSlot) {
        slotTabs.getSelectionModel().select(newSlot);
        for (RebindableControl<Integer,?> control : slotControls) {
            control.bind(newSlot);
        }
        getSelectedSlotPane().updateMorphBinds();
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
