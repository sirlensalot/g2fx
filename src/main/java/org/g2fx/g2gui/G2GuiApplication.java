package org.g2fx.g2gui;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.controls.Knob;
import org.g2fx.g2gui.controls.LoadMeter;
import org.g2fx.g2gui.controls.ModuleSelector;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.state.*;
import org.g2fx.g2lib.util.Util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.g2fx.g2gui.FXUtil.withClass;


public class G2GuiApplication extends Application {

    public static final int UI_MAX_VARIATIONS = 8;
    private final Logger log = Logger.getLogger(getClass().getName());

    public static final String TITLE = "g2fx nord modular g2 editor";
    private Stage stage;

    private Devices devices;

    private FXQueue fxQueue;

    private final List<PropertyBridge<?,?>> bridges = new ArrayList<>();
    private TabPane slotTabs;

    private final Undos undos = new Undos();

    public static class RebindableControl<T,P> {
        private final Property<P> control;
        private final Function<T,Property<P>> targetProps;
        private Property<P> last;
        public RebindableControl(Property<P> control, Function<T,Property<P>> targetProps) {
            this.control = control;
            this.targetProps = targetProps;
        }
        public void bind(T target) {
            if (last != null) { control.unbindBidirectional(last); }
            control.bindBidirectional(last = targetProps.apply(target));
        }
    }
    private final List<RebindableControl<Integer,?>> slotControls = new ArrayList<>();

    private final EnumMap<Slot,List<RebindableControl<Integer,?>>> varControls = initVarControls();

    public record SlotAndVar(Slot slot,Integer var) {}

    private final List<RebindableControl<SlotAndVar,?>> morphControls = new ArrayList<>();

    private static EnumMap<Slot, List<RebindableControl<Integer,?>>> initVarControls() {
        EnumMap<Slot, List<RebindableControl<Integer,?>>> m = new EnumMap<>(Slot.class);
        Arrays.stream(Slot.values()).forEach(s -> m.put(s,new ArrayList<>()));
        return m;
    }

    private final List<ObservableValue<Toggle>> selectedVars = new ArrayList<>();

    @Override
    public void init() throws Exception {
        Util.configureLogging(Level.WARNING);
        fxQueue = new FXQueue();
        devices = new Devices();
        devices.addListener(new Devices.DeviceListener() {
                    @Override
                    public void onDeviceInitialized(Device d) throws Exception {
                        initDevice(d);
                    }
                    @Override
                    public void onDeviceDisposal(Device d) throws Exception {
                        disposeDevice(d);
                    }});

    }

    private void initDevice(Device d) throws Exception {
        //on lib thread: finalize bridges to get fx init updates
        List<Runnable> fxUpdates = bridges.stream().map(b -> b.finalizeInit(d)).toList();
        //run all updates on fx thread
        fxQueue.execute(() -> fxUpdates.forEach(Runnable::run));
        d.sendStartStopComm(true);
    }

    private void disposeDevice(Device d) throws Exception {
        //on lib thread: dispose lib listeners, get fx disposals
        List<Runnable> fxDisposals = bridges.stream().map(PropertyBridge::dispose).toList();
        //run all disposals on fx thread
        fxQueue.execute(() -> fxDisposals.forEach(Runnable::run));
    }

    private <T> void bridge(Property<T> fxProperty, Function<Device,LibProperty<T>> libProperty) {
        bridge(libProperty, new FxProperty.SimpleFxProperty<>(fxProperty),PropertyBridge.id());
    }

