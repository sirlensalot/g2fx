package org.g2fx.g2gui.controls;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.g2fx.g2gui.ui.UIElement;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2lib.model.Connector;

import static org.g2fx.g2gui.FXUtil.withClass;
import static org.g2fx.g2gui.controls.CableColor.*;
import static org.g2fx.g2gui.panel.ModulePane.layout;
import static org.g2fx.g2lib.model.Connector.PortType.In;
import static org.g2fx.g2lib.model.Connector.PortType.Out;

public interface Connectors {


    double RADIUS = 5.5;
    double HOLE_RADIUS = RADIUS * .5;

    record Conn(Connector.PortType portType, UIElements.ConnectorType connType, Node control, int index) {}

    static Conn makeInput(UIElements.Input c) {
        return mkConnector(c, c.Type(), c.CodeRef(), In);
    }

    static Conn makeOutput(UIElements.Output c) {
        return mkConnector(c, c.Type(), c.CodeRef(), Out);
    }
    private static Conn mkConnector(UIElement c, UIElements.ConnectorType ctype, int ref, Connector.PortType portType) {
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
        return new Conn(portType,ctype,pane,ref);
    }

    public static CableColor getConnColor(UIElements.ConnectorType ctype) {
        return switch (ctype) {
            case Audio -> Red;
            case Control -> Blue;
            case Logic -> Yellow;
        };
    }

}
