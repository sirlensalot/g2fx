package org.g2fx.g2gui.panel;

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
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2gui.Undos;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.bridge.FxProperty;
import org.g2fx.g2gui.bridge.Iso;
import org.g2fx.g2gui.controls.*;
import org.g2fx.g2gui.ui.UIElement;
import org.g2fx.g2gui.ui.UIModule;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.SettingsModules;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.PatchSettings;
import org.g2fx.g2lib.state.Slot;
import org.g2fx.g2lib.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.g2fx.g2gui.FXUtil.UI_MAX_VARIATIONS;
import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.controls.CableColor.*;

public class SlotPane {

    private final Logger log;
    private final Bridges bridges;
    private final Slot slot;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final RebindableControls<Slots.SlotAndVar> morphControls;
    private final Undos undos;

    private SplitPane.Divider divider;
    private SplitPane patchSplit;

    private final RebindableControls<Integer> varControls = new RebindableControls<>();

    private final Map<AreaId,AreaPane> areaPanes = new HashMap<>();

    private SegmentedButton varSelector;
    private Map<CableColor,CheckBox> cableCheckboxes = new TreeMap<>();
    private ToggleButton hideCables;


    public SlotPane(Bridges bridges, FXUtil.TextFieldFocusListener textFocusListener,
                    Slot slot, RebindableControls<Slots.SlotAndVar> morphControls,
                    Undos undos) {
        this.bridges = bridges;
        this.slot = slot;
        log = Util.getLogger(getClass(),slot);
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

    public void disposeModuleBridges() {
        areaPanes.values().forEach(AreaPane::disposeModuleBridges);
    }



    public <T> RebindableControl<Integer, T> bindVarControl(Property<T> control, IntFunction<Property<T>> varPropBuilder) {
        List<Property<T>> l = IntStream.range(0, UI_MAX_VARIATIONS).mapToObj(varPropBuilder).toList();
        RebindableControl<Integer, T> c = new RebindableControl<>(control, l::get);
        varControls.add(c);
        return c;
    }


    public VBox mkPatchBox() {

        HBox patchBar = mkPatchBar();

        AreaPane voicePane = new AreaPane(AreaId.Voice, bridges, this, textFocusListener, undos);
        areaPanes.put(AreaId.Voice,voicePane);
        AreaPane fxPane = new AreaPane(AreaId.Fx, bridges, this, textFocusListener, undos);
        areaPanes.put(AreaId.Fx,fxPane);

        voicePane.addSelectionListener(fxPane::clearModuleSelection);
        fxPane.addSelectionListener(voicePane::clearModuleSelection);


        patchSplit =
                withClass(new SplitPane(voicePane.getScrollPane(),fxPane.getScrollPane()),"patch-split");
        patchSplit.setOrientation(Orientation.VERTICAL);

        setupDivider();

        VBox patchBox = withClass(new VBox(patchBar,patchSplit),"patch-box");

        VBox.setVgrow(patchSplit, Priority.ALWAYS);
        return patchBox;
    }

    private void setupDivider() {
        /*
         * FX does late-occuring adjustments to the divider long after device/bridge init,
         * which can result in spurious changes to lib property values, since the value ultimately depends
         * on the exact value of the pane height. To avoid this,
         * we suppress FX changes when the user is not using the mouse.
         */
        Property<Double> mouseOnlyDividerProperty = new SimpleObjectProperty<>(0d);
        /*
         * this now does double-duty as the commit property AND the mouse-only filter.
         */
        SimpleBooleanProperty valueChanging = new SimpleBooleanProperty(false);
        /*
         * flag for backend updates to update the divider.
         */
        AtomicBoolean libUpdating = new AtomicBoolean(false);

        divider = patchSplit.getDividers().getFirst();
        Platform.runLater(() -> {
            //the only way to attach mouse listeners is to scan by class :(
            patchSplit.lookupAll(".split-pane-divider").forEach(d -> {
                d.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> valueChanging.set(true));
                d.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> valueChanging.set(false));
            });
        });

        //only update FX->Lib property when mouse is being used or backend is updating
        divider.positionProperty().addListener((c,o,n) -> {
            if (valueChanging.get() || libUpdating.get()) mouseOnlyDividerProperty.setValue(n.doubleValue());
        });


