package org.g2fx.g2gui;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.LibProperty.LibPropertyListener;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Bridges a backend Property<T> and a JavaFX Property<T>.
 * Synchronizes changes bidirectionally, with optional thread dispatching.
 * Supports disposal to unregister listeners and allow garbage collection.
 */
public class PropertyBridge<T> implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(PropertyBridge.class.getName());

    private final LibProperty<T> libProperty;
    private final Property<T> fxProperty;
    private final Executor fxExecutor; // For dispatching to FX thread if needed
    private final Executor backendExecutor; // For dispatching to backend thread if needed

    private final AtomicBoolean updatingBackend = new AtomicBoolean(false);
    private final AtomicBoolean updatingFx = new AtomicBoolean(false);

    // Keep references to listeners for removal
    private final LibPropertyListener<T> backendListener;
    private final ChangeListener<T> fxListener;

    private volatile boolean disposed = false;

    public PropertyBridge(LibProperty<T> libProperty,
                          Property<T> fxProperty,
                          Executor fxExecutor,
                          Executor backendExecutor) {
        this.libProperty = libProperty;
        this.fxProperty = fxProperty;
        this.fxExecutor = fxExecutor;
        this.backendExecutor = backendExecutor;

        // Define listeners
        this.backendListener = (oldVal, newVal) -> {
            if (disposed || updatingBackend.get()) return;
            if (!updatingFx.compareAndSet(false,true)) return;
            Runnable updateFx = () -> {
                try {
                    fxProperty.setValue(newVal);
                } finally {
                    updatingFx.set(false);
                }
            };
            if (!Platform.isFxApplicationThread()) {
                fxExecutor.execute(updateFx);
            } else {
                updateFx.run();
            }
        };

        this.fxListener = (obs, oldVal, newVal) -> {
            if (disposed || updatingFx.get()) return;
            if (!updatingBackend.compareAndSet(false,true)) return;
            backendExecutor.execute(() -> {
                try {
                    libProperty.set(newVal);
                } finally {
                    updatingBackend.set(false);
                }
            });
        };

        wireUp();
    }

    private void wireUp() {
        libProperty.addListener(backendListener);
        fxProperty.addListener(fxListener);

        // Initial sync: set FX property to backend value.
        // If you want to also fire listeners, do so manually after bridge creation.
        if (!Platform.isFxApplicationThread() && fxExecutor != null) {
            fxExecutor.execute(() -> fxProperty.setValue(libProperty.get()));
        } else {
            fxProperty.setValue(libProperty.get());
        }
    }

    /**
     * Unregister listeners to break reference cycles and allow garbage collection.
     * After disposal, the bridge will no longer synchronize properties.
     */
    @Override
    public void close() {
        dispose();
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;

        try {
            libProperty.removeListener(backendListener);
        } catch (Exception e) {
            logger.warning("Failed to remove backend listener: " + e.getMessage());
        }

        try {
            fxProperty.removeListener(fxListener);
        } catch (Exception e) {
            logger.warning("Failed to remove FX listener: " + e.getMessage());
        }
    }
}
