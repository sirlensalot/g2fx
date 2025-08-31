package org.g2fx.g2gui.controls;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;

public interface Cables {

    static Polyline mkCable(Point2D start, Point2D end) {
        int segments = 40;
        double g = 1;
        double H = 1000;

        Point2D[] cablePoints = calculateCatenaryPoints(start, end, segments, g, H);

        Polyline cable = new Polyline();
        for (Point2D point : cablePoints) {
            cable.getPoints().addAll(point.getX(), point.getY());
        }

        cable.setStroke(Color.DARKGRAY);
        cable.setStrokeWidth(4);
        cable.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        return cable;

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