        bridges.bridge(d -> d.getPerf().getSlot(slot).getPatchSettings().height(),
                new FxProperty<>(mouseOnlyDividerProperty,valueChanging) {
                    @Override
                    public void setValue(Double value) {
                        libUpdating.set(true);
                        try {
                            divider.positionProperty().setValue(value);
                        } finally { libUpdating.set(false); }
                    }
                },
                new Iso<>() {
                    @Override
                    public Double to(Integer libHeight) {
                        return libHeight.doubleValue() / patchSplit.getHeight();
                    }
                    @Override
                    public Integer from(Double fxHeight) {
                        int h = (int) (fxHeight * patchSplit.getHeight());
                        //clip low values at 0, as fx doesn't seem to allow an actual "0" position
                        return h < 3 ? 0 : h;
                    }
                });
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
                Iso.INTEGER_NUMBER_ISO);

        Spinner<VoiceMode> voicesSpinner = mkVoicesSpinner();

        mkVarSelector();


        Button initVar = new Button("Init");

        ToggleButton patchEnable = new PowerButton().getButton();

        CheckBox redCable = cableCheckbox(Red,PatchSettings::red);
        CheckBox blueCable = cableCheckbox(Blue,PatchSettings::blue);
        CheckBox yellowCable = cableCheckbox(Yellow,PatchSettings::yellow);
        CheckBox orangeCable = cableCheckbox(Orange,PatchSettings::orange);
        CheckBox greenCable = cableCheckbox(Green,PatchSettings::purple);
        CheckBox purpleCable = cableCheckbox(Purple,PatchSettings::purple);
        CheckBox whiteCable = cableCheckbox(White,PatchSettings::white);
        hideCables = withClass(new ToggleButton("H"),"hide-cables","cable-button",FXUtil.G2_TOGGLE);
        hideCables.setFocusTraversable(false);
        hideCables.selectedProperty().addListener((c,o,l) -> manageCables(false));
        Button shakeCables = withClass(new Button("S"),"shake-cables","cable-button");
        shakeCables.setFocusTraversable(false);
        shakeCables.setOnAction(e -> manageCables(true));


        Knob patchVolume = withClass(new Knob("patch-volume", 1.0),"patch-volume");
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
                redCable, blueCable, yellowCable, orangeCable, greenCable, purpleCable, whiteCable,
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
        ObservableList<ToggleButton> varButtons = FXCollections.observableArrayList();
        for (int i = 1; i < 9; i++) {
            RadioButton b = new RadioButton(Integer.toString(i));
            b.setSelected(i==1);
            b.setFocusTraversable(false);
            varButtons.add(withClass(b,"var-button","g2-toggle"));
            b.setUserData(i - 1);
            FXUtil.radioToToggle(b);
        }
        varSelector = new SegmentedButton(varButtons);
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
        varControls.updateBinds(newVar);
    }

    private Integer getCurrentVar() {
        Toggle varToggle = varSelector.getToggleGroup().selectedToggleProperty().getValue();
        if (varToggle == null) { return null; }
        return (Integer) varToggle.getUserData();
    }

    public void updateMorphBinds() {
        Integer var = getCurrentVar();
        if (var != null) {
            morphControls.updateBinds(new Slots.SlotAndVar(slot, var));
        }
    }

    public boolean isCableVisible(Cables.Cable cable) {
        return !hideCables.isSelected() && cableCheckboxes.get(cable.color()).isSelected();
    }


    private CheckBox cableCheckbox(CableColor color, Function<PatchSettings,LibProperty<Boolean>> libProp) {
        CheckBox cb = withClass(new CheckBox(),"cable-" + color.name().toLowerCase(),"cable-checkbox");
        //logColor(color);
        cb.setSelected(true);
        cb.setFocusTraversable(false);
        bindVarControl(cb.selectedProperty(),v -> {
            BooleanProperty p = new SimpleBooleanProperty(cb,color + " cable",true);
            bridges.bridge(p,d->libProp.apply(d.getPerf().getSlot(slot).getPatchSettings()));
            return p;
        });
        cableCheckboxes.put(color,cb);
        cb.selectedProperty().addListener((c, o, n) -> manageCables(false));
        return cb;
    }

    public void manageCables(boolean redraw) {
        areaPanes.values().forEach(a -> a.manageCables(redraw));
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

    public AreaPane getAreaPane(AreaId id) {
        if (id == AreaId.Settings) { throw new IllegalArgumentException("getAreaPane: must be FX or Voice"); }
        return areaPanes.get(id);
    }

    public void updateModuleColor(int index) {
        undos.beginMulti();
        try {
            areaPanes.forEach((a, p) -> p.getSelectedModules().forEach(m -> m.color().setValue(index)));
        } finally {
            undos.commitMulti();
        }
    }

    public void unbindVarControls(List<RebindableControl<Integer,?>> bs) {
        bs.forEach(RebindableControl::unbind);
        varControls.remove(bs);
    }

    public void toggleShowCables() {
        hideCables.setSelected(!hideCables.isSelected());
    }
}
