package org.g2fx.g2gui;

import javafx.beans.property.Property;
import javafx.scene.control.Toggle;
import org.controlsfx.control.SegmentedButton;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.state.Device;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class Bridges {
    
    private final List<PropertyBridge<?,?>> bridges = new ArrayList<>();
    private final Executor devices;
    private final Executor fxQueue;
    private final Undos undos;

    public Bridges(Executor devices, Executor fxQueue, Undos undos) {
        this.devices = devices;
        this.fxQueue = fxQueue;
        this.undos = undos;
    }

    public <T> PropertyBridge<T, T> bridge(Property<T> fxProperty,
                                           Function<Device, LibProperty<T>> libProperty) {
        return bridge(libProperty,
                new FxProperty.SimpleFxProperty<>(fxProperty),PropertyBridge.id());
    }

    public <T,F> PropertyBridge<T, F> bridge(Function<Device,LibProperty<T>> libProperty,
                                              FxProperty<F> fxProperty,
                                              PropertyBridge.Iso<T,F> iso) {
        PropertyBridge<T, F> bridge =
                new PropertyBridge<>(libProperty, devices, fxProperty, fxQueue, iso, undos);
        bridges.add(bridge);
        return bridge;
    }

    public List<Runnable> initialize(Device d) {
        return bridges.stream().map(b -> b.finalizeInit(d)).toList();
    }

    public List<Runnable> dispose() {
        return bridges.stream().map(PropertyBridge::dispose).toList();
    }


    public void bridgeSegmentedButton(SegmentedButton button, Function<Device, LibProperty<Integer>> libPropBuilder) {
        bridge(libPropBuilder,
                new FxProperty<>(button.getToggleGroup().selectedToggleProperty()) {
                    @Override
                    public void setValue(Toggle value) {
                        value.setSelected(true);
                    }
                },
                new PropertyBridge.Iso<>() {
                    @Override
                    public Toggle to(Integer integer) {
                        return button.getToggleGroup().getToggles().get(integer);
                    }

                    @Override
                    public Integer from(Toggle tab) {
                        return (Integer) tab.getUserData();
                    }
                });
    }
}
