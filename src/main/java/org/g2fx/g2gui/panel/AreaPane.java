package org.g2fx.g2gui.panel;

import com.google.common.collect.Sets;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2gui.G2GuiApplication;
import org.g2fx.g2gui.Undos;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.controls.Cables;
import org.g2fx.g2gui.controls.Connectors;
import org.g2fx.g2gui.ui.UIElement;
import org.g2fx.g2gui.ui.UIModule;
import org.g2fx.g2lib.model.Connector;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.state.*;
import org.g2fx.g2lib.util.Util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.FXUtil.withClass1;
import static org.g2fx.g2gui.panel.ModulePane.GRID_X;
import static org.g2fx.g2gui.panel.ModulePane.GRID_Y;
import static org.g2fx.g2lib.model.Connector.PortType.In;
import static org.g2fx.g2lib.model.Connector.PortType.Out;

public class AreaPane {

    public static final String CSS_SELECTED_RECT = "selected-rect";

    private final Logger log;
    private final AreaId areaId;
    private final Bridges bridges;
    private final Undos undos;
    private final UIModule.UIModules uiModules;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final SlotPane slotPane;

    private final ScrollPane scrollPane;
    private final Pane areaPane;
    private final Label areaLabel;

    private final Map<Integer,ModulePane> modulePanes = new TreeMap<>();

    private final Set<ModulePane> selectedModules = new HashSet<>();
    private final Rectangle selectedRect;
    private Runnable selectionListener;
    private final List<Rectangle> dragGhosts = new ArrayList<>();
    private Point2D dragOrigin;

    private List<Cables.Cable> cables = new ArrayList<>();
    private final Connectors conns = new Connectors(this);

    private ModuleType toolbarDragModuleType = null;
    private Rectangle toolbarDragRect = null;

    public record ModuleAdd(ModuleType type, int index, String name, int color, Coords coords) {}
    private Property<Set<ModuleAdd>> moduleAdds = new SimpleObjectProperty<>(Set.of());
    private int moduleColor = 0;

    public AreaId getAreaId() {
        return areaId;
    }

    public Connectors getConns() {
        return conns;
    }

    public void newCable(Connectors.Conn start, Connectors.Conn end) {
        addCable(start,end);
        manageCables(false);
        //TODO add to backend as redoable action
    }

    enum SelectionStatus {
        IN_MODULE,
        IN_PANEL,
        DRAGGING
    }

    private boolean resizing = false;


    public AreaPane(AreaId areaId, Bridges bridges, SlotPane slotPane,
                    FXUtil.TextFieldFocusListener textFocusListener, Undos undos,
                    UIModule.UIModules uiModules) {
        this.areaId = areaId;
        this.bridges = bridges;
        this.slotPane = slotPane;
        this.textFocusListener = textFocusListener;
        this.undos = undos;
        this.uiModules = uiModules;
        this.log = Util.getLogger(getClass(),slotPane.getSlot(),areaId);

        areaLabel = withClass(new Label(areaId == AreaId.Voice ? "VA" : "FX"),"area-label");
        areaPane = withClass(new Pane(areaLabel),"area-pane","gfont");
        scrollPane = withClass(new ScrollPane(areaPane),"area-scroll");
        scrollPane.setMinHeight(0);

        selectedRect = withClass(new Rectangle(),"selected-rect");
        selectedRect.setVisible(false);
        setupSelectionDragging();
        setupModuleDrag();
        moduleAdds.addListener((c,o,n) -> {
            undoAddModule(Sets.difference(o,n));
            doAddModule(Sets.difference(n,o));
        });
        bridges.bridge(moduleAdds,d -> getPatchArea(d).getDummyModuleAddProp());
        areaPane.getChildren().add(selectedRect);
        scrollPane.boundsInLocalProperty().addListener((c,o,n) -> resizeAreaPane());
        areaPane.getChildren().addListener((ListChangeListener<? super Node>) c -> resizeAreaPane());
    }

    private void resizeAreaPane() {
        if (resizing) return;
        resizing = true;
        Bounds scrollBounds = scrollPane.getBoundsInLocal();
        try {
            double x = scrollBounds.getMaxX(), y = scrollBounds.getMaxY();
            for (Node n : areaPane.getChildren()) {
                Bounds b = n.getBoundsInParent();
                x = Math.max(x, b.getMaxX()+5);
                y = Math.max(y, b.getMaxY()+5);
            }
            areaPane.setPrefSize(x, y);
        } finally {
            resizing = false;
        }
    }

    private PatchArea getPatchArea(Device d) {
        return d.getPerf().getSlot(slotPane.getSlot()).getArea(areaId);
    }

