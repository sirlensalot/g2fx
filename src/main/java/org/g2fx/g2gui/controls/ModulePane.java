package org.g2fx.g2gui.controls;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2gui.PropertyBridge;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.state.Device;

import java.util.ArrayList;
import java.util.List;

import static org.g2fx.g2gui.FXUtil.withClass;

public class ModulePane {

    public static final int MODULE_WIDTH = 255;
    public static final int MODULE_Y_MULT = 15;


    /**
     * Captures initial module info that is then one-way UI -> backend from then on.
     */
    public record ModuleSpec(
            int index,
            ModuleType type,
            int horiz,
            int vert,
            int color,
            int uprate,
            boolean leds,
            List<Integer> modes) {}

    private final ModuleSelector moduleSelector;
    private final Pane pane;

    private final List<PropertyBridge<?,?>> bridges = new ArrayList<>();


    public ModulePane(UIModule<UIElement> ui, ModuleSpec m) {
        int x = m.horiz;
        int y = m.vert;
        int h = ui.Height();
        int w = MODULE_WIDTH;
        moduleSelector = new ModuleSelector(m.index, "", m.type);

        List<Node> children = List.of(moduleSelector.getPane());
        pane = withClass(new Pane(FXUtil.toArray(children)),"mod-pane");
        pane.setLayoutX(x * MODULE_WIDTH);
        pane.setLayoutY(y * MODULE_Y_MULT);
        pane.setMinHeight(h * MODULE_Y_MULT);
        pane.setMinWidth(w);
    }


    public void addBridge(PropertyBridge<?, ?> bridge, Device d) {
        bridges.add(bridge);
        bridge.finalizeInit(d).run();
    }

    public Pane getPane() {
        return pane;
    }

    public ModuleSelector getModuleSelector() {
        return moduleSelector;
    }
}
