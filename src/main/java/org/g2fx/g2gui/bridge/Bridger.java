package org.g2fx.g2gui.bridge;

import javafx.beans.property.Property;
import org.g2fx.g2lib.model.LibProperty;

import java.util.function.Function;

public interface Bridger<D> {
    <T> PropertyBridge<D, T, T> bridge(Property<T> fxProperty,
                                    Function<D, LibProperty<T>> libProperty);

    <T,F> PropertyBridge<D, T, F> bridge(Function<D, LibProperty<T>> libProperty,
                                      FxProperty<F> fxProperty,
                                      Iso<T, F> iso);
}
