package org.g2fx.g2gui.controls;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import org.g2fx.g2gui.panel.ModulePane;

import java.util.concurrent.ThreadLocalRandom;

import static org.g2fx.g2gui.controls.Connectors.RADIUS;
import static org.g2fx.g2gui.controls.Connectors.getConnColor;

public interface Cables {

    class CableRun {
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
    record Cable(
            Connectors.ConnectorColor color,
            ModulePane src,
            Connectors.Conn srcConn,
            Point2D start,
            ModulePane dest,
            Connectors.Conn destConn,
            Point2D end,
            CableRun run,
            Node srcJack,
            Node endJack) {}

    static void redrawRun(Cable c) {
        c.run.reset(mkCableRun(c.start,c.end,c.color));
    }

    static Cable mkCable(ModulePane src, Connectors.Conn srcConn, ModulePane dest, Connectors.Conn destConn) {

        Point2D start = srcConn.control().localToParent(
                src.getPane().getLayoutX(),src.getPane().getLayoutY()).add(6,6);
        Point2D end = destConn.control().localToParent(
                dest.getPane().getLayoutX(),dest.getPane().getLayoutY()).add(6,6);

        Connectors.ConnectorColor color = getConnColor(srcConn.connType());

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
        Circle destJack = mkJack(gradient,end);

        return new Cable(color,src,srcConn,start,dest,destConn,end,run,srcJack,destJack);


    }

    private static CableRun mkCableRun(Point2D start, Point2D end, Connectors.ConnectorColor color) {
        int segments = 40;
        double g = 1;
        double h = 200;
        double H = ThreadLocalRandom.current().nextDouble(h, h+600);

        Point2D[] cablePoints = calculateCatenaryPoints(start, end, segments, g, H);

        Polyline cable = new Polyline();
        Polyline shadow = new Polyline();
        for (Point2D point : cablePoints) {
            cable.getPoints().addAll(point.getX(), point.getY());
            shadow.getPoints().addAll(point.getX(), point.getY());
        }


        cable.setStroke(color.getColor(0.8));
        double width = 3.2;
        cable.setStrokeWidth(3);
        cable.setStrokeLineCap(StrokeLineCap.ROUND);

        shadow.setStroke(color.getColor(0.4));
        double shadowWidth = .75;
        shadow.setStrokeWidth(2);
        shadow.setStrokeLineCap(StrokeLineCap.ROUND);
        shadow.setTranslateX(width/2-shadowWidth);
        shadow.setTranslateY(width/2-shadowWidth);

        var run = new CableRun(cable,shadow);
        return run;
    }

    private static Circle mkJack(RadialGradient gradient, Point2D pos) {
        Circle srcJack = new Circle(0,0,RADIUS*.55);
        srcJack.getStyleClass().add("cable-jack");
        srcJack.setFill(gradient);
        srcJack.setLayoutX(pos.getX());
        srcJack.setLayoutY(pos.getY());
        return srcJack;
    }

    static double catenary(double x, double g, double H) {
        return -H / g * (Math.cosh(g * x / H) - 1);
    }

    static Point2D[] calculateCatenaryPoints(Point2D p1, Point2D p2, int segments, double g, double H) {
        Point2D[] points = new Point2D[segments + 2];
        double dx = (p2.getX() - p1.getX()) / (segments + 1);
        double dy = (p2.getY() - p1.getY()) / (segments + 1);
        double halfx = (p2.getX() - p1.getX()) / 2;
        double maxSag = catenary(-halfx, g, H);

        points[0] = p1;
        for (int i = 1; i <= segments; i++) {
            double x = p1.getX() + dx * i;
            double y = p1.getY() + dy * i + catenary(dx * i - halfx, g, H) - maxSag;
            points[i] = new Point2D(x, y);
        }
        points[segments + 1] = p2;

        return points;
    }

}
