package org.g2fx.g2gui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

/**
 * Make a momentary Button present a ToggleButton "selected" property
 * that will arm/disarm the button, and provide a read-write property
 * for binding.
 * Subclassed because `arm` and `disarm` have protected access.
 */
public class MomentaryButton extends Button {

    private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

    public MomentaryButton(String text, ImageView image) {
        super(text,image);

        armedProperty().addListener((c, o, n) ->
                selectedProperty.set(n));

        selectedProperty.addListener((c, o, n) -> {
            if (n) arm();
            else disarm();
        });
    }

    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }
}
