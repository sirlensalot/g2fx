package org.g2fx.g2gui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;

public class Knob extends Control {

    public static final double SIZE = 20;

    public static final int MIN=0;
    public static final int MAX=127;
    private final SimpleObjectProperty<Integer> value;
    private record Drag(double start,int value) {}
    private Drag drag;
    private final SimpleBooleanProperty valueChanging = new SimpleBooleanProperty(false);

    private final SVGPath thumb;

    public Knob(String name) {
        super();
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

        getChildren().addAll(l1,l2,edge,center,thumb);

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
        v = Math.max(MIN,Math.min(v, MAX));
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
        int v = (int) (pctg * MAX);
        value.set(v);
    }

    public int getValue() {
        return value.get();
    }

    public void setValue(int value) {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException("Invalid knob value: " + value);
        }
        double pctg = ((double) value) / ((double) MAX);
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