    private void setupSelectionDragging() {
        areaPane.setOnMousePressed(e -> {
            for (ModulePane mp : modulePanes.values()) {
                if (mp.getPane().getBoundsInParent().contains(e.getX(), e.getY())) {
                    selectedRect.setUserData(SelectionStatus.IN_MODULE);
                    return;
                }
            }
            selectedRect.setX(e.getX());
            selectedRect.setY(e.getY());
            selectedRect.getProperties().put("startPoint",new Point2D(e.getX(),e.getY()));
            selectedRect.setWidth(0);
            selectedRect.setHeight(0);
            selectedRect.setUserData(SelectionStatus.IN_PANEL);
            e.consume();
        });
        areaPane.setOnMouseDragged(e -> {
            if (selectedRect.getUserData() == SelectionStatus.IN_PANEL) {
                selectedRect.setVisible(true);
                selectedRect.setUserData(SelectionStatus.DRAGGING);
            }
            if (selectedRect.isVisible()) {
                Point2D start = (Point2D) selectedRect.getProperties().get("startPoint");
                selectedRect.setX(Math.min(start.getX(), e.getX()));
                selectedRect.setY(Math.min(start.getY(), e.getY()));
                selectedRect.setWidth(Math.abs(e.getX() - start.getX()));
                selectedRect.setHeight(Math.abs(e.getY() - start.getY()));
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
            e.consume();
        });

    }

    private void setupModuleDrag() {

        areaPane.setOnDragEntered(e -> {
            if (toolbarDragModuleType == null) return;
            if (!G2GuiApplication.G2_TOOLBAR_DRAG.equals(e.getDragboard().getString())) return;

            if (toolbarDragRect == null) {
                toolbarDragRect = withClass1(CSS_SELECTED_RECT, new Rectangle(
                        GRID_X, toolbarDragModuleType.height * GRID_Y));
                areaPane.getChildren().add(toolbarDragRect);
//                pendingToolbarDragRect.setVisible(true);
            }
            e.consume();
        });

        areaPane.setOnDragOver(e -> {
            if (toolbarDragRect == null) return;
            e.acceptTransferModes(TransferMode.COPY);

            toolbarDragRect.setX(e.getX() - toolbarDragRect.getWidth() / 2);
            toolbarDragRect.setY(e.getY() - toolbarDragRect.getHeight() / 2);

            e.consume();
        });

        areaPane.setOnDragExited(e -> clearToolbarDrag());

        areaPane.setOnDragDropped(e -> {
            if (toolbarDragRect == null) return;

            // Calculate grid position and add new module
            int col = (int) Math.round(e.getX() / GRID_X);
            int row = (int) Math.round(e.getY() / GRID_Y);
            int index = getNewModuleIndex();
            undos.withMulti(() -> {
                ModuleAdd ma = new ModuleAdd(toolbarDragModuleType, index,
                        getNewModuleName(toolbarDragModuleType), moduleColor,
                        new Coords(col, row));
                addNewModule(ma);
                clearModuleSelection();
                selectModule(ma.index);
                resolveCollisions(modulePanes.values());
            });
            clearToolbarDrag();
            e.consume();
        });
    }


    private String getNewModuleName(ModuleType mt) {
        AtomicInteger i = new AtomicInteger(0);
        modulePanes.values().forEach(mp -> {
            if (mp.getType() == mt) {
                Matcher m = Pattern.compile("^" + mt.shortName + "(\\d+)$").matcher(mp.getName());
                if (m.matches()) {
                    i.set(Math.max(i.get(), Integer.parseInt(m.group(1))));
                }
            }
        });
        return mt.shortName + (i.get() + 1);
    }

    private int getNewModuleIndex() {
        int ix = 1;
        for (Integer i : modulePanes.keySet()) {
            if (ix < i) { return ix; }
            ix = i+1;
        }
        return ix;
    }


    public void initModules(Device d, List<Runnable> l) {
        // on device thread
        PatchArea area = getPatchArea(d);
        for (PatchModule m : area.getModules()) {
            UserModuleData md = m.getUserModuleData();
            l.add(() -> renderModule(md.getIndex(), md.getType(), m, uiModules.get(md.getType())));
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
            addCable(srcConn, destConn);
        }
        manageCables(false);
    }

    private void addCable(Connectors.Conn srcConn, Connectors.Conn destConn) {
        Cables.Cable cable = Cables.mkCable(srcConn, destConn);
        cables.add(cable);
        areaPane.getChildren().addAll(cable.srcJack(), cable.endJack());
    }

    public void manageCables(boolean redraw) {
        for (Cables.Cable cable : cables) {
            areaPane.getChildren().removeAll(cable.run().getCable(), cable.run().getShadow());
            if (redraw) Cables.redrawRun(cable);
            if (slotPane.isCableVisible(cable))
                areaPane.getChildren().addAll(cable.run().getShadow(), cable.run().getCable());
        }
    }


    private void updateCables(ModulePane mp) {
        for (Cables.Cable c : new ArrayList<>(cables)) {
            if (mp == c.srcConn().parent()|| mp == c.destConn().parent()) {
                cables.remove(c);
                areaPane.getChildren().removeAll(c.endJack(),c.srcJack(),c.run().getShadow(),c.run().getCable());
                Cables.Cable cnew = Cables.mkCable(c);
                cables.add(cnew);
                areaPane.getChildren().addAll(cnew.endJack(),cnew.srcJack());
            }
        }
        manageCables(false);
    }


    private ModulePane resolveModule(int mIdx) {
        ModulePane mp = modulePanes.get(mIdx);
        if (mp == null) { throw new IllegalStateException("patchCable invalid module index: " + mIdx); }
        return mp;
    }


    private void renderModule(int index, ModuleType type, PatchModule pm, UIModule<UIElement> ui) {
        // on fx thread
        ModulePane modulePane = new ModulePane(ui,index,type, textFocusListener, bridges, pm, slotPane, this);
        modulePanes.put(index,modulePane);
        areaPane.getChildren().add(modulePane.getPane());
        setupModuleMouseHandling(modulePane);
        bridges.getDeviceExecutor().invokeWithCurrent(dd ->
            modulePane.getModuleBridges().stream().map(b -> b.finalizeInit(dd)).toList()
        ).forEach(Runnable::run);
        modulePane.coords().addListener((c,o,l) -> updateCables(modulePane));
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
            clearToolbarDrag();
            e.consume();
        });
        pane.setOnMousePressed(e -> {
            if (modulePane.isSelected()) {
                dragGhosts.clear();
                dragOrigin = new Point2D(e.getSceneX(),e.getSceneY());
            }
            clearToolbarDrag();
        });
        pane.setOnMouseDragged(e -> {
            if (dragOrigin != null) {
                if (dragGhosts.isEmpty()) {
                    for (ModulePane m : selectedModules) {
                        Pane p = m.getPane();
                        Rectangle r = withClass(new Rectangle(p.getLayoutX(),p.getLayoutY(),p.getWidth(),p.getHeight()), CSS_SELECTED_RECT);
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
                undos.withMulti(() ->
                        moveSelectedModules(new Coords(
                            (int) Math.round((e.getSceneX() - dragOrigin.getX()) / GRID_X),
                            (int) Math.round((e.getSceneY() - dragOrigin.getY()) / GRID_Y))));
            }
            areaPane.getChildren().removeAll(dragGhosts);
            dragGhosts.clear();
            dragOrigin = null;
        });

    }

    public void moveSelectedModules(Coords delta) {
        if (selectedModules.isEmpty()) { return; }
        for (ModulePane pane : selectedModules) {
            Coords oldCoords = pane.coords().getValue();
            pane.coords().setValue(new Coords(
                    oldCoords.column() + delta.column(),
                    oldCoords.row() + delta.row()));
        }
        resolveCollisions(modulePanes.values());
    }

    /**
     * Resolve module bounds collisions, where selected modules represent "moved" modules/user intent,
     * and unselected those to be moved. Moves are handled independently by column.
     */

    public static void resolveCollisions(Collection<? extends MoveableModule> allModules) {
        // sort, group by column
        Map<Integer, List<MoveableModule>> byColumn = allModules.stream()
                .sorted(Comparator.comparing(MoveableModule::getColumn))
                .collect(Collectors.groupingBy(MoveableModule::getColumn,
                        TreeMap::new,Collectors.toList()));
        byColumn.values().forEach(AreaPane::resolveCollisionsColumn);
    }

    /**
     * Moves are handled with each selection top-to-bottom.
     * For each selected, you have:
     *  - selected module SEL
     *  - remaining selecteds below SELS
     *  - First colliding nonselected TOPCOLL
     *  - List with colliding and every nonselected below COLLS
     *  1. If TOPCOLL above SEL:
     *    - remove TOPCALL from COLLS
     *    - move SEL down OFFSET rows to be below TOPCOLL
     *    - move all SELS down by OFFSET.
     *  2. Initialize loop var TOP with SEL.
     *  3. For each COLL in COLLS:
     *    a. If COLL does not collide with TOP, break.
     *    b. Move COLL below TOP.
     *    c. Set TOP to COLL.
     *  4. Loop to next selected.
     */
    private static void resolveCollisionsColumn(List<? extends MoveableModule> modules) {
        //sort by row, group by selected
        Map<Boolean, List<MoveableModule>> bySelected = modules.stream()
                .sorted(Comparator.comparing(MoveableModule::getRow))
                .collect(Collectors.groupingBy(MoveableModule::isSelected));
        if (bySelected.size() != 2) { return; }
        List<MoveableModule> selecteds = new ArrayList<>(bySelected.get(true));
        List<MoveableModule> unselecteds = new ArrayList<>(bySelected.get(false));
        while (!selecteds.isEmpty()) {
            // get/remove top selected
            MoveableModule sel = selecteds.removeFirst();
            // remove unselecteds whose bottom edge is above or equal to sel row
            unselecteds.removeIf(m -> m.getBottomEdge() <= sel.getRow());
            if (unselecteds.isEmpty()) { return; }
            MoveableModule topUnsel = unselecteds.getFirst();
            if (topUnsel.getRow() < sel.getRow()) {
                // keep top, move all sels immediately below
                unselecteds.removeFirst();
                int selInc = topUnsel.getBottomEdge() - sel.getRow();
                sel.incRow(selInc);
                selecteds.forEach(s -> s.incRow(selInc));
            }
            //iteratively move down unselecteds as needed
            MoveableModule top = sel;
            for (MoveableModule m : unselecteds) {
                if (top.getBottomEdge() <= m.getRow()) { break; }
                m.setRow(top.getBottomEdge());
                top = m;
            }
        }
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
        areaPane.getChildren().add(selectedRect);
        for (ModulePane m : modulePanes.values()) {
            m.unbindVarControls();
        }
        modulePanes.clear();
        cables.clear();
    }

    public void disposeModuleBridges() {
        for (ModulePane m : modulePanes.values()) {
            bridges.remove(m.getModuleBridges());
        }
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

    public void selectModule(int idx) {
        ModulePane mp = modulePanes.get(idx);
        if (mp == null) { throw new IllegalArgumentException("selectModule: invalid index: " + idx); }
        selectModule(mp);
    }

    public ModulePane getModule(int module) {
        return modulePanes.get(module);
    }

    public void updateModuleColor(int index) {
        getSelectedModules().forEach(m -> m.color().setValue(index));
        this.moduleColor = index;
    }

    /**
     * Optimistic module add via drag from toolbar, happens
     * before mouse has entered pane.
     */
    public void startToolbarDrag(ModuleType mt) {
        toolbarDragModuleType = mt;
    }

    private void clearToolbarDrag() {
        toolbarDragModuleType = null;
        if (toolbarDragRect != null) {
            areaPane.getChildren().remove(toolbarDragRect);
            toolbarDragRect = null;
        }
    }

    /**
     * Double-click add from toolbar. If selection, adds below lower-right selected;
     * if no selection add top-right.
     */
    public void addNewModule(ModuleType mt) {
        Coords coords;
        if (selectedModules.isEmpty()) {
            coords = new Coords(0,0);
        } else {
            ModulePane m = selectedModules.stream().sorted(
                    Comparator.comparing(o -> o.coords().getValue())).toList().getLast();
            coords = new Coords(m.coords().getValue().column(),m.coords().getValue().row()+m.getHeight());
        }
        clearModuleSelection();
        int ix = getNewModuleIndex();
        undos.withMulti(() -> {
            addNewModule(new ModuleAdd(mt, ix,getNewModuleName(mt),moduleColor,coords));
            selectModule(ix);
            resolveCollisions(modulePanes.values());
        });
    }


    /**
     * Internal, fire add-module property event.
     */
    private void addNewModule(ModuleAdd ma) {
        HashSet<ModuleAdd> as = new HashSet<>(moduleAdds.getValue());
        as.add(ma);
        moduleAdds.setValue(as);
    }


    /**
     * Module-add property event handler.
     */
    private void doAddModule(Sets.SetView<ModuleAdd> adds) {
        adds.forEach(ma -> {
            log.info(() -> "doAddModule: " + ma);
            PatchModule pm = bridges.getDeviceExecutor().invokeWithCurrent(d -> {
                PatchModule m = getPatchArea(d).createModule(ma);
                d.getPerf().getSlot(slotPane.getSlot()).getVisuals().updateVisualIndex();
                return m;
            });
            renderModule(ma.index(),ma.type(),pm,uiModules.get(ma.type()));
        });
    }

    private void undoAddModule(Sets.SetView<ModuleAdd> adds) {
        adds.forEach(ma -> {
            log.info(() -> "undoAddModule: " + ma);
        });

    }

}
