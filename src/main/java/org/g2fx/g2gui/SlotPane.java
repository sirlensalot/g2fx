package org.g2fx.g2gui;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.controls.Knob;
import org.g2fx.g2gui.controls.PowerButton;
import org.g2fx.g2gui.controls.UIElement;
import org.g2fx.g2gui.controls.UIModule;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.PatchSettings;
import org.g2fx.g2lib.state.Slot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.g2fx.g2gui.FXUtil.UI_MAX_VARIATIONS;
import static org.g2fx.g2gui.FXUtil.withClass;

public class SlotPane {

    private final Bridges bridges;
    private final Slot slot;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final List<RebindableControl<Slots.SlotAndVar, ?>> morphControls;
    private final Undos undos;

    private SplitPane.Divider divider;
    private SplitPane patchSplit;

    private final List<RebindableControl<Integer,?>> varControls = new ArrayList<>();

    private final Map<AreaId,AreaPane> areaPanes = new HashMap<>();

    private SegmentedButton varSelector;


    public SlotPane(Bridges bridges, FXUtil.TextFieldFocusListener textFocusListener,
                    Slot slot, List<RebindableControl<Slots.SlotAndVar, ?>> morphControls,
                    Undos undos) {
        this.bridges = bridges;
        this.slot = slot;
        this.textFocusListener = textFocusListener;
        this.morphControls = morphControls;
        this.undos = undos;
    }

    public void initModules(Device d, Map<ModuleType, UIModule<UIElement>> uiModules, List<Runnable> l) {
        // on device thread
        areaPanes.values().forEach(a -> a.initModules(d,uiModules,l));
        // add var rebind, as it will get skipped if the var selector doesn't change.
        l.add(() -> updateVarBinds(getCurrentVar()));
    }


    public void clearModules() {
        //on fx thread, and assumes bridges are already disposed
        areaPanes.values().forEach(AreaPane::clearModules);
    }



    public <T> void bindVarControl(Property<T> control, IntFunction<Property<T>> varPropBuilder) {
        List<Property<T>> l = IntStream.range(0, UI_MAX_VARIATIONS).mapToObj(varPropBuilder).toList();
        varControls.add(new RebindableControl<>(control, l::get));
    }


