package org.g2fx.g2gui.panel;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2gui.Undos;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.controls.Cables;
import org.g2fx.g2gui.ui.UIElement;
import org.g2fx.g2gui.ui.UIModule;
import org.g2fx.g2lib.model.Connector;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.state.*;

import java.util.*;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.panel.ModulePane.GRID_X;
import static org.g2fx.g2gui.panel.ModulePane.GRID_Y;
import static org.g2fx.g2lib.model.Connector.PortType.In;
import static org.g2fx.g2lib.model.Connector.PortType.Out;

public class AreaPane {

    private final AreaId areaId;
    private final Bridges bridges;
    private final SlotPane slotPane;
    private final Pane areaPane;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final Undos undos;
    private final ScrollPane scrollPane;

    private final Map<Integer,ModulePane> modulePanes = new TreeMap<>();

    private final Set<ModulePane> selectedModules = new HashSet<>();
    private final Rectangle selectedRect;
    private final List<Rectangle> dragGhosts = new ArrayList<>();
    private final Label areaLabel;
    private Point2D dragOrigin;
    private Runnable selectionListener;
    private List<Cables.Cable> cables = new ArrayList<>();

    public AreaId getAreaId() {
        return areaId;
    }


    enum SelectionStatus {
        IN_MODULE,
        IN_PANEL,
        DRAGGING
    }


    public AreaPane(AreaId areaId, Bridges bridges, SlotPane slotPane,
                    FXUtil.TextFieldFocusListener textFocusListener, Undos undos) {
        this.areaId = areaId;
        this.bridges = bridges;
        this.slotPane = slotPane;
        areaLabel = withClass(new Label(areaId == AreaId.Voice ? "VA" : "FX"),"area-label");
        areaPane = withClass(new Pane(areaLabel),"area-pane","gfont");
        this.textFocusListener = textFocusListener;
        this.undos = undos;
        scrollPane = withClass(new ScrollPane(areaPane),"area-scroll");
        scrollPane.setMinHeight(0);
        selectedRect = withClass(new Rectangle(),"selected-rect");
        selectedRect.setVisible(false);
        setupSelectionDragging();
        areaPane.getChildren().add(selectedRect);
    }

