package org.g2fx.g2gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2gui.controls.UIElement;
import org.g2fx.g2gui.controls.UIModule;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.Slot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.g2fx.g2gui.FXUtil.withClass;

public class Slots {

    private TabPane slotTabs;
    private SegmentedButton slotBar;
    private final List<SlotPane> slotPanes = new ArrayList<>();

    private final List<RebindableControl<Integer,?>> slotControls = new ArrayList<>();


    private final Map<ModuleType, UIModule<UIElement>> uiModules;



    public record SlotAndVar(Slot slot,Integer var) {}

    private final List<RebindableControl<SlotAndVar,?>> morphControls = new ArrayList<>();

    private final Bridges bridges;


    public Slots(Bridges bridges) throws Exception {
        uiModules = UIModule.readModuleUIs();
        this.bridges = bridges;
    }


    public List<Runnable> initModules(Device d) {
        List<Runnable> fxUpdates = new ArrayList<>();
        slotPanes.forEach(s -> s.initModules(d,uiModules,fxUpdates));
        return fxUpdates;
    }


    public Runnable clearModules() {
        // on device thread, return fx action
        return () -> {
            slotPanes.forEach(s -> s.clearModules());
        };
    }


    public TabPane mkSlotTabs(FXUtil.TextFieldFocusListener textFocusListener) {
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


    public SegmentedButton mkSlotBar(Bridges bridges) {

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



        slotBar = withClass(new SegmentedButton(sbs.toArray(new ToggleButton[]{})), "slot-bar");
        bridges.bridgeSegmentedButton(slotBar, d -> d.getPerf().getPerfSettings().selectedSlot());

        slotBar.getToggleGroup().selectedToggleProperty().addListener((v, o, n) ->
                slotChanged(o == null ? null : (Integer) o.getUserData(),
                        n == null ? null : (Integer) n.getUserData()));
        return slotBar;
    }


    private void slotChanged(Integer oldSlot, Integer newSlot) {
        slotTabs.getSelectionModel().select(newSlot);
        for (RebindableControl<Integer,?> control : slotControls) {
            control.bind(newSlot);
        }
        getSelectedSlotPane().updateMorphBinds();
    }

    public <T> void bindMorphControl(Property<T> control, Function<SlotAndVar,Property<T>> propBuilder) {
        List<List<Property<T>>> props = Arrays.stream(Slot.values()).map(s ->
                IntStream.range(0, FXUtil.UI_MAX_VARIATIONS).mapToObj(v -> propBuilder.apply(new SlotAndVar(s,v))).toList()).toList();
        morphControls.add(new RebindableControl<>(control,sv -> props.get(sv.slot.ordinal()).get(sv.var)));
    }





    public List<SlotPane> getAll() {
        return slotPanes;
    }


    public SlotPane getSelectedSlotPane() {
        return slotPanes.get(slotTabs.getSelectionModel().getSelectedIndex());
    }

    public void selectSlot(int slot) {
        slotBar.getToggleGroup().getToggles().get(slot).setSelected(true);
    }

    public <T> void bindSlotControl(Property<T> control, Function<Slot,Property<T>> slotPropBuilder) {
        List<Property<T>> l = Arrays.stream(Slot.values()).map(slotPropBuilder).toList();
        slotControls.add(new RebindableControl<>(control, l::get));
    }
}
