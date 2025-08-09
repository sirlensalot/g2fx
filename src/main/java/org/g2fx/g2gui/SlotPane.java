package org.g2fx.g2gui;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.controls.Knob;
import org.g2fx.g2gui.controls.ModulePane;
import org.g2fx.g2gui.controls.UIElement;
import org.g2fx.g2gui.controls.UIModule;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.state.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.g2fx.g2gui.FXUtil.UI_MAX_VARIATIONS;
import static org.g2fx.g2gui.FXUtil.withClass;

public class SlotPane {

    private final Bridges bridges;
    private final Slot slot;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final List<RebindableControl<G2GuiApplication.SlotAndVar, ?>> morphControls;

    private Pane voicePane;
    private Pane fxPane;
    private SplitPane.Divider divider;
    private SplitPane patchSplit;

    private final List<RebindableControl<Integer,?>> varControls = new ArrayList<>();

    private SegmentedButton varSelector;


    public SlotPane(Bridges bridges, FXUtil.TextFieldFocusListener textFocusListener,
                    Slot slot, List<RebindableControl<G2GuiApplication.SlotAndVar, ?>> morphControls) {
        this.bridges = bridges;
        this.slot = slot;
        this.textFocusListener = textFocusListener;
        this.morphControls = morphControls;
    }

    private void renderModule(AreaId a, ModulePane.ModuleSpec m, PatchModule pm, Device d, UIModule<UIElement> ui) {
        Pane pane = a == AreaId.Voice ? voicePane : fxPane;
        ModulePane modulePane = new ModulePane(ui,m, textFocusListener);
        pane.getChildren().add(modulePane.getPane());
        modulePane.addBridge(bridges.bridge(modulePane.getModuleSelector().name(),dd -> pm.name()),d);
    }

    public void initModules(Device d, Map<ModuleType, UIModule<UIElement>> uiModules, List<Runnable> l) {
        for (AreaId a : AreaId.USER_AREAS) {
            for (PatchModule m : d.getPerf().getSlot(slot).getArea(a).getModules()) {
                UserModuleData md = m.getUserModuleData();
                ModulePane.ModuleSpec spec = new ModulePane.ModuleSpec(md.getIndex(), md.getType(),
                        md.horiz().get(), md.vert().get(),
                        md.color().get(), md.uprate().get(),
                        md.leds().get(), md.getModes().stream().map(LibProperty::get).toList());
                l.add(() -> renderModule(a, spec, m, d, uiModules.get(md.getType())));
            }
        }
    }





    private <T> void bindVarControl(Property<T> control, IntFunction<Property<T>> varPropBuilder) {
        List<Property<T>> l = IntStream.range(0, UI_MAX_VARIATIONS).mapToObj(varPropBuilder).toList();
        varControls.add(new RebindableControl<>(control, l::get));
    }


