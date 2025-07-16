package org.g2fx.g2gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public abstract class FxProperty<T> {
    protected final ObservableValue<T> observable;
    protected final Undos undos;
    private final ChangeListener<T> undoListener;

    public FxProperty(ObservableValue<T> observable, Undos undos) {
        this.observable = observable;
        this.undos = undos;

        this.undoListener = (obs, oldVal, newVal) -> {
            // Don't record undo during undo/redo operation, and don't record redundant changes
            if (!undos.isInUndoRedo() && oldVal != null && !oldVal.equals(newVal)) {
                undos.push(new Undos.Undo<>(this, oldVal, newVal));
            }
        };
        observable.addListener(undoListener);
    }

    public void addListener(ChangeListener<T> listener) {
        observable.addListener(listener);
    }

    public void removeListener(ChangeListener<T> listener) {
        observable.removeListener(listener);
    }

    // If ever needed, lets you remove the auto undo listener
    // needed for module controls at least
    public void removeUndoListener() {
        observable.removeListener(undoListener);
    }

    public ObservableValue<T> getObservable() {
        return observable;
    }

    public abstract void setValue(T value);
}
