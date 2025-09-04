package org.g2fx.g2gui.bridge;

import javafx.beans.property.Property;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.state.Device;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class LocalBridger implements Bridger {
    private final Bridger bridger;
    private final List<PropertyBridge<?,?>> locals = new ArrayList<>();

    public LocalBridger(Bridger bridger) {
        this.bridger = bridger;
    }

    @Override
    public <T> PropertyBridge<T, T> bridge(Property<T> fxProperty, Function<Device, LibProperty<T>> libProperty) {
        PropertyBridge<T, T> bridge = bridger.bridge(fxProperty, libProperty);
        locals.add(bridge);
        return bridge;
    }

    @Override
    public <T, F> PropertyBridge<T, F> bridge(Function<Device, LibProperty<T>> libProperty, FxProperty<F> fxProperty, Iso<T, F> iso) {
        PropertyBridge<T, F> bridge = bridger.bridge(libProperty, fxProperty, iso);
        locals.add(bridge);
        return bridge;
    }

    public List<PropertyBridge<?, ?>> getLocalBridges() {
        return locals;
    }
}
