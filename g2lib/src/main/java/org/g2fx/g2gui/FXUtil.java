package org.g2fx.g2gui;

import javafx.css.Styleable;

public class FXUtil {

    public static <T extends Styleable> T withClass(T node, String... classes) {
        node.getStyleClass().addAll(classes);
        return node;
    }

}
