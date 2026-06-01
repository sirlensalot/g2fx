package org.g2fx.g2gui.bridge;

import javafx.beans.value.ChangeListener;
import org.g2fx.g2gui.Undos;
import org.g2fx.g2lib.device.LibExecutor;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.LibProperty.LibPropertyListener;

import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Bi-directional listener/publisher "bridging" a backend LibProperty
 * that is updated on a backend executor thread, to a front-end javaFX Property
 * that is updated on the FX thread.
 * <p>
 * Bridges are created along with an FX property and a supplier of a backend lib property.
 * In general the lifecycle matches the associated FX property.
 * <p>
 * LIFECYCLE:
 * <p>
 * Constructor is called on FX thread, registers FX listener,
 * and makes a "finalizer" task to register the lib listener on the lib thread.
 * <p>
 * Initialization (or "Activation"), on the lib thread, uses the supplier to create the property and register the lib
 * listener. It also returns a Runnable FX task to supply an initial update to the FX property. The active flag is
 * set to true.
 * <p>
 * Disposal (or "Deactivation"), on the lib thread, unregisters the listener, so that the backend
 * property can go out of scope, and sets active flag to false.
 * <p>
 * Since many bridges are long-lived (the life of the UI), Activation/Deactivation must be supported, that is,
 * the lib property activation must be repeatable with fresh backend sources, so a patch can be changed, etc.
 * Properly managed, the bridge should be fully GC-able after disposal, alongside the associated FX resource.
 * <p>
 * Reentrancy is guarded by thread-local booleans, so that setting a property does not cause the listener
 * to re-propagate the update. A volatile active flag ensures updates are not
 * dispatched before initialization or after disposal.
 * <p>
 *
 */
public class PropertyBridge<D,T,F> {


    private static final Logger log =
            Logger.getLogger(PropertyBridge.class.getName());

    /**
     * Pure iso used by both threads.
     */
    private final Iso<T, F> iso;

    /**
     * Not final: lib properties are created as part of activation.
     */
    private LibProperty<T> libProperty;
    private final LibPropertyListener<T> libListener;
    /**
     * lib update lock. All reads/writes on lib thread.
     */
    private boolean updatingLib = false;

    private final FxProperty<F> fxProperty;
    private final ChangeListener<F> fxListener;
    /**
     * fx update lock. All reads/writes on fx thread.
     */
    private boolean updatingFx = false;

    private final Function<D,Runnable> initFinalizer;

    /**
     * active flag read on both threads; inactive before
     * init finalization, and after disposal.
     */
    private volatile boolean active = false;

    /**
     * FX THREAD ONLY, construct bridge and register FX listeners; set
     * finalizer to register lib listener and set active.
     * @param libPropertyBuilder called in init finalizer to build/locate lib property endpoint
     * @param libExecutor lib-side executor
     * @param fxProperty FX property endpoint
     * @param fxExecutor FX thread executor
     * @param iso Pure functions to convert values from one side to other. MUST BE PURE since shared on both threads.
     * @param undos undos collected in FX listener.
     */
    public PropertyBridge(Function<D,LibProperty<T>> libPropertyBuilder,
                          LibExecutor libExecutor,
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
            // getting here means "real" UI update, not from backend
            // only test for change is here ONLY for undos!
            // all updates are sent to backend (for e.g. knob twiddling)
            if (newVal != null && !newVal.equals(oldVal) && !fxProperty.isChanging()) {
                undos.push(fxProperty,oldVal,newVal);
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

    /**
     * LIB THREAD ONLY - registers lib listener and activates bridge,
     * returning task to perform initial publish of lib value to FX side.
     * @param d for lib property builder/locator.
     * @return task to publish initial lib value.
     */
    public Runnable finalizeInit(D d) {
        // lib thread
        return initFinalizer.apply(d);
    }

    /**
     * FX update runnable factory, used both in dispatch and in init finalization
     * for initial update.
     */
    private Runnable mkFxUpdate(T newVal) {
        return () -> {
            // on fx thread: lock fx update
            updatingFx = true;
            try {
                fxProperty.setValue(iso.to(newVal));
            } finally {
                // unlock fx update
                updatingFx = false;
            }
        };
    }


    /**
     * LIB THREAD ONLY - unregister lib listener and deactivate.
     * @return Runnable to unregister FX property listener on fx thread.
     */
    public void dispose() {

        if (!active) return;
        active = false;

        try {
            libProperty.removeListener(libListener);
        } catch (Exception e) {
            log.warning("Failed to remove backend listener: " + e.getMessage());
        }

//        return () -> {
//            try {
//                fxProperty.removeListener(fxListener);
//            } catch (Exception e) {
//                log.warning("Failed to remove FX listener: " + e.getMessage());
//            }
//        };

    }

    @Override
    public String toString() {
        return "PropertyBridge: " + fxProperty.getObservable().toString();
    }
}