    public VBox mkPatchBox() {

        HBox patchBar = mkPatchBar();

        AreaPane voicePane = new AreaPane(AreaId.Voice, bridges, this, textFocusListener);
        areaPanes.put(AreaId.Voice,voicePane);
        AreaPane fxPane = new AreaPane(AreaId.Fx, bridges, this, textFocusListener);
        areaPanes.put(AreaId.Fx,fxPane);

        voicePane.addSelectionListener(fxPane::clearModuleSelection);
        fxPane.addSelectionListener(voicePane::clearModuleSelection);


        patchSplit =
                withClass(new SplitPane(voicePane.getScrollPane(),fxPane.getScrollPane()),"patch-split");
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
                new Iso<>() {
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
        bridges.bridge(FXUtil.mkTextFieldCommitProperty(patchName,textFocusListener, 16),
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
                FxProperty.adaptReadOnly(patchCategory.getSelectionModel().selectedIndexProperty(),
                        value -> patchCategory.getSelectionModel().select(value.intValue())),
               new Iso<>() {
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

        ToggleButton patchEnable = new PowerButton().getButton();

        CheckBox redCable = cableCheckbox("red",PatchSettings::red);
        CheckBox blueCable = cableCheckbox("blue",PatchSettings::blue);
        CheckBox yellowCable = cableCheckbox("yellow",PatchSettings::yellow);
        CheckBox orangeCable = cableCheckbox("orange",PatchSettings::orange);
        CheckBox purpleCable = cableCheckbox("purple",PatchSettings::purple);
        CheckBox whiteCable = cableCheckbox("white",PatchSettings::white);
        ToggleButton hideCables = withClass(new ToggleButton("H"),"hide-cables","cable-button",FXUtil.G2_TOGGLE);
        Button shakeCables = withClass(new Button("S"),"shake-cables","cable-button");


        Knob patchVolume = withClass(new Knob("patch-volume"),"patch-volume");
        bindVarControl(patchVolume.getValueProperty(), v -> {
            SimpleObjectProperty<Integer> p = new SimpleObjectProperty<>(patchVolume,"patchVolume:"+ slot +":"+v,0);
            bridges.bridge(d -> d.getPerf().getSlot(slot).getSettingsArea().getSettingsModule(SettingsModules.Gain)
                            .getSettingsValueProperty(ModParam.GainVolume,v),
                    new FxProperty.SimpleFxProperty<>(p,patchVolume.valueChangingProperty()),
                    Iso.id());
            return p;
        });

        bindVarControl(patchEnable.selectedProperty(), v -> {
            SimpleBooleanProperty p = new SimpleBooleanProperty(patchEnable,"patchEnable:"+ slot +":"+v,false);
            bridges.bridge(d -> d.getPerf().getSlot(slot).getSettingsArea().getSettingsModule(SettingsModules.Gain)
                            .getSettingsValueProperty(ModParam.GainActiveMuted,v),
                    new FxProperty.SimpleFxProperty<>(p),
                    Iso.BOOL_PARAM_ISO);
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

        SimpleObjectProperty<Integer> assignedVoices = new SimpleObjectProperty<>(0);
        bridges.bridge(assignedVoices,d->d.getPerf().getSlot(slot).assignedVoices());

        ObservableList<VoiceMode> items = FXCollections.observableArrayList(VoiceMode.ALL);
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

        bridges.bridge(d -> d.getPerf().getSlot(slot).getPatchSettings().voiceMode(),
                FxProperty.adaptReadOnly(spinner.valueProperty(),
                    value -> spinner.getValueFactory().setValue(value)),
                Iso.id());

        assignedVoices.addListener((obs, old, val) -> {
            VoiceMode current = spinner.getValue();
            if (current != null) {
                spinner.getEditor().setText(valueFactory.getConverter().toString(current));
            }
        });

        spinner.getValueFactory().setValue(VoiceMode.MONO);

        return spinner;
    }


    private void mkVarSelector() {
        List<ToggleButton> varButtons = new ArrayList<>();
        for (int i = 1; i < 9; i++) {
            RadioButton b = new RadioButton(Integer.toString(i));
            b.setSelected(i==1);
            b.setFocusTraversable(false);
            varButtons.add(withClass(b,"var-button","g2-toggle"));
            b.setUserData(i - 1);
            FXUtil.radioToToggle(b);
        }
        varSelector = new SegmentedButton(varButtons.toArray(new ToggleButton[] {}));
        bridges.bridgeSegmentedButton(varSelector, d -> d.getPerf().getSlot(slot).getPatchSettings().variation());

        varSelector.getToggleGroup().selectedToggleProperty().addListener((v, o, n) ->
                varChanged((Integer) n.getUserData()));
    }


    private void varChanged(Integer newVar) {
        updateVarBinds(newVar);
        updateMorphBinds();
    }

    private void updateVarBinds(Integer newVar) {
        if (newVar == null) { return; }
        for (RebindableControl<Integer,?> vc : varControls) {
            vc.bind(newVar);
        }
    }

    private Integer getCurrentVar() {
        Toggle varToggle = varSelector.getToggleGroup().selectedToggleProperty().getValue();
        if (varToggle == null) { return null; }
        return (Integer) varToggle.getUserData();
    }

    public void updateMorphBinds() {
        Integer var = getCurrentVar();
        if (var != null) {
            for (RebindableControl<Slots.SlotAndVar, ?> mc : morphControls) {
                mc.bind(new Slots.SlotAndVar(slot, var));
            }
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

    public void updateModuleColor(int index) {
        undos.beginMulti();
        try {
            areaPanes.forEach((a, p) -> p.getSelectedModules().forEach(m -> m.color().setValue(index)));
        } finally {
            undos.commitMulti();
        }
    }
}
