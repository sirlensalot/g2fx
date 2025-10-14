package org.g2fx.g2gui.bridge;

import javafx.beans.property.Property;
import org.g2fx.g2gui.Undos;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.DeviceExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class Bridges implements Bridger {
    
    private final List<PropertyBridge<?,?>> bridges = new ArrayList<>();
    private final DeviceExecutor deviceExecutor;
    private final Executor fxQueue;
    private final Undos undos;

    public Bridges(DeviceExecutor deviceExecutor, Executor fxQueue, Undos undos) {
        this.deviceExecutor = deviceExecutor;
        this.fxQueue = fxQueue;
        this.undos = undos;
    }

    @Override
    public <T> PropertyBridge<T, T> bridge(Property<T> fxProperty,
                                           Function<Device, LibProperty<T>> libProperty) {
        // fx thread
        return bridge(libProperty,
                new FxProperty.SimpleFxProperty<>(fxProperty), Iso.id());
    }

    @Override
    public <T,F> PropertyBridge<T, F> bridge(Function<Device, LibProperty<T>> libProperty,
                                             FxProperty<F> fxProperty,
                                             Iso<T, F> iso) {
        // fx thread
        PropertyBridge<T, F> bridge =
                new PropertyBridge<>(libProperty, deviceExecutor, fxProperty, fxQueue, iso, undos);
        bridges.add(bridge);
        return bridge;
    }

    public List<Runnable> initialize(Device d) {
        // on lib thread
        return bridges.stream().map(b -> b.finalizeInit(d)).toList();
    }

    public List<Runnable> dispose() {
        // on lib thread
        return bridges.stream().map(PropertyBridge::dispose).toList();
    }

    public void remove(List<PropertyBridge<?,?>> bridges) {
        //must be on fx thread, and does NOT dispose b/c all resources are GC'd (test?)
        this.bridges.removeAll(bridges);
    }

    public DeviceExecutor getDeviceExecutor() {
        return deviceExecutor;
    }
}
