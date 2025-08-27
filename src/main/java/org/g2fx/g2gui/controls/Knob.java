package org.g2fx.g2gui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.List;

public class Knob extends Control {

    public static final double SIZE = 20;

    public final int min;
    public final int max;
    private final SimpleObjectProperty<Integer> value;
    private final String name;

    private record Drag(double start,int value) {}
    private Drag drag;
    private final SimpleBooleanProperty valueChanging = new SimpleBooleanProperty(false);

    private final SVGPath thumb;

    public Knob(String name, double scale) {
        this(name,scale,false,0,127);
    }
    public Knob(String name, double scale, boolean reset,int min,int max) {
        super();
        this.max= max;
        this.min=min;
        this.name = name;
        value = new SimpleObjectProperty<>(this,name,0);
        value.addListener((v,o,n) ->
                setValue(n));
        getStyleClass().add("knob");
        setWidth(SIZE);
        setHeight(SIZE);

        double cx=0,cy=0;

        Circle edge = new Circle(cx,cy,10);
        edge.getStyleClass().add("knob-edge");

        Circle center = new Circle(cx,cy,8);
        center.getStyleClass().add("knob-center");

        Line l1 = new Line(0,2,2,0);
        l1.setTranslateX(-8);
        l1.setTranslateY(8);
        l1.getStyleClass().add("knob-line");

        Line l2 = new Line(0,0,2,2);
        l2.setTranslateX(8);
        l2.setTranslateY(8);
        l2.getStyleClass().add("knob-line");

        thumb = new SVGPath();
        thumb.setContent("M 0,-19 v -1 z M 0,0 v -10 z");
        thumb.getStyleClass().add("knob-thumb");

        List<Shape> cs = new ArrayList<>(List.of(l1, l2, edge, center, thumb));
        if (reset) {
            int size = 11;
            int bottom = 6;
            Polygon triangle = new Polygon(
                    0.0, 0.0,
                    size, 0.0,
                    size/2, bottom
            );
            triangle.setFill(Color.GREEN);
            triangle.setStroke(Color.BLACK);
            //triangle.setTranslateX(2);
            triangle.setTranslateY(-12.5);
            value.addListener((c,o,n) -> {
                triangle.setFill(n == max /2 + 1 ? Color.LIGHTGREEN : Color.GREEN);
            });
            cs.addFirst(triangle);
            setTranslateY(bottom-1);
        }
        getChildren().addAll(cs);
        setScaleX(scale);
        setScaleY(scale);

        setOnMouseDragged(this::mouseDragged);
        setOnMouseReleased(event -> valueChanging.set(false));
        setOnMousePressed(this::mousePressed);


        value.set(0);
        setValue(0);
    }

    private void mouseDragged(MouseEvent e) {
        if (drag == null) {
            drag = new Drag(e.getY(),getValue());
            return;
        }
        double dist = (drag.start() - e.getY()) / 2;
        int v = (int) (drag.value() + dist);
        v = Math.max(min,Math.min(v, max));
        value.set(v);
    }

    private void mousePressed(MouseEvent e) {
        drag=null; //reset drag
        valueChanging.set(true);
        double x = (SIZE/2) - e.getX();
        double y = e.getY() - (SIZE/2);
        double a = Math.atan2(x,y) * (180/Math.PI);
        a = a < 0 ? (360 + a) : a;
        a = Math.max(45,(Math.min(315,a)));
        double pctg = (a - 45) / 270;
        int v = (int) (pctg * max);
        value.set(v);
    }

    public int getValue() {
        return value.get();
    }

    public void setValue(int value) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("setValue [" + name + "]: Invalid knob value: " + value);
        }
        double pctg = ((double) value) / ((double) max);
        double angle = pctg * 270 + 45;
        //System.out.format("setValue: value=%s, pctg=%s, angle=%s",value,pctg,angle);
        thumb.setRotate(angle);
    }

    public Property<Integer> getValueProperty() {
        return value;
    }

    public BooleanProperty valueChangingProperty() {
        return valueChanging;
    }



    @Override
    protected Skin<Knob> createDefaultSkin() {
        return new SkinBase<>(this) {
            @Override
            public void dispose() {
                super.dispose();
            }
        };
    }

    @Override
    public boolean isResizable() {
        return false;
    }
}
