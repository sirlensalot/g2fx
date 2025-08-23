package org.g2fx.g2gui.controls;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ToggleButton;

import java.util.List;

import static org.g2fx.g2gui.FXUtil.withClass;

public class MultiStateToggle {

    private final SimpleObjectProperty<Integer> state;
    private final ToggleButton toggle;

    public MultiStateToggle(List<String> statuses, int defaultStatus, String cssClass) {
        state = new SimpleObjectProperty<>(defaultStatus);
        if (defaultStatus >= statuses.size()) {
            throw new IllegalArgumentException("Invalid default " + defaultStatus + ": " + statuses);
        }
        toggle = withClass(new ToggleButton(statuses.get(defaultStatus)),"multi-toggle", cssClass);
        toggle.setFocusTraversable(false);

        toggle.setOnAction(event -> {
            int next = (state.get() + 1) % statuses.size();
            state.set(next);
            toggle.setText(statuses.get(next));
            toggle.setSelected(false); // Always unselected UI
        });

        state.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal >= 0 && newVal < statuses.size())
                toggle.setText(statuses.get(newVal));
        });
    }

    public Property<Integer> state() {
        return state;
    }

    public ToggleButton getToggle() {
        return toggle;
    }
}
