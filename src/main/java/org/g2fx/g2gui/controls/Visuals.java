package org.g2fx.g2gui.controls;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.g2fx.g2gui.bridge.Bridger;
import org.g2fx.g2gui.bridge.FxProperty;
import org.g2fx.g2gui.bridge.Iso;
import org.g2fx.g2gui.panel.SlotPane;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.Visual;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.state.PatchVisual;

import java.util.List;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.panel.ModulePane.layout;

public class Visuals {

    private final Bridger bridges;
    private final ParamListener paramListener;
    private final PatchModule patchModule;
    private final SlotPane slotPane;

    public record LedControl(Node control,Property<Boolean> lit) {}

    public Visuals(Bridger bridges, ParamListener paramListener, PatchModule patchModule, SlotPane slotPane) {
        this.bridges = bridges;
        this.paramListener = paramListener;
        this.patchModule = patchModule;
        this.slotPane = slotPane;
    }

    public Node mkLed(UIElements.Led c) {
        if (c.Type() == UIElements.LedType.Green) {
            if (c.LedGroup() == null) { //Green, no group: use GroupId
                return mkSingleLed(c);
            } else {
                return mkGroupLed(c);
            }
        } else {
            return mkSequencerLed(c);
        }
    }

    private Node mkSequencerLed(UIElements.Led c) {
        LedControl ctl = mkLed(c,12,5,"led-sequencer"); //ledgroup(Visual.LedGroupType.Radio,
        bridgeGroupLed(c, ctl);
        return ctl.control();
    }

    private Node mkGroupLed(UIElements.Led c) {
        LedControl ctl = mkGreenLed(c);
        bridgeGroupLed(c, ctl);
        return ctl.control();
    }

    private void bridgeGroupLed(UIElements.Led c, LedControl ctl) {
        bridges.bridge(d -> {
                    List<PatchVisual> leds = d.getPerf().getSlot(slotPane.getSlot()).getVisuals().getMetersAndGroups();
                    return findVisual(c.GroupId(), leds, Visual.VisualType.LedGroup);
                },
                new FxProperty.SimpleFxProperty<>(ctl.lit()),
                new Iso<>() {
                    @Override
                    public Boolean to(Integer v) {
                        return v.intValue() == c.CodeRef();
                    }

                    @Override
                    public Integer from(Boolean aBoolean) {
                        return 0; // one-way
                    }
                });
    }

    private LibProperty<Integer> findVisual(int groupId, List<PatchVisual> leds, Visual.VisualType type) {
        for (PatchVisual led : leds) {
            if (led.getModule() == patchModule && led.getVisual().index() == groupId) {
                //System.out.println("Led: " + led + ", " + this);
                if (led.getVisual().type() != type) {
                    throw new IllegalArgumentException("findVisual: matching visual of wrong type " + type + ": " + led);
                }
                return led.value();
            }
        }
        throw new IllegalStateException("Could not locate led idx " + groupId + ", " + this);
    }

    private Node mkSingleLed(UIElements.Led c) {
        LedControl ctl = mkGreenLed(c);
        bridges.bridge(d -> {
                    List<PatchVisual> leds = d.getPerf().getSlot(slotPane.getSlot()).getVisuals().getLeds();
                    return findVisual(c.GroupId(), leds, Visual.VisualType.Led);
                },
                new FxProperty.SimpleFxProperty<>(ctl.lit()),
                Iso.BOOL_PARAM_ISO);
        return ctl.control();
    }

    private static LedControl mkGreenLed(UIElements.Led c) {
        return mkLed(c, 7, 7, "led-green");
    }

    private static LedControl mkLed(UIElements.Led c, int width, int height, String style) {
        Rectangle r = withClass(new Rectangle(width, height), style,"led-green-off");
        layout(c,r);
        Property<Boolean> lit = new SimpleObjectProperty<>(false);
        lit.addListener((cc,o,n) -> {
            r.getStyleClass().remove(n ? "led-green-off" : "led-green-on");
            r.getStyleClass().add(n ? "led-green-on" : "led-green-off");
        });
        LedControl ctl = new LedControl(r, lit);
        return ctl;
    }

    public Node mkMeter(UIElements.MiniVU c) {
        VuMeter v = new VuMeter(c);
        bridges.bridge(v.level(),d -> {
                    List<PatchVisual> leds = d.getPerf().getSlot(slotPane.getSlot()).getVisuals().getMetersAndGroups();
                    return findVisual(c.GroupId(), leds, Visual.VisualType.Meter);
                });
        return v.getControl();
    }
}
