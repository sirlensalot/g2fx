package org.g2fx.g2gui.controls;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.g2fx.g2gui.panel.AreaPane;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.ui.UIElement;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2lib.model.Connector;

import java.util.ArrayList;
import java.util.List;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.controls.CableColor.*;
import static org.g2fx.g2gui.panel.ModulePane.layout;
import static org.g2fx.g2lib.model.Connector.PortType.In;
import static org.g2fx.g2lib.model.Connector.PortType.Out;

public class Connectors {


    public static double RADIUS = 5.5;
    public static double HOLE_RADIUS = RADIUS * .5;
    private Point2D dragOrigin;
    private Cables.CableRun cr;


    public record Conn(Connector.PortType portType, UIElements.ConnectorType connType, Node control, int index, ModulePane parent) {
        @Override
        public String toString() {
            return parent + ":" + portType + ":" + index + ":" + connType;
        }

        public boolean validate(Conn c) {
            return portType != c.portType;
        }
    }

    public static Conn makeInput(UIElements.Input c, ModulePane modulePane) {
        return mkConnector(c, c.Type(), c.CodeRef(), In, modulePane);
    }

    public static Conn makeOutput(UIElements.Output c, ModulePane modulePane) {
        return mkConnector(c, c.Type(), c.CodeRef(), Out, modulePane);
    }
    private static Conn mkConnector(UIElement c, UIElements.ConnectorType ctype, int ref, Connector.PortType portType, ModulePane modulePane) {
        var color = getConnColor(ctype);
        Shape edge = portType == In ? new Circle(0,0, RADIUS) : new Rectangle(0,0, RADIUS *2, RADIUS *2);
        double sat = 0.6;
        edge.getStyleClass().add("conn-edge");
        edge.setStroke(color.getEdge());
        edge.setFill(color.getFill());

        Circle center = new Circle(-1,-1, HOLE_RADIUS);
        center.getStyleClass().add("conn-center");
        center.setStroke(color.getEdge());
        center.setFill(Color.BLACK);

        StackPane pane = withClass(new StackPane(edge,center),"conn-pane");
        pane.setAlignment(Pos.CENTER);
        layout(c,pane);
        return new Conn(portType,ctype,pane,ref,modulePane);
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

}