    public VBox mkPatchBox() {

        HBox patchBar = mkPatchBar();

        voicePane = withClass(
                new Pane(new Label("voice")),"voice-pane","area-pane","gfont");
        ScrollPane voiceScroll =
                withClass(new ScrollPane(voicePane),"voice-scroll","area-scroll");
        voiceScroll.setMinHeight(0);

        fxPane = withClass(new Pane(new Label("fx")),"fx-pane","area-pane","gfont");
        ScrollPane fxScroll = withClass(new ScrollPane(fxPane),"fx-scroll","area-scroll");
        fxScroll.setMinHeight(0);


        patchSplit =
                withClass(new SplitPane(voiceScroll,fxScroll),"patch-split");
        patchSplit.setOrientation(Orientation.VERTICAL);

        SimpleBooleanProperty valueChanging = new SimpleBooleanProperty(false);
        divider = patchSplit.getDividers().getFirst();

        Platform.runLater(() -> {
            //have to hack to get unexposed target for commit listener
            patchSplit.lookupAll(".split-pane-divider").forEach(d -> {
                d.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> valueChanging.set(false));
                d.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> valueChanging.set(true));
            });
        });

        bridges.bridge(d -> d.getPerf().getSlot(slot).getPatchSettings().height(),
                new FxProperty.SimpleFxProperty<>(divider.positionProperty(),valueChanging),
                new PropertyBridge.Iso<>() {
                    @Override
                    public Number to(Integer libHeight) {
                        return libHeight.doubleValue() / patchSplit.getHeight();
                    }

                    @Override
                    public Integer from(Number fxHeight) {
                        return (int) (fxHeight.doubleValue() * patchSplit.getHeight());
                    }
                });

        VBox patchBox = withClass(new VBox(patchBar,patchSplit),"patch-box");

        VBox.setVgrow(patchSplit, Priority.ALWAYS);
        return patchBox;
    }


    private HBox mkPatchBar() {

        TextField patchName = new TextField("slot" + slot);
        bridges.bridge(FXUtil.mkTextFieldCommitProperty(patchName,textFocusListener),
                d -> d.getPerf().getPerfSettings().getSlotSettings(slot).patchName());

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
        bridges.bridge(d -> d.getPerf().getSlot(slot).getPatchSettings().category(),
                new FxProperty<>(patchCategory.getSelectionModel().selectedIndexProperty()) {
                    @Override public void setValue(Number value) {
                        patchCategory.getSelectionModel().select(value.intValue());
                    }
                },
                new PropertyBridge.Iso<>() {
                    @Override public Number to(Integer integer) {
                        return integer;
                    }
                    @Override public Integer from(Number number) {
                        return number.intValue();
                    }
                });

        Spinner<VoiceMode> voicesSpinner = mkVoicesSpinner();

        mkVarSelector();


        Button initVar = new Button("Init");


        SVGPath powerGraphic = withClass(new SVGPath(),"power-graphic");
        powerGraphic.setContent("M -3 -3 A 4.5 4.5 0 1 0 3 -3 M 0 0 L 0 -4");
        ToggleButton patchEnable = withClass(new ToggleButton("", powerGraphic),"power-button");

        CheckBox redCable = cableCheckbox("red",PatchSettings::red);
        CheckBox blueCable = cableCheckbox("blue",PatchSettings::blue);
        CheckBox yellowCable = cableCheckbox("yellow",PatchSettings::yellow);
        CheckBox orangeCable = cableCheckbox("orange",PatchSettings::orange);
        CheckBox purpleCable = cableCheckbox("purple",PatchSettings::purple);
        CheckBox whiteCable = cableCheckbox("white",PatchSettings::white);
        ToggleButton hideCables = withClass(new ToggleButton("H"),"hide-cables","cable-button");
        Button shakeCables = withClass(new Button("S"),"shake-cables","cable-button");


        Knob patchVolume = withClass(new Knob("patch-volume"),"patch-volume");
        bindVarControl(patchVolume.getValueProperty(), v -> {
            SimpleObjectProperty<Integer> p = new SimpleObjectProperty<>(patchVolume,"patchVolume:"+ slot +":"+v,0);
            bridges.bridge(d -> d.getPerf().getSlot(slot).getSettingsArea().getSettingsModule(SettingsModules.Gain)
                            .getSettingsValueProperty(ModParam.GainVolume,v),
                    new FxProperty.SimpleFxProperty<>(p,patchVolume.valueChangingProperty()),
                    PropertyBridge.id());
            return p;
        });

        bindVarControl(patchEnable.selectedProperty(), v -> {
            SimpleBooleanProperty p = new SimpleBooleanProperty(patchEnable,"patchEnable:"+ slot +":"+v,false);
            bridges.bridge(d -> d.getPerf().getSlot(slot).getSettingsArea().getSettingsModule(SettingsModules.Gain)
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
                FXUtil.label("Patch\nName"),
                patchName,
                patchCategory,
                FXUtil.label("Voice\nMode"),
                voicesSpinner,
                new Label("Variation"),
                varSelector,
                initVar,
                FXUtil.label("Patch\nLevel"),
                patchVolume,
                patchEnable,
                FXUtil.label("Visible\nLabels"),
                redCable, blueCable, yellowCable, orangeCable, purpleCable, whiteCable,
                hideCables, shakeCables
        ),"patch-bar","bar","gfont");
        return patchBar;
    }





    private Spinner<VoiceMode> mkVoicesSpinner() {
        SimpleObjectProperty<Integer> monoPoly = new SimpleObjectProperty<>(0);
        bridges.bridge(monoPoly,d->d.getPerf().getSlot(slot).getPatchSettings().monoPoly());

        SimpleObjectProperty<Integer> voices = new SimpleObjectProperty<>(2);
        bridges.bridge(voices,d->d.getPerf().getSlot(slot).getPatchSettings().voices());

        SimpleObjectProperty<Integer> assignedVoices = new SimpleObjectProperty<>(0);
        bridges.bridge(assignedVoices,d->d.getPerf().getSlot(slot).assignedVoices());

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


    private void mkVarSelector() {
        List<ToggleButton> varButtons = new ArrayList<>();
        for (int i = 1; i < 9; i++) {
            RadioButton b = new RadioButton(Integer.toString(i));
            b.setSelected(i==1);
            b.setFocusTraversable(false);
            varButtons.add(withClass(b,"var-button"));
            b.setUserData(i - 1);
            FXUtil.radioToToggle(b);
        }
        varSelector = new SegmentedButton(varButtons.toArray(new ToggleButton[] {}));
        bridges.bridgeSegmentedButton(varSelector, d -> d.getPerf().getSlot(slot).getPatchSettings().variation());

        varSelector.getToggleGroup().selectedToggleProperty().addListener((v, o, n) ->
                varChanged(o == null ? null : (Integer) o.getUserData(),
                        n == null ? null : (Integer) n.getUserData()));
    }


    private void varChanged(Integer oldVar, Integer newVar) {
        for (RebindableControl<Integer,?> vc : varControls) {
            vc.bind(newVar);
        }
        updateMorphBinds();
    }


    public void updateMorphBinds() {
        Toggle varToggle = varSelector.getToggleGroup().selectedToggleProperty().getValue();
        if (varToggle == null) { return; }
        int var = (Integer) varToggle.getUserData();
        for (RebindableControl<G2GuiApplication.SlotAndVar, ?> mc : morphControls) {
            mc.bind(new G2GuiApplication.SlotAndVar(slot,var));
        }
    }




    private CheckBox cableCheckbox(String color, Function<PatchSettings,LibProperty<Boolean>> libProp) {
        CheckBox cb = withClass(new CheckBox(),"cable-" + color,"cable-checkbox");
        cb.setSelected(true);
        bindVarControl(cb.selectedProperty(),v -> {
            BooleanProperty p = new SimpleBooleanProperty(cb,color + " cable",true);
            bridges.bridge(p,d->libProp.apply(d.getPerf().getSlot(slot).getPatchSettings()));
            return p;
        });
        return cb;
    }

    public void maximizeAreaPane(AreaId area) {
        divider.setPosition(area == AreaId.Fx ? 0 : patchSplit.getHeight());
    }

    public Slot getSlot() {
        return slot;
    }

    public void selectVar(int i) {
        varSelector.getToggleGroup().getToggles().get(i).setSelected(true);
    }
}
