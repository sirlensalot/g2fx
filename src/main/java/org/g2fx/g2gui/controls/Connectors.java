package org.g2fx.g2gui.controls;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.g2fx.g2gui.panel.AreaPane;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.ui.UIElement;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2lib.model.CableDelta;
import org.g2fx.g2lib.model.Connector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.controls.CableColor.*;
import static org.g2fx.g2gui.panel.ModulePane.layout;
import static org.g2fx.g2gui.ui.UIElements.Bandwidth.Dynamic;
import static org.g2fx.g2gui.ui.UIElements.Bandwidth.Static;
import static org.g2fx.g2lib.model.Connector.PortType.In;
import static org.g2fx.g2lib.model.Connector.PortType.Out;

/**
 * Houses code for drawing Conn connectors, and is AreaPane delegate for
 * handling cable-dragging.
 */
public class Connectors {

    public static double RADIUS = 5.5;
    public static double HOLE_RADIUS = RADIUS * .5;
    private Point2D dragOrigin;
    private Cables.CableRun cr;


    public record Conn(Connector.PortType portType,
                       UIElements.ConnectorType connType,
                       UIElements.Bandwidth bandwidth,
                       Node control,
                       int index,
                       ModulePane modulePane,
                       BiConsumer<Conn, ContextMenuEvent> ctxMenuHandler,
                       Shape edge, Circle center) {
        @Override
        public String toString() {
            return modulePane + ":" + portType + ":" + index + ":" + connType;
        }

        public boolean validate(Conn c) {
            return portType != c.portType;
        }

        public CableColor getDefaultColor() {
            return switch (connType) {
                case UIElements.ConnectorType.Logic -> Yellow;
                case UIElements.ConnectorType.Control -> Blue;
                case UIElements.ConnectorType.Audio ->
                        bandwidth == Static ? Red : Blue;
            };
        }

        public CableColor getColor(boolean isModuleUprate) {
            CableColor def = getDefaultColor();
            boolean dynamicUprate = isModuleUprate && bandwidth() == Dynamic;
            return (def == Blue && dynamicUprate) ? Red :
                    (def == Yellow && dynamicUprate) ? Orange : def;
        }

        public CableColor getColor() { return getColor(modulePane.uprate().getValue()); }
        public CableColor getNewColor(CableDelta<?> delta) {
            return getColor(getNewModuleUprate(delta));
        }

        public boolean defaultUprate() {
            return connType()== UIElements.ConnectorType.Audio && bandwidth() == Static;
        }

        public boolean getCurrentUprate() {
            return (modulePane.uprate().getValue() && bandwidth()==Dynamic) || defaultUprate();
        }

        public boolean newUprate(CableDelta<?> delta) {
            return (getNewModuleUprate(delta) && bandwidth() == Dynamic) || defaultUprate();
        }

        private boolean getNewModuleUprate(CableDelta<?> delta) {
            return delta.uprateChanges().getOrDefault(modulePane.getIndex(), modulePane.uprate().getValue());
        }

        public void setColor(CableColor color) {
            edge.setStroke(color.getEdge());
            edge.setFill(color.getFill());
            center.setStroke(color.getEdge());
        }

        public void setColorForUprate(boolean uprate) {
            if (connType == UIElements.ConnectorType.Audio || bandwidth != Dynamic) { return; }
            setColor(connType == UIElements.ConnectorType.Control ? (uprate ? Red : Blue) :
                    uprate ? Orange : Yellow);
        }
    }

    public static Conn makeInput(UIElements.Input c, ModulePane modulePane, BiConsumer<Conn,ContextMenuEvent> ctxMenuHandler) {
        return mkConnector(c, c.Type(), c.Bandwidth(), c.CodeRef(), In, modulePane, ctxMenuHandler);
    }

    public static Conn makeOutput(UIElements.Output c, ModulePane modulePane, BiConsumer<Conn,ContextMenuEvent> ctxMenuHandler) {
        return mkConnector(c, c.Type(), c.Bandwidth(), c.CodeRef(), Out, modulePane, ctxMenuHandler);
    }
    private static Conn mkConnector(UIElement c,
                                    UIElements.ConnectorType ctype,
                                    UIElements.Bandwidth bandwidth,
                                    int ref,
                                    Connector.PortType portType,
                                    ModulePane modulePane,
                                    BiConsumer<Conn,ContextMenuEvent> ctxMenuHandler) {
        var color = getConnColor(ctype);
        Shape edge = portType == In ? new Circle(0,0, RADIUS) : new Rectangle(0,0, RADIUS *2, RADIUS *2);
        edge.getStyleClass().add("conn-edge");

        Circle center = new Circle(-1,-1, HOLE_RADIUS);
        center.getStyleClass().add("conn-center");
        center.setFill(Color.BLACK);

        StackPane pane = withClass(new StackPane(edge,center),"conn-pane");
        pane.setAlignment(Pos.CENTER);
        layout(c,pane);
        Conn conn = new Conn(portType, ctype, bandwidth, pane, ref, modulePane, ctxMenuHandler, edge, center);
        conn.setColor(color);
        pane.setOnContextMenuRequested(e -> ctxMenuHandler.accept(conn,e));

        return conn;
    }

    public static CableColor getConnColor(UIElements.ConnectorType ctype) {
        return switch (ctype) {
            case Audio -> Red;
            case Control -> Blue;
            case Logic -> Yellow;
        };
    }

    private final List<Conn> conns = new ArrayList<>();
    private final AreaPane areaPane;
    private Conn current;

    public Connectors(AreaPane areaPane) {
        this.areaPane = areaPane;
    }


    public void addConn(Conn conn) {
        conns.add(conn);
        conn.control().setOnMousePressed(e -> {
            current = conn;
            dragOrigin = areaPane.getAreaPane().sceneToLocal(e.getSceneX(),e.getSceneY());
            e.consume();
        });
        conn.control().setOnMouseDragged(e -> {
            if (dragOrigin == null) { return; }
            clearCableRun();
            cr = Cables.mkCableRun(dragOrigin, areaPane.getAreaPane().sceneToLocal(e.getSceneX(),e.getSceneY()), getConnColor(conn.connType));
            areaPane.getAreaPane().getChildren().addAll(cr.getShadow(),cr.getCable());
        });
        conn.control().setOnMouseReleased(e -> {
            clearCableRun();
            for (Conn c : conns) {
                if (c.control().localToScene(c.control().getBoundsInLocal()).contains(e.getSceneX(),e.getSceneY()) &&
                        conn.validate(c)) {
                    areaPane.newCable(conn,c);
                }
            }
        });
    }

    private void clearCableRun() {
        if (cr != null) {
            areaPane.getAreaPane().getChildren().removeAll(cr.getCable(),cr.getShadow());
        }
        cr = null;
    }

    public void clear() {
        conns.clear();
    }

}
