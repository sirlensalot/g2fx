package org.g2fx.g2gui;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import org.g2fx.g2gui.controls.ModulePane;
import org.g2fx.g2gui.controls.UIElement;
import org.g2fx.g2gui.controls.UIModule;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.state.UserModuleData;

import java.util.*;

import static org.g2fx.g2gui.FXUtil.withClass;

public class AreaPane {

    private final AreaId areaId;
    private final Bridges bridges;
    private final SlotPane slotPane;
    private final Pane areaPane;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final ScrollPane scrollPane;

    private final List<ModulePane> modulePanes = new ArrayList<>();

    private final Set<ModulePane> selectedModules = new HashSet<>();
    private final Rectangle selectedRect;
    private List<Rectangle> dragGhosts = new ArrayList<>();
    private Point2D dragOrigin;
    private Runnable selectionListener;


    enum SelectionStatus {
        IN_MODULE,
        IN_PANEL,
        DRAGGING
    }


    public AreaPane(AreaId areaId, Bridges bridges, SlotPane slotPane, FXUtil.TextFieldFocusListener textFocusListener) {
        this.areaId = areaId;
        this.bridges = bridges;
        this.slotPane = slotPane;
        areaPane = withClass(
                new Pane(new Label(areaId.name())),"area-pane","gfont");
        this.textFocusListener = textFocusListener;
        scrollPane = withClass(new ScrollPane(areaPane),"area-scroll");
        scrollPane.setMinHeight(0);
        selectedRect = withClass(new Rectangle(),"selected-rect");
        selectedRect.setVisible(false);
        setupSelectionDragging();
        areaPane.getChildren().add(selectedRect);
    }

    private void setupSelectionDragging() {
        areaPane.setOnMousePressed(e -> {
            for (ModulePane mp : modulePanes) {
                if (mp.getPane().getBoundsInParent().contains(e.getX(),e.getY())) {
                    selectedRect.setUserData(SelectionStatus.IN_MODULE);
                    return;
                }
            }
            selectedRect.setX(e.getX());
            selectedRect.setY(e.getY());
            selectedRect.setWidth(0);
            selectedRect.setHeight(0);
            selectedRect.setUserData(SelectionStatus.IN_PANEL);
        });
        areaPane.setOnMouseDragged(e -> {
            if (selectedRect.getUserData() == SelectionStatus.IN_PANEL) {
                selectedRect.setVisible(true);
                selectedRect.setUserData(SelectionStatus.DRAGGING);
            }
            if (selectedRect.isVisible()) {
                selectedRect.setWidth(e.getX() - selectedRect.getX());
                selectedRect.setHeight(e.getY() - selectedRect.getY());
            }
        });
        areaPane.setOnMouseReleased(e -> {
            if (selectedRect.isVisible()) { // e.g. DRAGGING
                Bounds selBounds = selectedRect.getBoundsInParent();
                if (!e.isShiftDown()) {
                    clearModuleSelection();
                }
                for (ModulePane mp : modulePanes) {
                    if (mp.getPane().getBoundsInParent().intersects(selBounds)) {
                        selectModule(mp);
                    }
                }
                selectedRect.setVisible(false);
            } else if (selectedRect.getUserData() == SelectionStatus.IN_PANEL) {
                clearModuleSelection();
            }
            selectedRect.setUserData(null);
        });
    }

    public void initModules(Device d, Map<ModuleType, UIModule<UIElement>> uiModules, List<Runnable> l) {
        // on device thread
        for (PatchModule m : d.getPerf().getSlot(slotPane.getSlot()).getArea(areaId).getModules()) {
            UserModuleData md = m.getUserModuleData();
            ModulePane.ModuleSpec spec = new ModulePane.ModuleSpec(md.getIndex(), md.getType(),
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
        setupModuleMouseHandling(modulePane);
        modulePane.getModuleBridges().forEach(b -> b.finalizeInit(d).run());
    }

    private void setupModuleMouseHandling(ModulePane modulePane) {
        Pane pane = modulePane.getPane();
        pane.setOnMouseClicked(e -> {
            if (e.isShortcutDown()) {
                if (selectedModules.contains(modulePane)) {
                    selectedModules.remove(modulePane);
                    modulePane.setSelected(false);
                } else {
                    selectModule(modulePane);
                }
            } else {
                clearModuleSelection();
                selectModule(modulePane);
            }
            e.consume();
        });
        pane.setOnMousePressed(e -> {
            if (modulePane.isSelected()) {
                dragGhosts.clear();;
                dragOrigin = new Point2D(e.getSceneX(),e.getSceneY());
            }
        });
        pane.setOnMouseDragged(e -> {
            if (dragOrigin != null) {
                if (dragGhosts.isEmpty()) {
                    for (ModulePane m : selectedModules) {
                        Pane p = m.getPane();
                        Rectangle r = withClass(new Rectangle(p.getLayoutX(),p.getLayoutY(),p.getWidth(),p.getHeight()),"selected-rect");
                        r.setUserData(new Point2D(r.getLayoutX(),r.getLayoutY()));
                        areaPane.getChildren().add(r);
                        dragGhosts.add(r);
                    }
                }
                for (Rectangle r : dragGhosts) {
                    Point2D o = (Point2D) r.getUserData();
                    r.setLayoutX(o.getX() + (e.getSceneX() - dragOrigin.getX()));
                    r.setLayoutY(o.getY() + (e.getSceneY() - dragOrigin.getY()));
                }
            }
        });
        pane.setOnMouseReleased(e -> {
            if (dragOrigin != null && !dragGhosts.isEmpty()) {
                System.out.println("end drag");
            }
            areaPane.getChildren().removeAll(dragGhosts);
            dragGhosts.clear();
            dragOrigin = null;
        });
    }

    private void selectModule(ModulePane modulePane) {
        selectedModules.add(modulePane);
        modulePane.setSelected(true);
        selectionListener.run();
    }

    public void clearModuleSelection() {
        selectedModules.forEach(p -> p.setSelected(false));
        selectedModules.clear();
    }

    public Set<ModulePane> getSelectedModules() {
        return selectedModules;
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


    public void addSelectionListener(Runnable r) {
        this.selectionListener = r;
    }
}
