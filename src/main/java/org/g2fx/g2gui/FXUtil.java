package org.g2fx.g2gui;

import javafx.css.Styleable;
import javafx.scene.Node;

import java.net.URL;
import java.util.List;

public class FXUtil {

    public static <T extends Styleable> T withClass(T node, String... classes) {
        node.getStyleClass().addAll(classes);
        return node;
    }

    public static URL getResource(String file) {
        URL r = FXUtil.class.getResource(file);
        if (r == null) {
            throw new IllegalArgumentException("getResource failed: " + file);
        }
        return r;
    }

    public static Node[] toArray(List<? extends Node> children) {
        return children.toArray(new Node[] {});
    }
}
