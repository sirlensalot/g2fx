package org.g2fx.g2gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

public class FXUtil {

    public static final int UI_MAX_VARIATIONS = 8;
    public static final String G2_TOGGLE = "g2-toggle";

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

    public static Label label(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setLineSpacing(-3);
        return l;
    }

    public static ToggleButton radioToToggle(ToggleButton b) {
        b.getStyleClass().remove("radio-button");
        b.getStyleClass().add("toggle-button");
        return b;
    }

    public static Preferences getPrefs() {
        return Preferences.userNodeForPackage(FXUtil.class);
    }

    public static Background rgbFill(String item) {
        return new Background(new BackgroundFill(Color.web(item), null, null));
    }

    /**
     * Global listener to disable bare-key accelerators.
     */
    public interface TextFieldFocusListener {
        /**
         * @param acquired true if text field acquired focus, false if released focus.
         */
        void focusChange(boolean acquired);
    }

    public static SimpleStringProperty mkTextFieldCommitProperty(TextField textField, TextFieldFocusListener focusListener, int maxLength) {
        SimpleStringProperty p = new SimpleStringProperty();
        setTextFieldMaxLength(textField, maxLength);
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

    public static <T extends Pane> T addChildren(T parent, Collection<? extends Node> children) {
        parent.getChildren().addAll(children);
        return parent;
    }

    public static TextField setTextFieldMaxLength(TextField textField, int maxLength) {
        textField.setTextFormatter(new TextFormatter<>(new StringConverter<>() {
            @Override public String toString(String object) { return object; }
            @Override public String fromString(String string) { return string; }
        }, textField.getText(), c -> { return c.getControlNewText().length() <= maxLength ? c : null; }));
        return textField;
    }
}
