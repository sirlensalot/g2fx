package org.g2fx.g2gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.bridge.FxProperty;
import org.g2fx.g2gui.bridge.Iso;
import org.g2fx.g2gui.controls.Knob;
import org.g2fx.g2gui.controls.LoadMeter;
import org.g2fx.g2gui.controls.MultiStateToggle;
import org.g2fx.g2gui.controls.TextOrImage;
import org.g2fx.g2gui.panel.Slots;
import org.g2fx.g2gui.window.*;
import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.device.DeviceListener;
import org.g2fx.g2lib.device.Devices;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.ParamConstants;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Patch;
import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.usb.UsbService;
import org.g2fx.g2lib.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.g2fx.g2gui.FXUtil.*;


public class G2GuiApplication extends Application implements DeviceListener {

    public static final String G2_TOOLBAR_DRAG = "G2_TOOLBAR_DRAG";
    private final Logger log;

    public static final String TITLE = "g2fx nord modular g2 editor";
    private final boolean usbEnabled;

    private Devices devices;

    private FXQueue fxQueue;

    private Bridges<Device> deviceBridges;
    private Bridges<Performance> perfBridges;

    private Commands commands;

    private final Undos undos = new Undos();

    private FXUtil.TextFieldFocusListener textFocusListener;

    private Slots slots;
    private ScriptWindow scriptWindow;
    private final Map<Integer, ObservableValue<String>> morphNames = new TreeMap<>();
    private UsbService usbService;

    public G2GuiApplication() {
        this(true);
    }
    public G2GuiApplication(boolean usbEnabled) {
        super();
        this.usbEnabled = usbEnabled;
        Util.configureLogging();
        log = Logger.getLogger(getClass().getName());
    }

    @Override
    public void init() throws Exception {
        fxQueue = new FXQueue();
        usbService = new UsbService();
        devices = new Devices(usbService);
        deviceBridges = new Bridges<>(devices,fxQueue,undos);
        perfBridges = deviceBridges.spawn();
        slots = new Slots(undos, perfBridges);
        commands = new Commands(devices, slots, undos);
        devices.addListener(this);

    }

    public Devices getDevices() {
        return devices;
    }

    @Override
    public void onDeviceInitialized(Device d) throws Exception {

        //on lib thread: finalize bridges to get fx init updates
        List<Runnable> fxUpdates = new ArrayList<>();
        fxUpdates.add(slots.clearModules());
        fxUpdates.addAll(slots.initModules(d)); //modules first so var controls will update
        fxUpdates.addAll(deviceBridges.initialize(d));
        fxUpdates.addAll(perfBridges.initialize(d.getPerf()));
        fxUpdates.addAll(slots.initBridges(d));

        //run all updates on fx thread
        fxQueue.execute(() -> {
            fxUpdates.forEach(Runnable::run);
        });

        fxQueue.execute(()->scriptWindow.updatePath());
    }

    @Override
    public void onDeviceDisposal(Device d) throws Exception {
        //on lib thread: dispose lib listeners
        deviceBridges.dispose();
        perfBridges.dispose();
        slots.disposeBridges();
        slots.disposeModuleBridges();
    }





    @Override
    public void start(Stage stage) throws Exception {


        textFocusListener = commands.setupKeyBindings(stage);
        Scene scene = mkScene(stage);

        scriptWindow = new ScriptWindow(devices,commands);
        ParameterOverview parameterOverview = new ParameterOverview(slots,perfBridges,morphNames);
        PatchSettingsWindow patchSettings = new PatchSettingsWindow(slots,perfBridges);
        PerformanceSettingsWindow perfSettings = new PerformanceSettingsWindow(perfBridges);
        PatchBrowser patchBrowser = new PatchBrowser(slots, deviceBridges);


        commands.setScriptWindow(scriptWindow);
        commands.setParameterOverview(parameterOverview);
        commands.setPatchSettings(patchSettings);
        commands.setPerfSettings(perfSettings);
        commands.setPatchBrowser(patchBrowser);

        stage.setTitle(TITLE);
        stage.setScene(scene);
        addGlobalStylesheet(scene);
        stage.show();

        fxQueue.startPolling();
        if (usbEnabled) usbService.start();

        textFocusListener.focusChange(false);


    }

    public static Scene addGlobalStylesheet(Scene scene) {
        scene.getStylesheets().add(FXUtil.getResource("g2fx.css").toExternalForm());
        return scene;
    }


