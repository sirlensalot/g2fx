package org.g2fx.g2gui.controls;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;

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
            CableColor color,
            Connectors.Conn srcConn,
            Point2D start,
            Connectors.Conn destConn,
            Point2D end,
            CableRun run,
            Node srcJack,
            Node endJack) {}

    static void redrawRun(Cable c) {
        c.run.reset(mkCableRun(c.start,c.end,c.color));
    }

    static Cable mkCable(Cable c) {
        return mkCable(c.srcConn,c.destConn);
    }

    static Cable mkCable(Connectors.Conn srcConn, Connectors.Conn destConn) {

        Point2D start = srcConn.control().localToParent(
                srcConn.parent().getPane().getLayoutX(),srcConn.parent().getPane().getLayoutY()).add(6,6);
        Point2D end = destConn.control().localToParent(
                destConn.parent().getPane().getLayoutX(),destConn.parent().getPane().getLayoutY()).add(6,6);

        CableColor color = getConnColor(srcConn.connType());

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


    static Path drawCablePath(Point2D start, Point2D end, double offsetMagnitude,
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


    public static Circle mkJack(RadialGradient gradient, Point2D pos) {
        Circle srcJack = new Circle(0,0,RADIUS*.55);
        srcJack.getStyleClass().add("cable-jack");
        srcJack.setFill(gradient);
        srcJack.setLayoutX(pos.getX());
        srcJack.setLayoutY(pos.getY());
        return srcJack;
    }


}
