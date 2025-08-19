package org.g2fx.g2gui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import org.g2fx.g2gui.*;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.state.UserModuleData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.g2fx.g2gui.FXUtil.label;
import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.controls.UIElements.Orientation.Vertical;

public class ModulePane {

    public static final int MODULE_WIDTH = 255;
    public static final int MODULE_Y_MULT = 15;
    public static final String MODULE_SELECTED = "module-selected";
    private final PatchModule patchModule;
    private final Bridges bridges;
    private final UIModule<UIElement> ui;
    private final SlotPane parent;
    private boolean selected;


    /**
     * Captures initial module info that is then one-way UI -> backend from then on.
     */
    public record ModuleSpec(
            int index,
            ModuleType type,
            int color,
            int uprate,
            boolean leds,
            List<Integer> modes) {}

    private final Pane pane;
    private final ModuleType type;

    private final List<PropertyBridge<?,?>> moduleBridges = new ArrayList<>();

    private final SimpleObjectProperty<UserModuleData.Coords> coords =
            new SimpleObjectProperty<>(new UserModuleData.Coords(0,0));
    
    public ModulePane(UIModule<UIElement> ui, ModuleSpec m,
                      FXUtil.TextFieldFocusListener textFocusListener,
                      Bridges bridges, PatchModule pm, SlotPane parent) {
        int h = ui.Height();
        type = m.type;
        this.bridges = bridges;
        this.patchModule = pm;
        this.ui = ui;
        this.parent = parent;
        ModuleSelector moduleSelector = new ModuleSelector(m.index, "", m.type, textFocusListener);

        List<Node> children = new ArrayList<>(List.of(moduleSelector.getPane()));
        children.addAll(renderControls());
        pane = withClass(new Pane(FXUtil.toArray(children)),"mod-pane");
        pane.setMinHeight(h * MODULE_Y_MULT);
        pane.setMinWidth(MODULE_WIDTH);

        addBridge(bridges.bridge(moduleSelector.name(), dd -> patchModule.name()));

        addBridge(bridges.bridge(d->patchModule.getUserModuleData().coords(), new FxProperty.SimpleFxProperty<>(coords),Iso.id()));
        coords.addListener((c,o,n) -> {
            pane.setLayoutX(n.column()*MODULE_WIDTH);
            pane.setLayoutY(n.row()*MODULE_Y_MULT);
        });
    }


    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) pane.getStyleClass().add(MODULE_SELECTED);
        else pane.getStyleClass().remove(MODULE_SELECTED);
    }

    public boolean isSelected() {
        return selected;
    }

    private Collection<? extends Node> renderControls() {
        List<Node> cs = new ArrayList<>(ui.Controls().size());
        for (UIElement e : ui.Controls()) {
            cs.add(renderControl(e));
        }
        return cs;
    }

    private Node renderControl(UIElement e) {
        return switch (e) {
            case UIElements.ButtonText c -> {
                IndexParam ip = resolveParam(c.Control());
                if (ip.param().param() == ModParam.ActiveMonitor) {
                    yield mkPowerButton(c, ip);
                } else if (c.Images() != null) {
                    yield empty(e); //TODO
                } else if (c.Type() == UIElements.ButtonType.Check) {
                    yield mkTextToggle(c, ip);
                } else {
                    yield mkTextMomentary(c,ip);
                }
            }
            case UIElements.Text c -> layout(e,label(c.Text()));
            case UIElements.Line c -> {
                boolean vertical = Vertical == c.Orientation();
                yield withClass(new Line(c.XPos(),c.YPos(),
                        vertical ? c.XPos() : c.XPos() + c.Length(),
                        vertical ? c.YPos() + c.Length() : c.YPos()
                        ),"module-line","module-line-"+c.Weight());
            }
            default -> empty(e);
        };
    }

    private Node empty(UIElement e) {
        //System.out.println("TODO: " + e);
        return new Pane();
    }

    private ToggleButton mkPowerButton(UIElements.ButtonText c, IndexParam ip) {
        final ToggleButton b = new PowerButton().getButton();
        return mkToggle(c, ip, b, b.selectedProperty());
    }

    private Node mkTextMomentary(UIElements.ButtonText c, IndexParam ip) {
        MomentaryButton b = layout(c,withClass(new MomentaryButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE));
        return mkToggle(c,ip,b,b.selectedProperty());
    }

    private ToggleButton mkTextToggle(UIElements.ButtonText c, IndexParam ip) {
        ToggleButton b = withClass(new ToggleButton(c.Text()), "text-toggle", FXUtil.G2_TOGGLE);
        return mkToggle(c, ip, b, b.selectedProperty());
    }

    private <T extends Node> T mkToggle(UIElement c, IndexParam ip, T b, BooleanProperty selectedProperty) {
        layout(c, b);
        parent.bindVarControl(selectedProperty, v -> {
            SimpleBooleanProperty p =
                    new SimpleBooleanProperty(b,type.shortName + ":" + ip.param().name() +":"+v,false);
            moduleBridges.add(bridges.bridge(d -> patchModule.getParamValueProperty(v, ip.index),
                    new FxProperty.SimpleFxProperty<>(p),
                    Iso.BOOL_PARAM_ISO));
            return p;
                });
        return b;
    }

    private static <T extends Node> T layout(UIElement c, T b) {
        b.setLayoutX(c.XPos());
        b.setLayoutY(c.YPos());
        return b;
    }

    record IndexParam(NamedParam param, int index) {}
    
    private IndexParam resolveParam(String control) {
        for (int i = 0; i < type.getParams().size(); i++) {
            NamedParam p = type.getParams().get(i);
            if (p.name().equals(control)) {
                return new IndexParam(p,i);
            }
        }
        throw new IllegalArgumentException("Bad control name: " + control + ", module type: " + type);
    }


    public void addBridge(PropertyBridge<?, ?> bridge) {
        moduleBridges.add(bridge);
    }

    public Pane getPane() {
        return pane;
    }

    public List<PropertyBridge<?, ?>> getModuleBridges() {
        return moduleBridges;
    }
}
