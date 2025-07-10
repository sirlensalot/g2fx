package org.g2fx.g2gui;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.LibProperty.LibPropertyListener;
import org.g2fx.g2lib.state.Device;

import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Implements bi-directional listeners for a LibProperty and a javaFX Property,
 * where property change events are propagated to the opposite property on
 * the appropriate thread, with locks to prevent reentrancy.
 */
public class PropertyBridge<T> {

    private static final Logger log =
            Logger.getLogger(PropertyBridge.class.getName());

    private LibProperty<T> libProperty;
    private final LibPropertyListener<T> libListener;
    /**
     * lib update lock. All reads/writes on lib thread.
     */
    private boolean updatingLib = false;

    private final Property<T> fxProperty;
    private final ChangeListener<T> fxListener;
    /**
     * fx update lock. All reads/writes on fx thread.
     */
    private boolean updatingFx = false;

    private final Function<Device,Runnable> initFinalizer;

    /**
     * active flag read on both threads. also guards libProperty being not null.
     */
    private volatile boolean active = false;

    /**
     * Constructor called on fx thread.
     */
    public PropertyBridge(Function<Device,LibProperty<T>> libPropertyBuilder,
                          Executor libExecutor,
                          Property<T> fxProperty,
                          Executor fxExecutor) {

        this.fxProperty = fxProperty;

        libListener = (oldVal, newVal) -> {
            // on lib thread, so lock read is safe
            if (!active || updatingLib) return;
            fxExecutor.execute(mkFxUpdate(newVal));
        };

        fxListener = (obs, oldVal, newVal) -> {
            // on fx thread, so lock read is safe
            if (!active || updatingFx) return;
            libExecutor.execute(() -> {
                // on lib thread: lock lib updates
                updatingLib = true;
                try {
                    libProperty.set(newVal);
                } finally {
                    // unlock lib updates
                    updatingLib = false;
                }
            });
        };

        fxProperty.addListener(fxListener);

        initFinalizer = d -> {
            libProperty = libPropertyBuilder.apply(d);
            active = true;
            libProperty.addListener(libListener);
            return mkFxUpdate(libProperty.get());
        };

    }

    public Runnable finalizeInit(Device d) {
        return initFinalizer.apply(d);
    }

    private Runnable mkFxUpdate(T newVal) {
        return () -> {
            // on fx thread: lock fx update
            updatingFx = true;
            try {
                fxProperty.setValue(newVal);
            } finally {
                // unlock fx update
                updatingFx = false;
            }
        };
    }



    public void dispose() {

        if (!active) return;
        active = false;

        try {
            libProperty.removeListener(libListener);
        } catch (Exception e) {
            log.warning("Failed to remove backend listener: " + e.getMessage());
        }

        try {
            //TODO this should be on fx thread
            fxProperty.removeListener(fxListener);
        } catch (Exception e) {
            log.warning("Failed to remove FX listener: " + e.getMessage());
        }
    }

}