    private <T,F> void bridge(Function<Device,LibProperty<T>> libProperty,
                            FxProperty<F> fxProperty,
                            PropertyBridge.Iso<T,F> iso) {
        bridges.add(new PropertyBridge<>(libProperty, devices,fxProperty, fxQueue, iso, undos));
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
        for (int i = 0; i < UI_MAX_VARIATIONS; i++) {
            final int ii = i;
            String morphCtl = SettingsModules.MORPH_LABELS[i];
            ToggleButton tb = mkMorphToggle(i,i == 4 ? List.of(morphCtl,SettingsModules.MORPH_GW1) : List.of(morphCtl));
            TextField tf = withClass(new TextField(morphCtl), "morph-name");
            bindSlotControl(tf.textProperty(),s -> {
                SimpleStringProperty gn = new SimpleStringProperty(morphCtl);
                bridge(gn, d -> d.getPerf().getSlot(s).getSettingsArea().getSettingsModule(SettingsModules.Morphs).getMorphLabel(ii));
                return gn;
            });

            tf.setPadding(new Insets(1));
            Knob dial = withClass(new Knob("Morph" + i), "morph-knob");
            bindMorphControl(dial.getValueProperty(),sv -> {
                SimpleObjectProperty<Integer> p = new SimpleObjectProperty<>(dial,"morphDial:"+sv,0);
                bridge(d -> d.getPerf().getSlot(sv.slot).getSettingsArea().getSettingsModule(SettingsModules.Morphs)
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
                label("Morph Groups"),
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
            bridge(p,d->d.getPerf().getSlot(sv.slot).getSettingsArea().getSettingsModule(SettingsModules.Morphs)
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

    private <T> void bindVarControl(Slot slot, Property<T> control, IntFunction<Property<T>> varPropBuilder) {
        List<Property<T>> l = IntStream.range(0, UI_MAX_VARIATIONS).mapToObj(varPropBuilder).toList();
        varControls.get(slot).add(new RebindableControl<>(control, l::get));
    }

    private <T> void bindMorphControl(Property<T> control, Function<SlotAndVar,Property<T>> propBuilder) {
        List<List<Property<T>>> props = Arrays.stream(Slot.values()).map(s ->
                IntStream.range(0, UI_MAX_VARIATIONS).mapToObj(v -> propBuilder.apply(new SlotAndVar(s,v))).toList()).toList();
        morphControls.add(new RebindableControl<>(control,sv -> props.get(sv.slot.ordinal()).get(sv.var)));
    }

    private void bindLoadMeter(LoadMeter m, Function<Slot, Function<Device, LibProperty<Double>>> slotPropBuilder) {
        bindSlotControl(m.getValueProperty(), s -> {
            Property<Double> p = new SimpleObjectProperty<>((double) 0);
            Function<Device, LibProperty<Double>> b = slotPropBuilder.apply(s);
            bridge(p, b);
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
                label("Module Color"),
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
                    ToggleButton tb = radioToToggle(withClass(new RadioButton(n.name()),
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

            Tab t = withClass(new Tab(slot.name()),"slot-tab","gfont");
            t.setUserData(slot.ordinal());
            VBox pb = mkPatchBox(slot);
            t.setContent(pb);
            t.setClosable(false);
            slots.add(t);

        }

        slotTabs = withClass(new TabPane(slots.toArray(new Tab[]{})), "slot-tabs","gfont");


        return slotTabs;
    }


    private HBox mkGlobalBar() {
        TextField perfName = new TextField("perf name");
        bridge(perfName.textProperty(),d -> d.getPerf().perfName());

        Spinner<Integer> clockSpinner = new Spinner<>(30,240,120);
        bridge(clockSpinner.getValueFactory().valueProperty(),
                d -> d.getPerf().getPerfSettings().masterClock());

        ToggleButton runClockButton = withClass(new ToggleButton("Run"), "g2-toggle");
        bridge(runClockButton.selectedProperty(),d->d.getPerf().getPerfSettings().masterClockRun());

        List<ToggleButton> sbs = Arrays.stream(Slot.values()).map(s -> {
            ToggleButton b = radioToToggle(withClass(new RadioButton(s.name()), "slot-button", "slot-none", "slot-disabled"));
            b.setFocusTraversable(false);
            b.setUserData(s.ordinal());
            BooleanProperty keyboard = new SimpleBooleanProperty(false);
            bridge(keyboard,d -> d.getPerf().getPerfSettings().getSlotSettings(s).keyboard());
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
            bridge(enabled,d -> d.getPerf().getPerfSettings().getSlotSettings(s).enabled());
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

        SegmentedButton slotBar = withClass(new SegmentedButton(sbs.toArray(new ToggleButton[]{})),"slot-bar");
        bridgeSegmentedButton(slotBar,d -> d.getPerf().getPerfSettings().selectedSlot());

        slotBar.getToggleGroup().selectedToggleProperty().addListener((v,o,n) ->
                slotChanged(o == null ? null : (Integer) o.getUserData(),
                        n == null ? null : (Integer) n.getUserData()));

        TextField synthName = new TextField("synth name");
        bridge(synthName.textProperty(),d -> d.getSynthSettings().deviceName());

        ToggleButton perfModeButton = withClass(new ToggleButton("Perf"), "g2-toggle");
        bridge(perfModeButton.selectedProperty(),d -> d.getSynthSettings().perfMode());

        Button testFileButton = new Button("Test file");
        testFileButton.setOnAction(e ->
                devices.invoke(true,() -> devices.loadFile("data/perf-20240802.prf2")));
        HBox globalBar = withClass(new HBox(
                label("Perf\nName"),
                perfName,
                label("Master\nClock"),
                clockSpinner,
                runClockButton,
                slotBar,
                synthName,
                perfModeButton,
                testFileButton
        ),"global-bar","bar","gfont");
        return globalBar;
    }


    private static ToggleButton radioToToggle(ToggleButton b) {
        b.getStyleClass().remove("radio-button");
        b.getStyleClass().add("toggle-button");
        return b;
    }

    private void slotChanged(Integer oldSlot, Integer newSlot) {
        slotTabs.getSelectionModel().select(newSlot);
        for (RebindableControl<Integer,?> control : slotControls) {
            control.bind(newSlot);
        }
        updateMorphBinds();
    }

    private void varChanged(Slot slot, Integer oldVar, Integer newVar) {
        for (RebindableControl<Integer,?> vc : varControls.get(slot)) {
            vc.bind(newVar);
        }
        updateMorphBinds();
    }

    private void updateMorphBinds() {
        int slot = slotTabs.getSelectionModel().getSelectedIndex();
        Toggle varToggle = selectedVars.get(slot).getValue();
        if (varToggle == null) { return; }
        int var = (Integer) varToggle.getUserData();
        for (RebindableControl<SlotAndVar, ?> mc : morphControls) {
            mc.bind(new SlotAndVar(Slot.fromIndex(slot),var));
        }
    }


    private VBox mkPatchBox(Slot slot) {

        TextField patchName = new TextField("slot" + slot);
        bridge(patchName.textProperty(),d -> d.getPerf().getPerfSettings().getSlotSettings(slot).patchName());
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
        bridge(d -> d.getPerf().getSlot(slot).getPatchSettings().category(),
            new FxProperty<>(patchCategory.getSelectionModel().selectedIndexProperty()) {
                @Override
                public void setValue(Number value) {
                    patchCategory.getSelectionModel().select(value.intValue());
                }
            },
            new PropertyBridge.Iso<>() {
                @Override
                public Number to(Integer integer) {
                    return integer;
                }

                @Override
                public Integer from(Number number) {
                    return number.intValue();
                }
            }
        );

        Spinner<VoiceMode> voicesSpinner = mkVoicesSpinner(slot);

        SegmentedButton varSelector = mkVarSelector(slot);


        Button initVar = new Button("Init");

        ModuleSelector mc = new ModuleSelector(1,"ClkGen1", ModuleType.M_ClkGen);
        Pane voicePane = withClass(
                new FlowPane(new Label("voice"), mc.getPane()),"voice-pane","area-pane","gfont"); // fixed-size area pane (although maybe no scroll unless modules are outside)
        ScrollPane voiceScroll =
                withClass(new ScrollPane(voicePane),"voice-scroll","area-scroll"); // scroll for area. investigate pannable. can prob use ctor instead of setContent

        Pane fxPane = withClass(new Pane(new Label("fx")),"fx-pane","area-pane","gfont");
        ScrollPane fxScroll = withClass(new ScrollPane(fxPane),"fx-scroll","area-scroll");

        SVGPath powerGraphic = withClass(new SVGPath(),"power-graphic");
        powerGraphic.setContent("M -3 -3 A 4.5 4.5 0 1 0 3 -3 M 0 0 L 0 -4");
        ToggleButton patchEnable = withClass(new ToggleButton("", powerGraphic),"power-button");

        CheckBox redCable = cableCheckbox(slot,"red",PatchSettings::red);
        CheckBox blueCable = cableCheckbox(slot,"blue",PatchSettings::blue);
        CheckBox yellowCable = cableCheckbox(slot,"yellow",PatchSettings::yellow);
        CheckBox orangeCable = cableCheckbox(slot,"orange",PatchSettings::orange);
        CheckBox purpleCable = cableCheckbox(slot,"purple",PatchSettings::purple);
        CheckBox whiteCable = cableCheckbox(slot,"white",PatchSettings::white);
        ToggleButton hideCables = withClass(new ToggleButton("H"),"hide-cables","cable-button");
        Button shakeCables = withClass(new Button("S"),"shake-cables","cable-button");

        SplitPane patchSplit =
                withClass(new SplitPane(voiceScroll,fxScroll),"patch-split"); // voice + fx
        patchSplit.setOrientation(Orientation.VERTICAL);
        Knob patchVolume = withClass(new Knob("patch-volume"),"patch-volume");
        bindVarControl(slot,patchVolume.getValueProperty(),v -> {
            SimpleObjectProperty<Integer> p = new SimpleObjectProperty<>(patchVolume,"patchVolume:"+slot+":"+v,0);
            bridge(d -> d.getPerf().getSlot(slot).getSettingsArea().getSettingsModule(SettingsModules.Gain)
                        .getSettingsValueProperty(ModParam.GainVolume,v),
                    new FxProperty.SimpleFxProperty<>(p,patchVolume.valueChangingProperty()),
                    PropertyBridge.id());
            return p;
        });

        bindVarControl(slot,patchEnable.selectedProperty(),v -> {
            SimpleBooleanProperty p = new SimpleBooleanProperty(patchEnable,"patchEnable:"+slot+":"+v,false);
            bridge(d -> d.getPerf().getSlot(slot).getSettingsArea().getSettingsModule(SettingsModules.Gain)
                            .getSettingsValueProperty(ModParam.GainActiveMuted,v),
                    new FxProperty.SimpleFxProperty<>(p),
                    new PropertyBridge.Iso<>() {
                        @Override
                        public Boolean to(Integer a) {
                            return a == 1;
                        }
                        @Override
                        public Integer from(Boolean b) {
                            return b ? 1 : 0;
                        }
                    });
            return p;
        });
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
                patchVolume,
                patchEnable,
                label("Visible\nLabels"),
                redCable, blueCable, yellowCable, orangeCable, purpleCable, whiteCable,
                hideCables, shakeCables
        ),"patch-bar","bar","gfont");
        VBox patchBox = withClass(new VBox(patchBar,patchSplit),"patch-box"); // patch top bar + uis

        VBox.setVgrow(patchSplit,Priority.ALWAYS);
        return patchBox;
    }

    private SegmentedButton mkVarSelector(Slot slot) {
        List<ToggleButton> varButtons = new ArrayList<>();
        for (int i = 1; i < 9; i++) {
            RadioButton b = new RadioButton(Integer.toString(i));
            b.setSelected(i==1);
            b.setFocusTraversable(false);
            varButtons.add(withClass(b,"var-button"));
            b.setUserData(i - 1);
            radioToToggle(b);
        }
        SegmentedButton varSelector = new SegmentedButton(varButtons.toArray(new ToggleButton[] {}));
        bridgeSegmentedButton(varSelector, d -> d.getPerf().getSlot(slot).getPatchSettings().variation());

        varSelector.getToggleGroup().selectedToggleProperty().addListener((v,o,n) ->
                varChanged(slot,o == null ? null : (Integer) o.getUserData(),
                        n == null ? null : (Integer) n.getUserData()));
        selectedVars.add(varSelector.getToggleGroup().selectedToggleProperty());
        return varSelector;
    }

    private Spinner<VoiceMode> mkVoicesSpinner(Slot slot) {
        SimpleObjectProperty<Integer> monoPoly = new SimpleObjectProperty<>(0);
        bridge(monoPoly,d->d.getPerf().getSlot(slot).getPatchSettings().monoPoly());

        SimpleObjectProperty<Integer> voices = new SimpleObjectProperty<>(2);
        bridge(voices,d->d.getPerf().getSlot(slot).getPatchSettings().voices());

        SimpleObjectProperty<Integer> assignedVoices = new SimpleObjectProperty<>(0);
        bridge(assignedVoices,d->d.getPerf().getSlot(slot).assignedVoices());

        ObservableList<VoiceMode> items = FXCollections.observableArrayList(VoiceMode.values());
        Spinner<VoiceMode> spinner = withClass(new Spinner<>(),"voice-spinner");
        SpinnerValueFactory.ListSpinnerValueFactory<VoiceMode> valueFactory =
                new SpinnerValueFactory.ListSpinnerValueFactory<>(items);
        spinner.setValueFactory(valueFactory);
        // Set converter to format display in editable area
        valueFactory.setConverter(new StringConverter<>() {
            @Override
            public String toString(VoiceMode voiceMode) {
                return voiceMode.getDisplayName(assignedVoices.get());
            }
            @Override public VoiceMode fromString(String s) { return null; }
        });

        AtomicBoolean skipUpdate = new AtomicBoolean(false);
        spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                skipUpdate.set(true);
                try { monoPoly.set(newVal.getMonoPoly()); }
                finally { skipUpdate.set(false); }
                voices.set(newVal.getVoices());
            }
        });

        ChangeListener<Number> syncSpinner = (obs, oldVal, newVal) -> {
            if (skipUpdate.get()) { return; }
            VoiceMode updated = VoiceMode.fromMonoPolyAndVoices(monoPoly.get(), voices.get());
            if (updated != spinner.getValue()) {
                spinner.getValueFactory().setValue(updated);
                // Force a UI update on assignedVoices change
                spinner.getEditor().setText(valueFactory.getConverter().toString(updated));
            }
        };
        monoPoly.addListener(syncSpinner);
        voices.addListener(syncSpinner);

        assignedVoices.addListener((obs, old, val) -> {
            VoiceMode current = spinner.getValue();
            if (current != null) {
                spinner.getEditor().setText(valueFactory.getConverter().toString(current));
            }
        });

        spinner.getValueFactory().setValue(VoiceMode.P2);

        return spinner;
    }

    private void bridgeSegmentedButton(SegmentedButton button, Function<Device, LibProperty<Integer>> libPropBuilder) {
        bridge(libPropBuilder,
                new FxProperty<>(button.getToggleGroup().selectedToggleProperty()) {
                    @Override
                    public void setValue(Toggle value) {
                        value.setSelected(true);
                    }
                },
                new PropertyBridge.Iso<>() {
                    @Override
                    public Toggle to(Integer integer) {
                        return button.getToggleGroup().getToggles().get(integer);
                    }

                    @Override
                    public Integer from(Toggle tab) {
                        return (Integer) tab.getUserData();
                    }
                });
    }


    private CheckBox cableCheckbox(Slot slot, String color,Function<PatchSettings,LibProperty<Boolean>> libProp) {
        CheckBox cb = withClass(new CheckBox(),"cable-" + color,"cable-checkbox");
        cb.setSelected(true);
        bindVarControl(slot,cb.selectedProperty(),v -> {
            BooleanProperty p = new SimpleBooleanProperty(cb,color + " cable",true);
            bridge(p,d->libProp.apply(d.getPerf().getSlot(slot).getPatchSettings()));
            return p;
        });
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
