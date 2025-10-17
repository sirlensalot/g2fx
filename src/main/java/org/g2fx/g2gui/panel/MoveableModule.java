package org.g2fx.g2gui.panel;

import javafx.beans.property.Property;
import org.g2fx.g2lib.state.Coords;

public interface MoveableModule {
    boolean isSelected();

    Property<Coords> coords();

    int getHeight();

}
