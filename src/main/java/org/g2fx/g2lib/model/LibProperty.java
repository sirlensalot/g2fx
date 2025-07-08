package org.g2fx.g2lib.model;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A thread-safe, generic property for backend use.
 * 
 * <p><b>Threading note:</b> All access to the value (get/set) must occur on the backend (USB) thread.
 * Listener registration/removal is thread-safe via CopyOnWriteArraySet.</p>
 *
 * @param <T> The type of the property value.
 */
public class LibProperty<T> {
    /**
     * Listener interface for property changes.
     */
    @FunctionalInterface
    public interface LibPropertyListener<T> {
        void propertyChanged(T oldValue, T newValue);
    }

    // All access must be on the backend thread!
    private T value;
    private final CopyOnWriteArraySet<LibPropertyListener<T>> listeners = new CopyOnWriteArraySet<>();

    public LibProperty(T initialValue) {
        this.value = initialValue;
    }

    /**
     * Gets the current value.
     */
    public T get() {
        return value;
    }

    /**
     * Sets the value and notifies listeners if the value has changed.
     */
    public void set(T newValue) {
        T oldValue = this.value;
        if (!Objects.equals(oldValue, newValue)) {
            this.value = newValue;
            notifyListeners(oldValue, newValue);
        }
    }

    /**
     * Adds a listener to be notified when the value changes.
     */
    public void addListener(LibPropertyListener<T> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     */
    public void removeListener(LibPropertyListener<T> listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners of a value change.
     */
    private void notifyListeners(T oldValue, T newValue) {
        for (LibPropertyListener<T> listener : listeners) {
            try {
                listener.propertyChanged(oldValue, newValue);
            } catch (Exception e) {
                // Optionally log or handle listener exceptions
                e.printStackTrace();
            }
        }
    }
}