    private Scene mkScene(Stage stage) {

        MenuBar menuBar = commands.setupMenu(stage);

        TabPane slotTabs = slots.mkSlotTabs(textFocusListener);

        HBox editorBar = mkEditorBar();

        HBox globalBar = mkGlobalBar();

        VBox topBox = withClass(
                new VBox(menuBar,globalBar,editorBar,slotTabs),"top-box");
        VBox.setVgrow(slotTabs, Priority.ALWAYS);

        return new Scene(topBox, 1300, 800);
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
        for (int i = 0; i < SettingsModules.MORPH_LABELS.length; i++) {
            final int ii = i;
            String morphCtl = SettingsModules.MORPH_LABELS[i];
            ToggleButton tb = mkMorphToggle(i,i == 4 ? List.of(morphCtl,SettingsModules.MORPH_GW1) : List.of(morphCtl));
            TextField tf = withClass(new TextField(morphCtl), "morph-name");
            morphNames.put(i,tf.textProperty());
            slots.bindSlotControl(FXUtil.mkTextFieldCommitProperty(tf,textFocusListener, 16), s -> {
                SimpleStringProperty gn = new SimpleStringProperty(morphCtl);
                s.getBridges().bridge(gn, d -> d.getSettingsArea().getSettingsModule(SettingsModules.Morphs).getMorphLabel(ii));
                return gn;
            });

            tf.setPadding(new Insets(1));
            Knob dial = withClass(new Knob("Morph" + i, 1.0), "morph-knob");
            slots.bindSlotVarControl(dial.getValueProperty(), sv -> {
                SimpleObjectProperty<Integer> p = new SimpleObjectProperty<>(dial,"morphDial:"+sv,0);
                perfBridges.bridge(d -> d.getSlot(sv.slot()).getSettingsArea().getSettingsModule(SettingsModules.Morphs)
                                .getParamValueProperty(sv.var(),ii),
                        new FxProperty.SimpleFxProperty<>(p,dial.valueChangingProperty()),
                        Iso.id());
                return p;
            });
            morphs.add(withClass(new VBox(
                    tf,
                    dial,
                    tb
            ),"morph-box"));
        }
        VBox morphsBox = withClass(new VBox(
                label("Morph Groups"),
                withClass(addChildren(new HBox(),morphs),"morphs-bar")
        ),"morphs-box");
        return morphsBox;
    }

    public ToggleButton mkMorphToggle(int index, List<String> g2Controls) {

        List<String> statuses = new ArrayList<>();
        statuses.add("Knob");
        statuses.addAll(g2Controls);
        MultiStateToggle mst = new MultiStateToggle(TextOrImage.mkTexts(statuses), 1, "morph-mode-toggle");
        slots.bindSlotVarControl(mst.state(), sv -> {
            Property<Integer> p = new SimpleObjectProperty<>(mst.getToggle(),"morphMode:"+sv,1);
            perfBridges.bridge(p, d->d.getSlot(sv.slot()).getSettingsArea().getSettingsModule(SettingsModules.Morphs)
                    .getParamValueProperty(sv.var(),index+ 8));
            return p;
        });

        return mst.getToggle();
    }




    private void bindLoadMeter(LoadMeter m, Function<Patch, LibProperty<Double>> slotPropBuilder) {
        slots.bindSlotControl(m.getValueProperty(), s -> {
            Property<Double> p = new SimpleObjectProperty<>((double) 0);
            s.getBridges().bridge(p, slotPropBuilder);
            return p;
        });
    }


