package org.g2fx.g2gui.bridge;

import javafx.beans.property.Property;
import org.g2fx.g2gui.Undos;
import org.g2fx.g2lib.device.LibExecutor;
import org.g2fx.g2lib.model.LibProperty;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class Bridges<D> implements Bridger<D> {

    /**
     * Accessed by both threads. Light benchmarking showed no difference vs ArrayList
     * for PERF_002 which is decently large.
     */
    private final List<PropertyBridge<D,?,?>> bridges = new CopyOnWriteArrayList<>();
    private final LibExecutor libExecutor;
    private final Executor fxQueue;
    private final Undos undos;

    /**
     * On FX thread, top-level ctor.
     */
    public Bridges(LibExecutor libExecutor, Executor fxQueue, Undos undos) {
        this.libExecutor = libExecutor;
        this.fxQueue = fxQueue;
        this.undos = undos;
    }

    /**
     * Lower level ctor, copy services from higher-level bridger.
     */
    public Bridges(Bridges<?> parent) {
        this.libExecutor = parent.libExecutor;
        this.fxQueue = parent.fxQueue;
        this.undos = parent.undos;
    }

    /**
     * Spawn a differently-parameterized bridges instance.
     */
    public <T> Bridges<T> spawn() {
        return new Bridges<>(this);
    }

    @Override
    public <T> PropertyBridge<D, T, T> bridge(Property<T> fxProperty,
                                           Function<D, LibProperty<T>> libProperty) {
        // fx thread
        return bridge(libProperty,
                new FxProperty.SimpleFxProperty<>(fxProperty), Iso.id());
    }

    @Override
    public <T,F> PropertyBridge<D, T, F> bridge(Function<D, LibProperty<T>> libProperty,
                                             FxProperty<F> fxProperty,
                                             Iso<T, F> iso) {
        // fx thread
        PropertyBridge<D, T, F> bridge =
                new PropertyBridge<>(libProperty, libExecutor, fxProperty, fxQueue, iso, undos);
        bridges.add(bridge);
        return bridge;
    }

    public List<Runnable> initialize(D d) {
        // on lib thread
        return bridges.stream().map(b -> b.finalizeInit(d)).toList();
    }

    public List<Runnable> dispose() {
        // on lib thread
        return bridges.stream().map(PropertyBridge::dispose).toList();
    }

    public LibExecutor getDeviceExecutor() {
        return libExecutor;
    }
}
