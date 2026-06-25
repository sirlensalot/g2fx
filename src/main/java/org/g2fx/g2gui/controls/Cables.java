package org.g2fx.g2gui.controls;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import org.g2fx.g2gui.Undos;
import org.g2fx.g2gui.bridge.Bridges;
import org.g2fx.g2gui.bridge.FxProperty;
import org.g2fx.g2gui.bridge.Iso;
import org.g2fx.g2gui.module.ModuleDelta;
import org.g2fx.g2gui.panel.AreaPane;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.panel.SlotPane;
import org.g2fx.g2lib.model.CableDelta;
import org.g2fx.g2lib.model.Connector;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.state.PatchArea;
import org.g2fx.g2lib.state.PatchCable;
import org.g2fx.g2lib.util.Util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.g2fx.g2gui.Commands.mkMenu;
import static org.g2fx.g2gui.Commands.mkMenuItem;
import static org.g2fx.g2gui.controls.Connectors.RADIUS;
import static org.g2fx.g2lib.model.Connector.ConnDir.In;
import static org.g2fx.g2lib.model.Connector.ConnDir.Out;
import static org.g2fx.g2lib.util.Util.with;

public class Cables {

    public enum ColorSelection {
        Audio,
        Control,
        Logic_BG,
        Logic_FG,
        User_1,
        User_2;
        public String displayName() { return name().replace('_',' '); }
    }

    public static class CableRun {
        private Node cable;
        private Node shadow;

        public CableRun(Node cable, Node shadow) {
            this.cable = cable;
            this.shadow = shadow;
        }

        public Node getCable() {
            return cable;
        }

        public void setCable(Node cable) {
            this.cable = cable;
        }

        public Node getShadow() {
            return shadow;
        }

        public void reset(CableRun run) {
            this.shadow = run.getShadow();
            this.cable = run.getCable();
        }
    }
    public record Cable(
            CableColor color,
            Connectors.Conn srcConn,
            Point2D start,
            Connectors.Conn destConn,
            Point2D end,
            CableRun run,
            Node srcJack,
            Node endJack) {
        public Cable changeColor(CableColor newColor) {
            return new Cable(newColor,srcConn,start,destConn,end,run,srcJack,endJack);
        }

        public CableDelta.CableIndex toCableIndex() {
            return new CableDelta.CableIndex(
                    srcConn.modulePane().getIndex(),srcConn.connector(),
                    destConn.modulePane().getIndex(), destConn.connector(),
                    color.ordinal());
        }
    }

    private final Logger log = Util.getLogger(getClass());

    private record CableStore(List<Cable> cables,Map<Connectors.Conn, Set<Cable>> connToCable) {

        public CableStore() { this(new ArrayList<>(),new HashMap<>()); }

        public void add(Cable cable) {
            cables.add(cable);
            connToCable.compute(cable.srcConn(),(_, s)->
                    with(s == null ? new HashSet<>() : s, s1->s1.add(cable)));
            connToCable.compute(cable.destConn(),(_, s)->
                    with(s == null ? new HashSet<>() : s, s1->s1.add(cable)));
        }

        public void remove(Cable cable) {
            cables.remove(cable);
            connToCable.computeIfPresent(cable.srcConn(),(_,s)->
                    with(s,s1->s1.remove(cable)));
            connToCable.computeIfPresent(cable.destConn(),(_,s)->
                    with(s,s1->s1.remove(cable)));
        }

        public Set<Cable> cablesForConn(Connectors.Conn conn) {
            return connToCable.computeIfAbsent(conn,_->new HashSet<>());
        }

        public void clear() {
            cables.clear();
            connToCable.clear();
        }

        /**
         * mutation-safe iteration
         */
        public void forEach(Consumer<Cable> f) {
            new ArrayList<>(cables).forEach(f);;
        }




    }
    private final CableStore store = new CableStore();

    private final SlotPane slotPane;
    private final AreaPane areaPane;
    private final Property<CableDelta<Cable>> cableDelta =
            new SimpleObjectProperty<>(new CableDelta<>(List.of(),false));
    private final Bridges<PatchArea> bridges;

    public Cables(SlotPane slotPane, AreaPane areaPane, Bridges<PatchArea> bridges) {
        this.slotPane = slotPane;
        this.areaPane = areaPane;
        bridges.bridge(PatchArea::getDummyCableDeltaProp, new FxProperty.SimpleFxProperty<>(cableDelta, u ->
                new Undos.Undo<>(u.property(),u.newValue().invert(c->c.color().ordinal()),u.newValue())),
                        Iso.id());
        this.bridges = bridges;
        cableDelta.addListener((_,_,d)->{ if (d.add()) doAdd(d); else doDelete(d); });
    }