    private VBox mkLoadMeterBox() {
        LoadMeter voiceCycles = FXUtil.withClass(
                new LoadMeter("voice-cycles"),"load-meter-voice-cycles");
        bindLoadMeter(voiceCycles, d ->
                d.getArea(AreaId.Voice).getPatchLoadData().cycles());
        LoadMeter voiceMem = FXUtil.withClass(
                new LoadMeter("voice-mem"),"load-meter-voice-mem");
        bindLoadMeter(voiceMem, d ->
                d.getArea(AreaId.Voice).getPatchLoadData().mem());
        LoadMeter fxCycles = FXUtil.withClass(
                new LoadMeter("fx-cycles"),"load-meter-fx-cycles");
        bindLoadMeter(fxCycles, d ->
                d.getArea(AreaId.Fx).getPatchLoadData().cycles());
        LoadMeter fxMem = FXUtil.withClass(
                new LoadMeter("fx-mem"),"load-meter-fx-mem");
        bindLoadMeter(fxMem, d ->
                d.getArea(AreaId.Fx).getPatchLoadData().mem());

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
        undoButton.setOnAction(e -> undos.undo());
        redoButton.setOnAction(e -> undos.redo());
        HBox undoRedoBar = withClass(new HBox(undoButton,redoButton),"undo-redo-bar");

        ObservableList<String> moduleColors =
                FXCollections.observableArrayList(ParamConstants.MODULE_COLORS);
        ComboBox<String> moduleColorsCombo = FXUtil.withClass(
                new ComboBox<>(moduleColors),"module-colors-combo");
        Platform.runLater(() -> {
            moduleColorsCombo.getSelectionModel().select(0);
            moduleColorsCombo.showingProperty().addListener((c,o,n) -> {
                if (!n) {
                    slots.updateModuleColor(moduleColorsCombo.getSelectionModel().getSelectedIndex());
                }
            });
        });

        Callback<ListView<String>, ListCell<String>> cf = e -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setBackground(FXUtil.rgbFill(item));
                }
            }
        };
        moduleColorsCombo.setCellFactory(cf);
        moduleColorsCombo.setButtonCell(cf.call(null));

        VBox undoRedoModColorBox = withClass(new VBox(
                undoRedoBar,
                FXUtil.label("Module Color"),
                moduleColorsCombo
        ),"undo-redo-mod-colors-box");
        return undoRedoModColorBox;
    }

    private VBox mkModuleSelectBox() {
        ToggleGroup moduleSectionSelector = new ToggleGroup();

        Map<ModuleType.ModPage,List<ModuleButtonInfo>> modsByType = new TreeMap<>();
        ModuleType.BY_PAGE.forEach((mp,l) -> modsByType.put(mp,l.stream().map(mt -> {
            ImageView iv = getImageViewResource("module-icons" +
                    File.separator + String.format("%03d.png", mt.ix));
            Button tb = withClass(new Button("",iv),"module-select-button");
            tb.setOnDragDetected(e -> {
                slots.startToolbarModuleDrag(mt);
                Dragboard db = tb.startDragAndDrop(TransferMode.COPY);
                ClipboardContent c = new ClipboardContent();
                c.putString(G2_TOOLBAR_DRAG);
                db.setContent(c);
                e.consume();
            });
            tb.setOnMouseClicked(e -> {
                if (e.getClickCount() != 2) return;
                slots.getSelectedSlotPane().addNewModule(mt);
            });
            return new ModuleButtonInfo(mt.modPageIx.ix(),mt.ix,tb);
        }).toList()));

        Map<ModuleType.ModPage,HBox> modBars = new TreeMap<>();
        modsByType.forEach((mp,mbis) -> modBars.put(mp,addChildren(new HBox(),
                mbis.stream().map(ModuleButtonInfo::button).toList())));
        StackPane modsPane = addChildren(new StackPane(),modBars.values());

        List<ToggleButton> moduleSectButtons = Stream.of(ModuleType.ModPage.values()).map(n -> {
                    ToggleButton tb = FXUtil.radioToToggle(withClass(new RadioButton(n.name()),
                            "module-sect-toggle", "module-sect-" + n, FXUtil.G2_TOGGLE));
                    tb.setOnAction(e -> modBars.forEach((p, b) -> b.setVisible(p == n)));
                    tb.setToggleGroup(moduleSectionSelector);
                    return tb;
                }).toList();

        List<VBox> modulePairs = new ArrayList<>();
        for (int i = 0; i < moduleSectButtons.size()/2; i++) {
            modulePairs.add(withClass(new VBox(moduleSectButtons.get(i*2),moduleSectButtons.get(i*2+1)),
                    "module-sect-pair"));
        }
        HBox moduleSectPairsBar = withClass(addChildren(new HBox(),modulePairs),"module-sect-bar");

        VBox moduleSelectBox = withClass(new VBox(moduleSectPairsBar,modsPane),"module-select-box");
        modBars.forEach((p,b) -> b.setVisible(false));
        return moduleSelectBox;
    }

    private HBox mkGlobalBar() {
        TextField perfName = new TextField("perf name");
        perfBridges.bridge(FXUtil.mkTextFieldCommitProperty(perfName,textFocusListener, 16), Performance::perfName);

        Spinner<Integer> clockSpinner = mkClockSpinner(perfBridges);

        ToggleButton runClockButton = mkClockRunButton(perfBridges);

        SegmentedButton slotBar = slots.mkSlotBar();

        TextField synthName = new TextField("synth name");
        deviceBridges.bridge(FXUtil.mkTextFieldCommitProperty(synthName,textFocusListener, 16),
                d -> d.getSynthSettings().deviceName());

        ToggleButton perfModeButton = withClass(new ToggleButton("Perf"), FXUtil.G2_TOGGLE);
        deviceBridges.bridge(perfModeButton.selectedProperty(), d -> d.getSynthSettings().perfMode());

        HBox globalBar = withClass(new HBox(
                FXUtil.label("Perf\nName"),
                perfName,
                FXUtil.label("Master\nClock"),
                clockSpinner,
                runClockButton,
                slotBar,
                synthName,
                perfModeButton
        ),"global-bar","bar","gfont");
        return globalBar;
    }

    public static ToggleButton mkClockRunButton(Bridges<Performance> bridges) {
        ToggleButton runClockButton = withClass(new ToggleButton("Run"), FXUtil.G2_TOGGLE);
        bridges.bridge(runClockButton.selectedProperty(),d->d.getPerfSettings().masterClockRun());
        return runClockButton;
    }

    public static Spinner<Integer> mkClockSpinner(Bridges<Performance> bridges) {
        Spinner<Integer> clockSpinner = new Spinner<>(30,240,120);
        bridges.bridge(clockSpinner.getValueFactory().valueProperty(),
                d -> d.getPerfSettings().masterClock());
        return clockSpinner;
    }


    @Override
    public void stop() throws Exception {
        fxQueue.shutdown();
        devices.shutdown();
        if (usbEnabled) usbService.shutdown();
    }

    public static void main(String[] args) {
        Application.launch();
    }
}
