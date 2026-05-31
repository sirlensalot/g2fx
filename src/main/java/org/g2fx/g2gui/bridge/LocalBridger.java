package org.g2fx.g2gui.bridge;

import javafx.beans.property.Property;
import org.g2fx.g2lib.model.LibProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class LocalBridger<D> implements Bridger<D> {
    private final Bridger<D> bridger;
    private final List<PropertyBridge<D,?,?>> locals = new ArrayList<>();

    public LocalBridger(Bridger<D> bridger) {
        this.bridger = bridger;
    }

    @Override
    public <T> PropertyBridge<D, T, T> bridge(Property<T> fxProperty, Function<D, LibProperty<T>> libProperty) {
        PropertyBridge<D, T, T> bridge = bridger.bridge(fxProperty, libProperty);
        locals.add(bridge);
        return bridge;
    }

    @Override
    public <T, F> PropertyBridge<D, T, F> bridge(Function<D, LibProperty<T>> libProperty, FxProperty<F> fxProperty, Iso<T, F> iso) {
        PropertyBridge<D, T, F> bridge = bridger.bridge(libProperty, fxProperty, iso);
        locals.add(bridge);
        return bridge;
    }

    public List<PropertyBridge<D, ?, ?>> getLocalBridges() {
        return locals;
    }
}