    private void setupSelectionDragging() {
        areaPane.setOnMousePressed(e -> {
            for (ModulePane mp : modulePanes.values()) {
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
                for (ModulePane mp : modulePanes.values()) {
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
        PatchArea area = d.getPerf().getSlot(slotPane.getSlot()).getArea(areaId);
        for (PatchModule m : area.getModules()) {
            UserModuleData md = m.getUserModuleData();
            ModulePane.ModuleSpec spec = new ModulePane.ModuleSpec(md.getIndex(), md.getType(),
                    md.uprate().get(),
                    md.leds().get(), md.getModes().stream().map(LibProperty::get).toList());
            l.add(() -> renderModule(spec, m, d, uiModules.get(md.getType())));
        }
        l.add(() -> renderCables(new ArrayList<>(area.getCables()))); //assuming fvs are one-off, should be thread-safe
    }

    private void renderCables(List<PatchCable> patchCables) {
        // fx thread with fresh list of "immutable" PatchCable instances
        for (PatchCable patchCable : patchCables) {
            var src = resolveModule(patchCable.getSrcModule());
            var dest = resolveModule(patchCable.getDestModule());
            Connector.PortType fromConnType = patchCable.getDirection() ? Out : In;
            var srcConn = src.resolveConn(fromConnType == In ? In : Out, patchCable.getSrcConn());
            var destConn = dest.resolveConn(In, patchCable.getDestConn());
            Cables.Cable cable = Cables.mkCable(src, srcConn, dest, destConn);
            cables.add(cable);
            areaPane.getChildren().addAll(cable.srcJack(), cable.endJack(), cable.run().getShadow(), cable.run().getCable());
        }
    }

    public void redrawCables() {
        for (Cables.Cable cable : cables) {
            areaPane.getChildren().removeAll(cable.run().getCable(),cable.run().getShadow());
            Cables.redrawRun(cable);
            areaPane.getChildren().addAll(cable.run().getShadow(), cable.run().getCable());
        }
    }

    private ModulePane resolveModule(int mIdx) {
        ModulePane mp = modulePanes.get(mIdx);
        if (mp == null) { throw new IllegalStateException("patchCable invalid module index: " + mIdx); }
        return mp;
    }


    private void renderModule(ModulePane.ModuleSpec m, PatchModule pm, Device d, UIModule<UIElement> ui) {
        // on fx thread
        ModulePane modulePane = new ModulePane(ui,m, textFocusListener, bridges, pm, slotPane, this);
        modulePanes.put(m.index(),modulePane);
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
                undos.beginMulti();
                moveSelectedModules(new UserModuleData.Coords(
                        (int) Math.round((e.getSceneX() - dragOrigin.getX()) / GRID_X),
                        (int) Math.round((e.getSceneY() - dragOrigin.getY()) / GRID_Y)));
                undos.commitMulti();
            }
            areaPane.getChildren().removeAll(dragGhosts);
            dragGhosts.clear();
            dragOrigin = null;
        });
    }

    public void moveSelectedModules(UserModuleData.Coords delta) {
        if (selectedModules.isEmpty()) { return; }
        for (ModulePane pane : selectedModules) {
            UserModuleData.Coords oldCoords = pane.coords().getValue();
            pane.coords().setValue(new UserModuleData.Coords(
                    oldCoords.column() + delta.column(),
                    oldCoords.row() + delta.row()));
        }
        resolveCollisions();
    }

    private void resolveCollisions() {

        Map<Integer, List<ModulePane>> modulesByColumn = new TreeMap<>();
        // Group modules by column
        for (ModulePane pane : modulePanes.values()) {
            modulesByColumn.computeIfAbsent(pane.coords().getValue().column(),
                    k -> new ArrayList<>()).add(pane);
        }

        // Comparator to sort panes by their row value ascending
        Comparator<ModulePane> rowComparator = Comparator.comparingInt(p -> p.coords().getValue().row());

        for (List<ModulePane> columnModules : modulesByColumn.values()) {

            // Sort column modules by row ascending for consistent order
            columnModules.sort(rowComparator);

            // Separate selected and non-selected modules into TreeSets sorted by row
            Set<ModulePane> selectedInCol = new TreeSet<>(rowComparator);
            Set<ModulePane> nonSelectedInCol = new TreeSet<>(rowComparator);
            Set<Integer> selectedRows = new HashSet<>();

            for (ModulePane pane : columnModules) {
                if (pane.isSelected()) {
                    selectedInCol.add(pane);
                    // Add all rows occupied by the selected module (multi-row height)
                    int startRow = pane.coords().getValue().row();
                    int height = pane.getHeight();
                    for (int r = startRow; r < startRow + height; r++) {
                        selectedRows.add(r);
                    }
                } else {
                    nonSelectedInCol.add(pane);
                }
            }

            // For each moved selected module in this column (top-down),
            // move non-selected modules down to resolve collisions
            for (ModulePane selPane : selectedInCol) {
                int selStartRow = selPane.coords().getValue().row();
                int selHeight = selPane.getHeight();
                int currentRow = selStartRow + selHeight; // start just below the bottom of selected module

                for (ModulePane nonSelPane : nonSelectedInCol) {
                    int nonSelStartRow = nonSelPane.coords().getValue().row();
                    int nonSelHeight = nonSelPane.getHeight();

                    // Detect vertical overlap between selected module and non-selected module
                    boolean overlapsVertically =
                            !(nonSelStartRow + nonSelHeight <= selStartRow || nonSelStartRow >= selStartRow + selHeight);

                    // Only act on non-selected modules that overlap vertically with the selected module's original position
                    if (overlapsVertically) {
                        // Move non-selected module down until no collision with selected modules or other non-selected modules
                        while (isOccupiedRange(selectedRows, currentRow, nonSelHeight)
                                || isOccupiedRange(nonSelectedInCol, currentRow, nonSelHeight)) {
                            currentRow++;
                        }
                        nonSelPane.coords().setValue(new UserModuleData.Coords(
                                nonSelPane.coords().getValue().column(), currentRow));
                        currentRow += nonSelHeight;
                    }
                }
            }
        }
    }

    /**
     * Checks if any rows in the range [startRow, startRow + height) are occupied in the given set of row ints.
     */
    private boolean isOccupiedRange(Set<Integer> occupiedRows, int startRow, int height) {
        for (int r = startRow; r < startRow + height; r++) {
            if (occupiedRows.contains(r)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if any ModulePane rows in the vertical range [startRow, startRow + height) are occupied by any pane in the collection.
     */
    private boolean isOccupiedRange(Collection<ModulePane> modules, int startRow, int height) {
        for (ModulePane pane : modules) {
            int paneStartRow = pane.coords().getValue().row();
            int paneHeight = pane.getHeight();
            // Check vertical overlap between the pane's occupied rows and [startRow, startRow + height)
            if (!(paneStartRow + paneHeight <= startRow || paneStartRow >= startRow + height)) {
                return true;
            }
        }
        return false;
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
        areaPane.getChildren().clear();
        areaPane.getChildren().add(areaLabel);
        for (ModulePane m : modulePanes.values()) {
            bridges.remove(m.getModuleBridges());
            m.unbindVarControls();
        }
        modulePanes.clear();
        cables.clear();
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
