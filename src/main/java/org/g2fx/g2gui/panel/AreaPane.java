package org.g2fx.g2gui.panel;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import org.g2fx.g2gui.FXUtil;
import org.g2fx.g2gui.G2GuiApplication;
import org.g2fx.g2gui.Undos;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.bridge.FxProperty;
import org.g2fx.g2gui.bridge.Iso;
import org.g2fx.g2gui.controls.Cables;
import org.g2fx.g2gui.controls.Connectors;
import org.g2fx.g2gui.module.ModuleDelta;
import org.g2fx.g2gui.module.MoveableModule;
import org.g2fx.g2gui.ui.UIElement;
import org.g2fx.g2gui.ui.UIModule;
import org.g2fx.g2lib.device.LibExecutor;
import org.g2fx.g2lib.model.Connector;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.state.*;
import org.g2fx.g2lib.util.Util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final Bridges<Patch> bridges;
    private final LibExecutor<PatchArea> areaExecutor;
    private final Undos undos;
    private final UIModule.UIModules uiModules;
    private final FXUtil.TextFieldFocusListener textFocusListener;
    private final SlotPane slotPane;

    private final ScrollPane scrollPane;
    private final Pane areaPane;
    private final Label areaLabel;
    private boolean resizing = false;

    private final Map<Integer,ModulePane> modulePanes = new TreeMap<>();

    private final Cables cables;

    private final Connectors conns = new Connectors(this);

    private final Property<ModuleDelta> moduleDelta = new SimpleObjectProperty<>(null,"moduleDelta");

    private int moduleColor = 0;

    private class ModuleSelection {

        private final Rectangle selectedRect;
        enum SelectionStatus { IN_MODULE, IN_PANEL, DRAGGING }

        private final Set<ModulePane> selectedModules = new HashSet<>();
        private Runnable selectionListener;
        private final List<Rectangle> dragGhosts = new ArrayList<>();
        private Point2D dragOrigin;

        public ModuleSelection(Rectangle selectedRect) {
            this.selectedRect = selectedRect;
        }

        public void onMousePressed(MouseEvent e) {
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
        }

        public void onMouseDragged(MouseEvent e) {
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
        }

        public void onMouseReleased(MouseEvent e) {
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
                        drawDragGhosts(selectedModules);
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
                    undos.withMulti(() -> moveSelectedModules(new Coords(
                            (int) Math.round((e.getSceneX() - dragOrigin.getX()) / GRID_X),
                            (int) Math.round((e.getSceneY() - dragOrigin.getY()) / GRID_Y))));
                }
                clearDragGhosts(dragGhosts);
                dragOrigin = null;
            });
        }

        private void drawDragGhosts(Collection<ModulePane> ms) {
            for (ModulePane m : ms) {
                Pane p = m.getPane();
                Rectangle r = withClass(new Rectangle(p.getLayoutX(),p.getLayoutY(),p.getWidth(),p.getHeight()), CSS_SELECTED_RECT);
                r.setUserData(new Point2D(r.getLayoutX(),r.getLayoutY()));
                areaPane.getChildren().add(r);
                dragGhosts.add(r);
            }
        }

        private void selectModule(ModulePane modulePane) {
            selectedModules.add(modulePane);
            modulePane.setSelected(true);
            selectionListener.run();
        }

        private void clearModuleSelection() {
            selectedModules.forEach(p -> p.setSelected(false));
            selectedModules.clear();
        }

        private void moveSelectedModules(Coords delta) {
            if (selectedModules.isEmpty()) { return; }
            for (ModulePane pane : selectedModules) {
                Coords oldCoords = pane.coords().getValue();
                pane.coords().setValue(new Coords(
                        oldCoords.column() + delta.column(),
                        oldCoords.row() + delta.row()));
            }
            resolveModuleCollisions();
        }

        public void setupSelectionListener(AreaPane otherPane) {
            selectionListener = otherPane.moduleSelection::clearModuleSelection;
        }

        public void selectModules(ModuleDelta md) {
            clearModuleSelection();
            md.modules().forEach(r -> selectModule(modulePanes.get(r.getIndex())));
        }
    }
    private final ModuleSelection moduleSelection;


    private class ToolbarDrag {

        private final ModuleType toolbarDragModuleType;
        private Rectangle toolbarDragRect = null;

        private ToolbarDrag(ModuleType toolbarDragModuleType) {
            this.toolbarDragModuleType = toolbarDragModuleType;
        }

        public void onDragEntered(DragEvent e) {

            if (!G2GuiApplication.G2_TOOLBAR_DRAG.equals(e.getDragboard().getString())) return;

            if (toolbarDragRect == null) {
                toolbarDragRect = withClass1(CSS_SELECTED_RECT, new Rectangle(
                        GRID_X, toolbarDragModuleType.height * GRID_Y));
                areaPane.getChildren().add(toolbarDragRect);
            }
            e.consume();

        }

        public void onDragOver(DragEvent e) {
            if (toolbarDragRect == null) return;
            e.acceptTransferModes(TransferMode.COPY);

            toolbarDragRect.setX(e.getX() - toolbarDragRect.getWidth() / 2);
            toolbarDragRect.setY(e.getY() - toolbarDragRect.getHeight() / 2);

            e.consume();
        }

        public void onDragDropped(DragEvent e) {

            if (toolbarDragRect == null) return;

            // Calculate grid position and add new module
            int col = (int) Math.round((e.getX() - (toolbarDragRect.getWidth()/2)) / GRID_X);
            int row = (int) Math.round((e.getY() - (toolbarDragRect.getHeight()/2)) / GRID_Y);
            int index = getNewModuleIndex();
            undos.withMulti(() -> {
                ModuleDelta ma = ModuleDelta.addNewModule(areaId,toolbarDragModuleType, index,
                        getNewModuleName(toolbarDragModuleType), moduleColor,
                        new Coords(col, row));
                fireModuleDelta(ma);
                moduleSelection.clearModuleSelection();
                selectModule(index);
                resolveModuleCollisions();
            });
            clearToolbarDrag();
            e.consume();

        }

        public void clear() {
            if (toolbarDragRect != null) {
                areaPane.getChildren().remove(toolbarDragRect);
                toolbarDragRect = null;
            }
        }
    }
    private ToolbarDrag toolbarDrag;

    public class ModulePaste {
        private final AreaPane otherPane;
        private final ModuleDelta delta;
        private Point2D pasteOrigin;
        private record PasteGhost(Rectangle rect, Point2D origin, Point2D local, ModuleDelta.UserModuleRecord module) {}
        private final List<PasteGhost> pasteGhosts = new ArrayList<>();


        public ModulePaste(ModuleDelta md, AreaPane otherPane) {
            this.delta = md;
            this.otherPane = otherPane;
        }

        public void mouseEntered(MouseEvent e) {
            init(e);
        }

        public void mouseExited() {
            pasteOrigin = null;
            clearDragGhosts();
        }

        public void onMouseReleased() {
            if (pasteOrigin == null) { return; }
            Set<Integer> ixs = new TreeSet<>(modulePanes.keySet());
            int minCol = Integer.MAX_VALUE;
            int minRow = Integer.MAX_VALUE;
            pasteGhosts.sort(Comparator.comparing(g -> new Coords((int) g.origin.getX(), (int) g.origin.getY())));
            LinkedHashMap<Integer,ModuleDelta.UserModuleRecord> updated = new LinkedHashMap<>();
            for (PasteGhost g : pasteGhosts) {
                Coords cs = new Coords((int) Math.round(g.rect.getX() / GRID_X),
                        (int) Math.round(g.rect.getY() / GRID_Y));
                minCol = Math.min(cs.column(),minCol);
                minRow = Math.min(cs.row(),minRow);
                int idx = getNewModuleIndex(ixs);
                ixs.add(idx);
                updated.put(g.module.getIndex(),g.module.duplicate(idx,cs));
            }
            ModuleDelta newDelta = delta.update(updated);
            undos.withMulti(() -> {
                fireModuleDelta(newDelta);
                moduleSelection.selectModules(newDelta);
                resolveModuleCollisions();
            });
            clearDragGhosts();
            finishPaste();
            otherPane.cancelPaste();
        }

        private void init(MouseEvent e) {
            init(new Point2D(e.getX(), e.getY()));
        }

        public void init(Point2D pt) {
            if (pasteOrigin != null) {
                return;
            }
            pasteOrigin = pt;
            //compute union rectangle
            double x1 = Double.POSITIVE_INFINITY;
            double y1 = Double.POSITIVE_INFINITY;
            double x2 = Double.NEGATIVE_INFINITY;
            double y2 = Double.NEGATIVE_INFINITY;
            List<Rectangle> ghosts = new ArrayList<>();
            for (ModuleDelta.UserModuleRecord mr : delta.modules()) {
                Rectangle r = withClass(new Rectangle(
                        mr.getCoords().column() * GRID_X,
                        mr.getCoords().row() * GRID_Y,
                        GRID_X,
                        mr.getType().height * GRID_Y),
                        CSS_SELECTED_RECT);
                r.setUserData(mr);
                x1 = Math.min(r.getX(),x1);
                y1 = Math.min(r.getY(),y1);
                x2 = Math.max(r.getX()+r.getWidth(),x2);
                y2 = Math.max(r.getY()+r.getHeight(),y2);
                ghosts.add(r);
            }
            //center on mouse position
            double orgX=pasteOrigin.getX()-((x2-x1)/2);
            double orgY=pasteOrigin.getY()-((y2-y1)/2);
            for (Rectangle r : ghosts) {
                double localX = r.getX() - x1;
                r.setX(localX+orgX);
                double localY = r.getY() - y1;
                r.setY(localY+orgY);
                pasteGhosts.add(new PasteGhost(r,
                        new Point2D(r.getX(), r.getY()), new Point2D(localX,localY),
                        (ModuleDelta.UserModuleRecord) r.getUserData()));
                areaPane.getChildren().add(r);
            }
        }

        public void onMouseMoved(MouseEvent e) {
            init(e);
            for (PasteGhost g : pasteGhosts) {
                g.rect.setX(g.origin.getX()+ (e.getX() - pasteOrigin.getX()));
                g.rect.setY(g.origin.getY()+ (e.getY() - pasteOrigin.getY()));
            }
        }

        public void cancel() {
            clearDragGhosts();
        }

        private void clearDragGhosts() {
            pasteGhosts.forEach(pg -> areaPane.getChildren().remove(pg.rect));
            pasteGhosts.clear();
        }
    }
    private ModulePaste modulePaste;

    public AreaPane(AreaId areaId, Bridges<Patch> bridges, SlotPane slotPane,
                    FXUtil.TextFieldFocusListener textFocusListener, Undos undos,
                    UIModule.UIModules uiModules) {
        this.areaId = areaId;
        this.bridges = bridges;
        areaExecutor = LibExecutor.adapt(bridges.getLibExecutor(), p->p.getArea(areaId));
        this.slotPane = slotPane;
        this.textFocusListener = textFocusListener;
        this.undos = undos;
        this.uiModules = uiModules;
        this.log = Util.getLogger(getClass(),slotPane.getSlot(),areaId);

        areaLabel = withClass(new Label(areaId == AreaId.Voice ? "VA" : "FX"),"area-label");
        areaPane = withClass(new Pane(areaLabel),"area-pane","gfont");
        scrollPane = withClass(new ScrollPane(areaPane),"area-scroll");
        scrollPane.setMinHeight(0);

        this.cables = new Cables(slotPane,areaPane);

        Rectangle selectedRect = withClass(new Rectangle(),"selected-rect");
        selectedRect.setVisible(false);
        moduleSelection = new ModuleSelection(selectedRect);
        areaPane.getChildren().add(selectedRect);

        setupMouseHandling();
        setupModuleDrag();
        moduleDelta.addListener((_,_,n) -> handleModuleDelta(n));
        // fx thread
        bridges.bridge(d -> d.getArea(this.areaId).getDummyModuleAddProp(),
                new FxProperty.SimpleFxProperty<>(moduleDelta, u ->
                        new Undos.Undo<>(u.property(),u.newValue().invert(),u.newValue())),
                Iso.id());
        scrollPane.boundsInLocalProperty().addListener((_,_,_) -> resizeAreaPane());
        areaPane.getChildren().addListener((ListChangeListener<? super Node>) _ -> resizeAreaPane());
    }


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

    private void setupMouseHandling() {
        areaPane.setOnMouseEntered(e -> {
            if (modulePaste!=null) { modulePaste.mouseEntered(e); }
        });
        areaPane.setOnMouseExited(_ -> {
            if (modulePaste!=null) { modulePaste.mouseExited(); }
        });
        areaPane.setOnMousePressed(moduleSelection::onMousePressed);
        areaPane.setOnMouseDragged(moduleSelection::onMouseDragged);
        areaPane.setOnMouseReleased(e -> {
            if (modulePaste != null) {
                modulePaste.onMouseReleased();
                return;
            }
            moduleSelection.onMouseReleased(e);
        });
        areaPane.setOnMouseMoved(e -> {
            if (modulePaste != null) { modulePaste.onMouseMoved(e); }
        });
    }


    private void setupModuleDrag() {
        areaPane.setOnDragEntered(e -> {
            if (toolbarDrag != null) { toolbarDrag.onDragEntered(e); }
        });
        areaPane.setOnDragOver(e -> {
            if (toolbarDrag != null) { toolbarDrag.onDragOver(e); }
        });
        areaPane.setOnDragExited(_ -> clearToolbarDrag());
        areaPane.setOnDragDropped(e -> {
            if (toolbarDrag != null) { toolbarDrag.onDragDropped(e); }
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
        return getNewModuleIndex(modulePanes.keySet());
    }

    private static int getNewModuleIndex(Collection<Integer> ixs) {
        int ix = 1;
        for (Integer i : ixs) {
            if (ix < i) {
                return ix;
            }
            ix = i+1;
        }
        return ix;
    }


    public void initModules(Patch d, List<Runnable> l) {
        // on device thread
        PatchArea area = d.getArea(areaId);
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
        cables.addCable(srcConn,destConn);
    }

    public void mkConnCtxMenu(Connectors.Conn conn, ContextMenuEvent cme) {
        cables.mkConnCtxMenu(conn,cme);
    }



    public void manageCables(boolean redraw) {
        cables.manageCables(redraw);
    }


    private ModulePane resolveModule(int mIdx) {
        ModulePane mp = modulePanes.get(mIdx);
        if (mp == null) { throw new IllegalStateException("patchCable invalid module index: " + mIdx); }
        return mp;
    }


    private void renderModule(int index, ModuleType type, PatchModule pm, UIModule<UIElement> ui) {
        // on fx thread
        ModulePane modulePane = new ModulePane(ui,index,type,
                textFocusListener, slotPane,
                this.getConns(), this.getAreaId(),
                bridges.spawn(p->p.getArea(areaId).getModule(index)),
                this::mkConnCtxMenu);
        modulePanes.put(index,modulePane);
        areaPane.getChildren().add(modulePane.getPane());
        moduleSelection.setupModuleMouseHandling(modulePane);
        bridges.getLibExecutor().invoke(() -> modulePane.getBridges().initialize(pm))
                .forEach(Runnable::run);
        modulePane.coords().addListener((_,_,_) -> cables.updateCables(modulePane));
    }




    private void clearDragGhosts(List<Rectangle> ghosts) {
        areaPane.getChildren().removeAll(ghosts);
        ghosts.clear();
    }




    public Set<ModulePane> getSelectedModules() {
        return moduleSelection.selectedModules;
    }

    public boolean hasModuleSelection() {
        return !moduleSelection.selectedModules.isEmpty();
    }

    public void clearModules() {
        areaPane.getChildren().clear();
        areaPane.getChildren().add(areaLabel);
        areaPane.getChildren().add(moduleSelection.selectedRect);
        for (ModulePane m : modulePanes.values()) {
            m.unbindVarControls();
        }
        modulePanes.clear();
        cables.clear();
        conns.clear();
    }

    public void disposeModuleBridges() {
        modulePanes.values().forEach(m -> m.getBridges().dispose());
    }





    public Pane getAreaPane() {
        return areaPane;
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }


    public void setupSelectionListener(AreaPane otherPane) {
        moduleSelection.setupSelectionListener(otherPane);
    }

    public void selectModule(int idx) {
        ModulePane mp = modulePanes.get(idx);
        if (mp == null) { throw new IllegalArgumentException("selectModule: invalid index: " + idx); }
        moduleSelection.selectModule(mp);
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
        toolbarDrag = new ToolbarDrag(mt);
    }

    private void clearToolbarDrag() {
        if (toolbarDrag != null) {
            toolbarDrag.clear();
            toolbarDrag = null;
        }
    }

    /**
     * Double-click add from toolbar. If selection, adds below lower-right selected;
     * if no selection add top-right.
     */
    public void addNewModule(ModuleType mt) {
        Coords coords;
        if (moduleSelection.selectedModules.isEmpty()) {
            coords = new Coords(0,0);
        } else {
            ModulePane m = moduleSelection.selectedModules.stream().sorted(
                    Comparator.comparing(o -> o.coords().getValue())).toList().getLast();
            coords = new Coords(m.coords().getValue().column(),m.coords().getValue().row()+m.getHeight());
        }
        moduleSelection.clearModuleSelection();
        int ix = getNewModuleIndex();
        undos.withMulti(() -> {
            fireModuleDelta(ModuleDelta.addNewModule(areaId,mt, ix,getNewModuleName(mt),moduleColor,coords));
            selectModule(ix);
            resolveModuleCollisions();
        });
    }

    private void resolveModuleCollisions() {
        MoveableModule.resolveCollisions(modulePanes.values());
    }


    private void handleModuleDelta(ModuleDelta md) {
        if (md.isEmpty()) { return; }
        if (md.add()) {
            doAddModule(md);
        } else {
            doDeleteModule(md);
        }
    }

    /**
     * Internal, fire add-module property event.
     */
    private void fireModuleDelta(ModuleDelta ma) {
        moduleDelta.setValue(ma);
    }


    /**
     * Module-add property event handler.
     */
    private void doAddModule(ModuleDelta ma) {
        PatchArea.CreateResult cr = bridges.getLibExecutor().invokeWithCurrent(d -> {
            PatchArea.CreateResult ms = d.getArea(areaId).createModules(ma);
            d.getVisuals().updateVisualIndex();
            return ms;
        });
        for (PatchModule pm : cr.modules()) {
            ModuleType type = pm.getUserModuleData().getType();
            renderModule(pm.getIndex(), type,pm,uiModules.get(type));
        }
        renderCables(cr.cables());
    }

    private void doDeleteModule(ModuleDelta md) {
        for (ModuleDelta.UserModuleRecord mr : md.modules()) {
            ModulePane mp = modulePanes.remove(mr.getIndex());
            bridges.getLibExecutor().execute(() -> mp.getBridges().dispose());
            areaPane.getChildren().remove(mp.getPane());
        }
        cables.doDeleteModule(md);
        areaExecutor.runWithCurrent(p -> p.deleteModules(md));
    }



    public void initPaste(ModuleDelta md, AreaPane otherPane) {
        modulePaste = new ModulePaste(md,otherPane);
    }
    public void cancelPaste() {
        if (modulePaste != null) {
            modulePaste.cancel();
        }
        modulePaste = null;
    }


    private void finishPaste() {
        modulePaste = null;
    }

    /**
     * exposed for testing
     */
    public ModulePaste getModulePaste() {
        return modulePaste;
    }

    /**
     * exposed for testing
     */
    public int getCableCount() {
        return cables.size();
    }

    private List<Integer> getSelectedModuleIdxs() {
        return getSelectedModules().stream().map(ModulePane::getIndex).toList();
    }

    public ModuleDelta doCopy() {
        List<Integer> modules = getSelectedModuleIdxs();
        return areaExecutor.invokeWithCurrent(p -> p.mkCopyModuleDelta(modules));
    }

    public ModuleDelta doCut() {
        List<Integer> modules = getSelectedModuleIdxs();
        ModuleDelta md = areaExecutor.invokeWithCurrent(p -> p.mkCopyModuleDelta(modules));
        ModuleDelta cut = areaExecutor.invokeWithCurrent(p -> p.mkDeleteModuleDelta(modules));
        moduleDelta.setValue(cut);
        return md;
    }

    public void doDelete() {
        List<Integer> modules = getSelectedModuleIdxs();
        moduleDelta.setValue(areaExecutor.invokeWithCurrent(p -> p.mkDeleteModuleDelta(modules)));
    }
}
