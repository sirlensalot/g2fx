package org.g2fx.g2gui.panel;

import javafx.beans.property.Property;
import org.g2fx.g2lib.state.Coords;

public interface MoveableModule {
    boolean isSelected();

    Property<Coords> coords();

    int getHeight();

    default int getRow() { return coords().getValue().row(); }
    default int getColumn() { return coords().getValue().column(); }
    default int getBottomEdge() { return getRow() + getHeight(); }
    default void setRow(int row) { coords().setValue(coords().getValue().setRow(row)); }
    default void incRow(int inc) { coords().setValue(coords().getValue().incRow(inc));}
}
