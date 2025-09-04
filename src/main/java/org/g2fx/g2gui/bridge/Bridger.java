package org.g2fx.g2gui.bridge;

import javafx.beans.property.Property;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.state.Device;

import java.util.function.Function;

public interface Bridger {
    <T> PropertyBridge<T, T> bridge(Property<T> fxProperty,
                                    Function<Device, LibProperty<T>> libProperty);

    <T,F> PropertyBridge<T, F> bridge(Function<Device, LibProperty<T>> libProperty,
                                      FxProperty<F> fxProperty,
                                      Iso<T, F> iso);
}
