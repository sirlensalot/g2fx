package org.g2fx.g2gui.controls;

import com.sun.javafx.scene.shape.CircleHelper;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;

public class Knob extends Control {

    private int value;
    private record Drag(double start,int value) {};
    private Drag drag;

    public static final int MIN=0;
    public static final int MAX=63;

    private SVGPath thumb;

    public Knob() {
        super();
        getStyleClass().add("knob");
        setWidth(30);
        setHeight(30);

        double cx=0,cy=0;

        Circle edge = new Circle(cx,cy,13);
        edge.getStyleClass().add("knob-edge");

        Circle center = new Circle(cx,cy,10);
        center.getStyleClass().add("knob-center");

        Line l1 = new Line(0,3,3,0);
        l1.setTranslateX(-10);
        l1.setTranslateY(10);
        l1.getStyleClass().add("knob-line");

        Line l2 = new Line(0,0,3,3);
        l2.setTranslateX(10);
        l2.setTranslateY(10);
        l1.getStyleClass().add("knob-line");

        thumb = new SVGPath();
        thumb.setContent("M 0,-24 v -1 z M 0,0 v -12 z");
        thumb.getStyleClass().add("knob-thumb");
        Line tl = new Line(0,0,0,11);
        tl.setTranslateY(-7);
        tl.getStyleClass().add("knob-line");


        getChildren().addAll(l1,l2,edge,center,thumb);

        setOnMouseDragged(this::mouseDragged);
        setOnMousePressed(this::mousePressed);

    }

    private void mouseDragged(MouseEvent e) {
        if (drag == null) {
            drag = new Drag(e.getY(),getValue());
            return;
        }
        double dist = (drag.start() - e.getY()) / 2;
        int v = (int) (drag.value() + dist);
        v = Math.max(MIN,Math.min(v, MAX));
        System.out.printf("Drag: %s %s\n",dist,v);
        setValue(v);
    }

    private void mousePressed(MouseEvent e) {
        System.out.printf("Press: %s\n",e);
        //setValue(30);
        drag=null;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException("Invalid knob value: " + value);
        }
        this.value = value;
        double pctg = ((double) value) / ((double) MAX);
        double angle = pctg * 270 + 45;
        System.out.format("setValue: value=%s, pctg=%s, angle=%s",value,pctg,angle);
        thumb.setRotate(angle);
    }

    @Override
    protected Skin<Knob> createDefaultSkin() {
        return new SkinBase<Knob>(this) {
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
