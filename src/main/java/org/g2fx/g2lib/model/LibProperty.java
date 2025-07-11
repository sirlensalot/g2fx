package org.g2fx.g2lib.model;

import org.g2fx.g2lib.protocol.FieldEnum;
import org.g2fx.g2lib.protocol.FieldValues;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread-safe, generic property for backend use.
 * 
 * <p><b>Threading note:</b> All access to the value (get/set) must occur on the backend (USB) thread.
 * Listener registration/removal is thread-safe via CopyOnWriteArraySet.</p>
 *
 * @param <T> The type of the property value.
 */
public class LibProperty<T> {

    @FunctionalInterface
    public interface LibPropertyListener<T> {
        void propertyChanged(T oldValue, T newValue);
    }

    public interface LibPropertyGetterSetter<T> {
        T get();
        void set(T newValue);
    }

    private final Logger log = Logger.getLogger(getClass().getName());

    private final LibPropertyGetterSetter<T> getterSetter;

    private final CopyOnWriteArraySet<LibPropertyListener<T>> listeners = new CopyOnWriteArraySet<>();

    public LibProperty(LibPropertyGetterSetter<T> getterSetter) {
        this.getterSetter = getterSetter;
    }

    public LibProperty(T initialValue) {
        this.getterSetter = new LibPropertyGetterSetter<>() {
            private T value;
            @Override
            public T get() {
                return value;
            }

            @Override
            public void set(T newValue) {
                value = newValue;
            }
        };
        getterSetter.set(initialValue);
    }

    /**
     * Gets the current value.
     */
    public T get() {
        return getterSetter.get();
    }

    /**
     * Sets the value and notifies listeners if the value has changed.
     */
    public void set(T newValue) {
        T old = get();
        getterSetter.set(newValue);
        if (!Objects.equals(old, newValue)) {
            notifyListeners(old, newValue);
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
                log.log(Level.SEVERE,"Error notifying lib listener",e);
            }
        }
    }

    public static LibProperty<Integer> intFieldProperty(FieldValues fvs, FieldEnum f) {
        return new LibProperty<>(new LibProperty.LibPropertyGetterSetter<>() {
            @Override
            public Integer get() {
                return f.intValue(fvs);
            }

            @Override
            public void set(Integer newValue) {
                fvs.update(f.value(newValue));
            }
        });
    }

    public static LibProperty<String> stringFieldProperty(FieldValues fvs, FieldEnum f) {
        return new LibProperty<>(new LibPropertyGetterSetter<>() {
            @Override
            public String get() {
                return f.stringValue(fvs);
            }

            @Override
            public void set(String newValue) {
                fvs.update(f.value(newValue));
            }
        });
    }

    public static LibProperty<Boolean> booleanFieldProperty(FieldValues fvs, FieldEnum f) {
        return new LibProperty<>(new LibPropertyGetterSetter<>() {
            @Override
            public Boolean get() {
                return f.booleanIntValue(fvs);
            }

            @Override
            public void set(Boolean newValue) {
                fvs.update(f.value(newValue));
            }
        });
    }
}
