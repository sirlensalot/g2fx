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
import static org.g2fx.g2gui.panel.ModulePane.layout;
import static org.g2fx.g2lib.model.Connector.PortType.In;
import static org.g2fx.g2lib.model.Connector.PortType.Out;

public interface Connectors {

    double RED = 355;
    double ORANGE = 30;
    double BLUE = 210;
    double YELLOW = 60;

    record Conn(Connector.PortType portType, UIElements.ConnectorType connType, Node control, int index) {}

    static Conn makeInput(UIElements.Input c) {
        return mkConnector(c, c.Type(), c.CodeRef(), In);
    }

    static Conn makeOutput(UIElements.Output c) {
        return mkConnector(c, c.Type(), c.CodeRef(), Out);
    }
    private static Conn mkConnector(UIElement c, UIElements.ConnectorType ctype, int ref, Connector.PortType portType) {
        double hue = switch (ctype) {
            case Audio -> RED;
            case Control -> BLUE;
            case Logic -> YELLOW;
        };
        double r = 5.5;
        Shape edge = portType == In ? new Circle(0,0,r) : new Rectangle(0,0,r*2,r*2);
        double sat = 0.6;
        edge.getStyleClass().add("conn-edge");
        edge.setStroke(Color.hsb(hue,sat,.6));
        edge.setFill(Color.hsb(hue,sat,1));

        Circle center = new Circle(-1,-1,r*.5);
        center.getStyleClass().add("conn-center");
        center.setStroke(Color.hsb(hue,sat,.6));
        center.setFill(Color.hsb(hue,0,0));

        StackPane pane = withClass(new StackPane(edge,center),"conn-pane");
        pane.setAlignment(Pos.CENTER);
        layout(c,pane);
        return new Conn(portType,ctype,pane,ref);
    }

}
