package org.g2fx.g2gui.controls;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.g2fx.g2gui.bridge.Bridger;
import org.g2fx.g2gui.bridge.FxProperty;
import org.g2fx.g2gui.bridge.Iso;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.Visual;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.state.PatchVisual;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.panel.ModulePane.layout;

public class Visuals {

    private final Bridger<PatchModule> bridges;
    private final Map<Integer,LedControl> leds = new TreeMap<>();
    private final Map<Integer,Map<Integer,LedControl>> ledGroups = new TreeMap<>();
    private final Map<Integer,VuMeter> meters = new TreeMap<>();

    public record LedControl(Node control,Property<Boolean> lit,int group,int codeRef) {}

    public Visuals(Bridger<PatchModule> bridges) {
        this.bridges = bridges;
    }

    public Node mkLed(UIElements.Led c) {
        if (c.Type() == UIElements.LedType.Green) {
            if (c.LedGroup() == null) { //Green, no group: use GroupId
                return addLed(mkSingleLed(c));
            } else {
                return addLedGroup(mkGroupLed(c));
            }
        } else {
            return addLedGroup(mkSequencerLed(c));
        }
    }

    private Node addLed(LedControl c) {
        leds.put(c.group(),c);
        return c.control();
    }

    private Node addLedGroup(LedControl c) {
        ledGroups.computeIfAbsent(c.group,(_ -> new TreeMap<>()));
        ledGroups.get(c.group).put(c.codeRef,c);
        return c.control();
    }


    private LedControl mkSequencerLed(UIElements.Led c) {
        LedControl ctl = mkLed(c,12,5,"led-sequencer"); //ledgroup(Visual.LedGroupType.Radio,
        bridgeGroupLed(c, ctl);
        return ctl;
    }

    private LedControl mkGroupLed(UIElements.Led c) {
        LedControl ctl = mkGreenLed(c);
        bridgeGroupLed(c, ctl);
        return ctl;
    }

    private void bridgeGroupLed(UIElements.Led c, LedControl ctl) {
        bridges.bridge(d -> {
                    List<PatchVisual> leds = d.getMetersAndGroups();
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
            if (led.getVisual().index() == groupId) {
                //System.out.println("Led: " + led + ", " + this);
                if (led.getVisual().type() != type) {
                    throw new IllegalArgumentException("findVisual: matching visual of wrong type " + type + ": " + led);
                }
                return led.value();
            }
        }
        throw new IllegalStateException("Could not locate led idx " + groupId + ", " + this);
    }

    private LedControl mkSingleLed(UIElements.Led c) {
        LedControl ctl = mkGreenLed(c);
        bridges.bridge(d -> {
                    List<PatchVisual> leds = d.getLeds();
                    return findVisual(c.GroupId(), leds, Visual.VisualType.Led);
                },
                new FxProperty.SimpleFxProperty<>(ctl.lit()),
                Iso.BOOL_PARAM_ISO);
        return ctl;
    }

    private static LedControl mkGreenLed(UIElements.Led c) {
        return mkLed(c, 7, 7, "led-green");
    }

    private static LedControl mkLed(UIElements.Led c, int width, int height, String style) {
        Rectangle r = withClass(new Rectangle(width, height), style,"led-green-off");
        layout(c,r);
        Property<Boolean> lit = new SimpleObjectProperty<>(false);
        lit.addListener((_, _, n) -> {
            r.getStyleClass().remove(n ? "led-green-off" : "led-green-on");
            r.getStyleClass().add(n ? "led-green-on" : "led-green-off");
        });
        LedControl ctl = new LedControl(r, lit, c.GroupId(),c.CodeRef());
        return ctl;
    }

    public Node mkMeter(UIElements.MiniVU c) {
        VuMeter v = new VuMeter(c);
        bridges.bridge(v.level(),d -> {
                    List<PatchVisual> leds = d.getMetersAndGroups();
                    return findVisual(c.GroupId(), leds, Visual.VisualType.Meter);
                });
        meters.put(c.GroupId(),v);
        return v.getControl();
    }


    public LedControl getLed(int group) {
        return leds.get(group);
    }
    public VuMeter getMeter(int group) {
        return meters.get(group);
    }
    public LedControl getLedGroup(int group,int codeRef) {
        return ledGroups.get(group).get(codeRef);
    }
}
