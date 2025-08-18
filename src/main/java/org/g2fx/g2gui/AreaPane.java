package org.g2fx.g2gui;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import org.g2fx.g2gui.controls.ModulePane;
import org.g2fx.g2gui.controls.UIElement;
import org.g2fx.g2gui.controls.UIModule;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.state.UserModuleData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.g2fx.g2gui.FXUtil.withClass;

public class AreaPane {

    private final AreaId areaId;
    private final Bridges bridges;
    private final SlotPane slotPane;
    private final Pane areaPane;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final ScrollPane scrollPane;

    private final List<ModulePane> modulePanes = new ArrayList<>();


    public AreaPane(AreaId areaId, Bridges bridges, SlotPane slotPane, FXUtil.TextFieldFocusListener textFocusListener) {
        this.areaId = areaId;
        this.bridges = bridges;
        this.slotPane = slotPane;
        areaPane = withClass(
                new Pane(new Label(areaId.name())),"area-pane","gfont");
        this.textFocusListener = textFocusListener;
        scrollPane = withClass(new ScrollPane(areaPane),"area-scroll");
        scrollPane.setMinHeight(0);
    }

    public void initModules(Device d, Map<ModuleType, UIModule<UIElement>> uiModules, List<Runnable> l) {
        // on device thread
        for (PatchModule m : d.getPerf().getSlot(slotPane.getSlot()).getArea(areaId).getModules()) {
            UserModuleData md = m.getUserModuleData();
            ModulePane.ModuleSpec spec = new ModulePane.ModuleSpec(md.getIndex(), md.getType(),
                    md.horiz().get(), md.vert().get(),
                    md.color().get(), md.uprate().get(),
                    md.leds().get(), md.getModes().stream().map(LibProperty::get).toList());
            l.add(() -> renderModule(spec, m, d, uiModules.get(md.getType())));
        }
    }


    private void renderModule(ModulePane.ModuleSpec m, PatchModule pm, Device d, UIModule<UIElement> ui) {
        // on fx thread
        ModulePane modulePane = new ModulePane(ui,m, textFocusListener, bridges, pm, slotPane);
        modulePanes.add(modulePane);
        areaPane.getChildren().add(modulePane.getPane());
        modulePane.getModuleBridges().forEach(b -> b.finalizeInit(d).run());
    }

    public void clearModules() {
        for (ModulePane m : modulePanes) {
            areaPane.getChildren().remove(m.getPane());
            bridges.remove(m.getModuleBridges());
        }
        modulePanes.clear();
    }



    public Pane getAreaPane() {
        return areaPane;
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }
}
