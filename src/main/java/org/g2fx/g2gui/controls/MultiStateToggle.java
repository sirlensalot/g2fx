package org.g2fx.g2gui.controls;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ToggleButton;

import java.util.List;

import static org.g2fx.g2gui.FXUtil.withClass;

public class MultiStateToggle {

    private final SimpleObjectProperty<Integer> state;
    private final ToggleButton toggle;

    public MultiStateToggle(List<TextOrImage> statuses, int defaultStatus, String cssClass) {
        state = new SimpleObjectProperty<>(defaultStatus);
        if (defaultStatus >= statuses.size()) {
            throw new IllegalArgumentException("Invalid default " + defaultStatus + ": " + statuses);
        }
        TextOrImage sd = statuses.get(defaultStatus);
        toggle = withClass(switch (sd) {
            case TextOrImage.IsText t -> new ToggleButton(t.text());
            case TextOrImage.IsImage i -> {
                ToggleButton b = new ToggleButton();
                b.setGraphic(i.image());
                yield b;
            }}, "multi-toggle", cssClass);
        toggle.setFocusTraversable(false);

        toggle.setOnAction(event -> {
            int next = (state.get() + 1) % statuses.size();
            state.set(next);
            setToggle(statuses.get(next));
            toggle.setSelected(false); // Always unselected UI
        });

        state.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal >= 0 && newVal < statuses.size()) {
                setToggle(statuses.get(newVal));
            }
        });
    }

    private void setToggle(TextOrImage s) {
        switch (s) {
            case TextOrImage.IsImage i -> toggle.setGraphic(i.image());
            case TextOrImage.IsText t -> toggle.setText(t.text());
        }
    }

    public Property<Integer> state() {
        return state;
    }

    public ToggleButton getToggle() {
        return toggle;
    }
}
