package org.g2fx.g2gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public interface TextFieldFocusListener {
        void focusChange(boolean acquired);
    }

    public static SimpleStringProperty mkTextFieldCommitProperty(TextField textField, TextFieldFocusListener focusListener) {
        SimpleStringProperty p = new SimpleStringProperty();
        textField.setTextFormatter(new TextFormatter<>(new StringConverter<>() {
            @Override public String toString(String object) { return object; }
            @Override public String fromString(String string) { return string; }
        }, textField.getText()));
        AtomicBoolean committing = new AtomicBoolean(false);
        textField.getTextFormatter().valueProperty().addListener((c,o,n) -> {
            if (n != null && !n.equals(o)) {
                committing.set(true);
                p.set((String) n);
                committing.set(false);
            }});
        p.addListener((c,o,n) -> {
            if (!committing.get()) {
                textField.setText(n);
            }});

        textField.setEditable(false);
        textField.setFocusTraversable(false);

        textField.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                textField.setEditable(true);
                textField.requestFocus();
                focusListener.focusChange(true);
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                textField.setEditable(false);
                focusListener.focusChange(false);
            }
        });
        return p;
    }
}
