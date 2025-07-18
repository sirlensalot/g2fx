package org.g2fx.g2gui;

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
public class PropertyBridge<T,F> {


    public interface Iso<A,B> {
        B to(A a);
        A from(B b);
    }

    public static <I> Iso<I,I> id() {
        return new Iso<>() {
            @Override public I to(I i) { return i; }
            @Override public I from(I i) { return i; }
        };
    }

    private static final Logger log =
            Logger.getLogger(PropertyBridge.class.getName());

    private final Iso<T, F> iso;

    private LibProperty<T> libProperty;
    private final LibPropertyListener<T> libListener;
    /**
     * lib update lock. All reads/writes on lib thread.
     */
    private static boolean updatingLib = false;

    private final FxProperty<F> fxProperty;
    private final ChangeListener<F> fxListener;
    /**
     * fx update lock. All reads/writes on fx thread.
     */
    private static boolean updatingFx = false;

    private final Function<Device,Runnable> initFinalizer;

    /**
     * active flag read on both threads. also guards libProperty being not null.
     */
    private volatile boolean active = false;

    public PropertyBridge(Function<Device,LibProperty<T>> libPropertyBuilder,
                          Executor libExecutor,
                          FxProperty<F> fxProperty,
                          Executor fxExecutor,
                          Iso<T,F> iso,
                          Undos undos) {

        this.fxProperty = fxProperty;
        this.iso = iso;

        libListener = (oldVal, newVal) -> {
            // on lib thread, so lock read is safe
            if (!active || updatingLib) return;
            fxExecutor.execute(mkFxUpdate(newVal));
        };

        fxListener = (obs, oldVal, newVal) -> {
            // on fx thread, so lock read is safe
            if (!active || updatingFx) return;
            //getting here means "real" UI update, not from backend
            if (newVal != null && !newVal.equals(oldVal) && !fxProperty.isChanging()) {
                undos.push(new Undos.Undo<F>(fxProperty,oldVal,newVal)); //TODO refactor undo creation into Undos::undo
            }
            libExecutor.execute(() -> {
                // on lib thread: lock lib updates
                updatingLib = true;
                try {
                    libProperty.set(iso.from(newVal));
                } finally {
                    // unlock lib updates
                    updatingLib = false;
                }
            });
        };

        this.fxProperty.addListener(fxListener);

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
                setFxValue(iso.to(newVal));
            } finally {
                // unlock fx update
                updatingFx = false;
            }
        };
    }

    protected void setFxValue(F fv) {
        fxProperty.setValue(fv);
    }


    public Runnable dispose() {

        if (!active) return () -> {};
        active = false;

        try {
            libProperty.removeListener(libListener);
        } catch (Exception e) {
            log.warning("Failed to remove backend listener: " + e.getMessage());
        }

        return () -> {
            try {
                //TODO this should be on fx thread
                fxProperty.removeListener(fxListener);
            } catch (Exception e) {
                log.warning("Failed to remove FX listener: " + e.getMessage());
            }
        };

    }

}