    private static void redrawRun(Cable c) {
        c.run.reset(mkCableRun(c.start,c.end,c.color));
    }

    private static Cable mkCable(Cable c) {
        return mkCable(c.srcConn,c.destConn);
    }

    private static Cable mkCable(Connectors.Conn srcConn, Connectors.Conn destConn) {

        Point2D start = srcConn.control().localToParent(
                srcConn.modulePane().getPane().getLayoutX(),srcConn.modulePane().getPane().getLayoutY()).add(6,6);
        Point2D end = destConn.control().localToParent(
                destConn.modulePane().getPane().getLayoutX(),destConn.modulePane().getPane().getLayoutY()).add(6,6);

        CableColor color = srcConn.getColor();

        var run = mkCableRun(start, end, color);

        RadialGradient gradient = new RadialGradient(
                180,   // focus angle (upper left is 315°)
                0,//0.6,   // focus distance (move shine a bit away from center)
                .3,.3,//0.34, 0.34, // center of highlight (upper left, proportional coordinates)
                1,//0.7,   // radius
                true,  // proportional
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),           // upper left shine
                new Stop(0.2, color.getFill()),             // dome color
                new Stop(0.8, color.getEdge()),         // lower right shade
                new Stop(1.0, Color.BLACK)            // deepest shade, e.g. edge fade
        );
        Circle srcJack = mkJack(gradient, start);
        srcJack.setOnContextMenuRequested(e -> srcConn.ctxMenuHandler().accept(srcConn,e));
        Circle destJack = mkJack(gradient,end);
        destJack.setOnContextMenuRequested(e -> destConn.ctxMenuHandler().accept(destConn,e));
        run.getCable().setOnContextMenuRequested(e -> {
            if (srcJack.getBoundsInParent().contains(e.getX(),e.getY())) {
                srcConn.ctxMenuHandler().accept(srcConn,e);
            }
            if (destJack.getBoundsInParent().contains(e.getX(),e.getY())) {
                destConn.ctxMenuHandler().accept(destConn,e);
            }
        });
        return new Cable(color,srcConn,start,destConn,end,run,srcJack,destJack);

    }

    public static CableRun mkCableRun(Point2D start, Point2D end, CableColor color) {

        ThreadLocalRandom r = ThreadLocalRandom.current();
        int offsetMagnitude = r.nextInt(10,20);
        double controlPointRatio = r.nextDouble(.1,.17);
        boolean flip = r.nextBoolean();
        Path cable = drawCablePath(start, end, offsetMagnitude, controlPointRatio, flip);
        Path shadow = drawCablePath(start, end, offsetMagnitude, controlPointRatio, flip);

        cable.setFill(null); // No fill, just stroke
        cable.setStrokeWidth(3);
        cable.setStrokeLineCap(StrokeLineCap.ROUND);
        cable.setStroke(color.getColor(0.8));

        shadow.setFill(null);
        shadow.setStrokeWidth(2);
        shadow.setStrokeLineCap(StrokeLineCap.ROUND);
        double width = 3.2;
        double shadowWidth = .75;
        shadow.setTranslateX(width/2-shadowWidth);
        shadow.setTranslateY(width/2-shadowWidth);
        var run = new CableRun(cable,shadow);
        return run;
    }


    private static Path drawCablePath(Point2D start, Point2D end, double offsetMagnitude,
                                      double controlPointRatio, boolean flip) {
        Path path = new Path();

        MoveTo moveTo = new MoveTo(start.getX(), start.getY());
        path.getElements().add(moveTo);

        Point2D dir = end.subtract(start);
        double length = dir.magnitude();
        offsetMagnitude = Math.abs(offsetMagnitude * length * .01);
        Point2D unitDir = dir.normalize();

        Point2D perp = new Point2D(-unitDir.getY(), unitDir.getX());
        if (flip) perp = perp.multiply(-1);

        Point2D control1 = start.add(unitDir.multiply(controlPointRatio * length))
                .add(perp.multiply(offsetMagnitude));
        Point2D control2 = end.subtract(unitDir.multiply(controlPointRatio * length))
                .subtract(perp.multiply(offsetMagnitude));

        CubicCurveTo cubicCurveTo = new CubicCurveTo();
        cubicCurveTo.setControlX1(control1.getX());
        cubicCurveTo.setControlY1(control1.getY());
        cubicCurveTo.setControlX2(control2.getX());
        cubicCurveTo.setControlY2(control2.getY());
        cubicCurveTo.setX(end.getX());
        cubicCurveTo.setY(end.getY());

        path.getElements().add(cubicCurveTo);

        return path;
    }


    private static Circle mkJack(RadialGradient gradient, Point2D pos) {
        Circle srcJack = new Circle(0,0,RADIUS*.55);
        srcJack.getStyleClass().add("cable-jack");
        srcJack.setFill(gradient);
        srcJack.setLayoutX(pos.getX());
        srcJack.setLayoutY(pos.getY());
        return srcJack;
    }


    public void renderCables(List<PatchCable> patchCables) {
        // fx thread with fresh list of "immutable" PatchCable instances
        for (PatchCable patchCable : patchCables) {
            var src = resolveModule(patchCable.getSrcModule());
            var dest = resolveModule(patchCable.getDestModule());
            Connector.ConnDir fromConnType = patchCable.getDirection() ? Out : In;
            var srcConn = src.resolveConn(fromConnType == In ? In : Out, patchCable.getSrcConn());
            var destConn = dest.resolveConn(In, patchCable.getDestConn());
            addCable(srcConn, destConn);
        }
        redrawCables(false);
    }



    private ModulePane resolveModule(int mIdx) {
        ModulePane mp = areaPane.getModule(mIdx);
        if (mp == null) { throw new IllegalStateException("patchCable invalid module index: " + mIdx); }
        return mp;
    }


    public void redrawCables(boolean shake) {
        store.forEach(cable -> {
            areaPane.getAreaPane().getChildren().removeAll(cable.run().getCable(), cable.run().getShadow());
            if (shake) Cables.redrawRun(cable);
            if (slotPane.isCableVisible(cable))
                areaPane.getAreaPane().getChildren().addAll(cable.run().getShadow(), cable.run().getCable());
        });
    }

    public void moduleMoved(ModulePane mp) {
        store.forEach(c -> {
            if (mp == c.srcConn().modulePane()|| mp == c.destConn().modulePane()) {
                store.remove(c);
                removeCableElements(c);
                Cables.Cable cnew = Cables.mkCable(c);
                store.add(cnew);
                areaPane.getAreaPane().getChildren().addAll(cnew.endJack(),cnew.srcJack());
            }
        });
    }

    private void removeCableElements(Cable c) {
        areaPane.getAreaPane().getChildren().removeAll(
                c.endJack(), c.srcJack(), c.run().getShadow(), c.run().getCable());
    }

    public void clear() {
        store.clear();
    }

    public void doDeleteModule(ModuleDelta md) {
        store.forEach(cable -> {
            for (FieldValues c : md.cables()) {
                if (cable.srcConn().modulePane().getIndex() == Protocol.Cable.DestModule.intValue(c) &&
                        cable.srcConn().index() == Protocol.Cable.DestConn.intValue(c) &&
                        cable.destConn().modulePane().getIndex() == Protocol.Cable.SrcModule.intValue(c) &&
                        cable.destConn().index() == Protocol.Cable.SrcConn.intValue(c)) {
                    store.remove(cable);
                    removeCableElements(cable);
                }
            }
        });
    }

    public int size() {
        return store.cables.size();
    }

    /**
     * exposed for test
     */
    public Cable addCable(Connectors.Conn srcConn, Connectors.Conn destConn) {
        Cable cable = Cables.mkCable(srcConn, destConn);
        addCable(cable);
        return cable;
    }

    private void addCable(Cable cable) {
        store.add(cable);
        areaPane.getAreaPane().getChildren().addAll(cable.srcJack(), cable.endJack());
    }

    private void doDelete(CableDelta<Cable> d) {
        d.cables().forEach(c -> {
            store.remove(c);
            removeCableElements(c);
        });
        doDelta(d);
    }

    private void doDelta(CableDelta<Cable> d) {
        d.uprateChanges().forEach((i, r)->areaPane.getModule(i).uprate().setValue(r));
        d.colorChanges().forEach((c, v)-> {
            if (d.cables().contains(c)) { return; }
            store.remove(c);
            store.add(c.changeColor(CableColor.LOOKUP.get(v)));
        });
        bridges.getLibExecutor().runWithCurrent(a -> a.execCableDelta(d.convert(Cable::toCableIndex)));
    }

    private void doAdd(CableDelta<Cable> d) {
        d.cables().forEach(this::addCable);
        redrawCables(false);
        doDelta(d);
    }

    /**
     * exposed for test
     */
    public void checkUprate(Connectors.Conn to,
                             boolean uprate,
                             CableDelta<Cable> delta) {
        // bail if module already good
        if (to.modulePane().uprate().getValue() == uprate) { return; }
        // bail if module change already good
        if (to.newUprate(delta) == uprate) { return; }
        if (!uprate) {
            // downrate: walk module upstream cables to check if canceled by uprate
            for (Connectors.Conn in : to.modulePane().getConns(Connector.ConnDir.In)) {
                if (in == to) { continue; }
                for (Cable cable : store.cablesForConn(in)) {
                    if (cable.srcConn.newUprate(delta)) {
                        //uprate found, bail
                        return;
                    }
                }
            }
        }
        // add uprate
        delta.uprateChanges().put(to.modulePane().getIndex(),uprate);
        // walk module out-cables to compute color changes and downstream uprates
        for (Connectors.Conn out : to.modulePane().getConns(Connector.ConnDir.Out)) {
            for (Cable cable : store.cablesForConn(out)) {
                CableColor newColor = out.getNewColor(delta);
                if (cable.destConn().connector().bandwidth() == Connector.Bandwidth.Dynamic &&
                        cable.destConn().newUprate(delta) != cable.srcConn().newUprate(delta)) {
                    delta.colorChanges().put(cable, newColor.ordinal());
                    if (cable.destConn.modulePane() != to.modulePane()) {
                        checkUprate(cable.destConn, uprate, delta);
                    }
                } else if (cable.color() != newColor) {
                    delta.colorChanges().put(cable, newColor.ordinal());
                }
            }
        }
    }

    public void mkConnCtxMenu(Connectors.Conn conn, ContextMenuEvent cme) {
        //find cable if any
        Set<Cables.Cable> cs = new HashSet<>(store.cablesForConn(conn));

        ContextMenu cm = new ContextMenu();
        if (!cs.isEmpty()) {
            cm.getItems().add(mkMenuItem("Disconnect", _ -> ctxDisconnectConn(conn, cs)));
            if (cs.size() > 1) cm.getItems().add(mkMenuItem("Break", _ -> ctxBreakConn(conn, cs)));
            Menu m = mkMenu("Color");
            Arrays.stream(Cables.ColorSelection.values()).forEach(v ->
                    m.getItems().add(mkMenuItem(v.displayName(),_-> ctxSetCableColor(cs,v))));
            cm.getItems().add(m);
            cm.getItems().add(mkMenuItem("Delete",_-> ctxDeleteCable(cs)));
        }
        cm.getItems().add(mkMenuItem("Delete Unused Cables", _ -> ctxDeleteUnusedCables()));
        cm.show(conn.control(),cme.getScreenX(),cme.getScreenY());

    }


    private void ctxDeleteCable(Set<Cables.Cable> cs) {
        deleteCables(cs);
    }

    private void ctxSetCableColor(Set<Cables.Cable> cs, Cables.ColorSelection color) {
        log.warning("ctxSetCableColor TODO");

    }

    private void ctxDisconnectConn(Connectors.Conn conn, Set<Cables.Cable> cs) {
        log.warning("ctxDisconnectConn TODO");

    }

    private void ctxBreakConn(Connectors.Conn conn, Set<Cables.Cable> cs) {
        log.warning("ctxBreakConn TODO");

    }

    private void ctxDeleteUnusedCables() {
        log.warning("ctxDeleteUnusedCables TODO");
    }

    private void deleteCables(Set<Cable> cs) {
        CableDelta<Cable> delta = new CableDelta<>(cs,false);
        for (Cable c : cs) {
            checkUprate(c.destConn, c.destConn.defaultUprate(), delta);
        }
        cableDelta.setValue(delta);
    }

    /**
     * UI cable add
     */
    public void newCable(Connectors.Conn start, Connectors.Conn end) {
        Cable c = mkCable(start,end);
        CableDelta<Cable> delta = new CableDelta<>(Set.of(c),true);
        checkUprate(c.destConn,c.srcConn.getCurrentUprate(),delta);
        cableDelta.setValue(delta);
    }


}
