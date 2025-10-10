package org.g2fx.g2lib.model;

import org.g2fx.g2lib.protocol.FieldEnum;
import org.g2fx.g2lib.protocol.FieldValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;


public class LibProperty<T> {

    @FunctionalInterface
    public interface LibPropertyListener<T> {
        void propertyChanged(T oldValue, T newValue) throws Exception;
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

    public T get() {
        return getterSetter.get();
    }

    public void set(T newValue) {
        T old = get();
        getterSetter.set(newValue);
        if (!Objects.equals(old, newValue)) {
            notifyListeners(old, newValue);
        }
    }

    public void addListener(LibPropertyListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(LibPropertyListener<T> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(T oldValue, T newValue) {
        for (LibPropertyListener<T> listener : listeners) {
            try {
                listener.propertyChanged(oldValue, newValue);
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error notifying lib listener",e);
            }
        }
    }

    public void refresh() {
        notifyListeners(get(),get());
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

    @FunctionalInterface
    public interface FieldValuesChangeListener {
        void changed(FieldValues fvs) throws Exception;
    }

    public static class FieldValuesLibProperties {

        private FieldValues fvs;
        private final List<LibProperty<?>> properties = new ArrayList<>();
        private boolean refreshing = false;
        private final FieldValuesChangeListener updater;

        public FieldValuesLibProperties(FieldValuesChangeListener updater) {
            this.updater = updater;
        }

        public void update(FieldValues fvs) {
            this.fvs = fvs;
            refreshing = true;
            try {
                properties.forEach(LibProperty::refresh);
            } finally {
                refreshing = false;
            }
        }

        public LibProperty<Integer> intFieldProperty(FieldEnum f) {
            return intFieldProperty(f, true);
        }
        public LibProperty<Integer> intFieldProperty(FieldEnum f, boolean register) {
            LibProperty<Integer> prop = new LibProperty<>(new LibPropertyGetterSetter<>() {
                @Override
                public Integer get() {
                    return f.intValue(fvs);
                }

                @Override
                public void set(Integer newValue) {
                    fvs.update(f.value(newValue));
                }
            });
            return register ? register(prop) : prop;
        }

        public LibProperty<String> stringFieldProperty(FieldEnum f) {
            return stringFieldProperty(f,true);
        }
        public LibProperty<String> stringFieldProperty(FieldEnum f,boolean register) {
            LibProperty<String> prop = new LibProperty<>(new LibPropertyGetterSetter<>() {
                @Override
                public String get() {
                    return f.stringValue(fvs);
                }

                @Override
                public void set(String newValue) {
                    fvs.update(f.value(newValue));
                }
            });
            return register ? register(prop) : prop;
        }
        public LibProperty<Boolean> booleanFieldProperty(FieldEnum f) {
            return booleanFieldProperty(f,true);
        }

        public LibProperty<Boolean> booleanFieldProperty(FieldEnum f, boolean register) {
            LibProperty<Boolean> prop = new LibProperty<>(new LibPropertyGetterSetter<>() {
                @Override
                public Boolean get() {
                    return f.booleanIntValue(fvs);
                }

                @Override
                public void set(Boolean newValue) {
                    fvs.update(f.value(newValue));
                }
            });
            return register? register(prop) : prop;
        }

        public <T> LibProperty<T> register(LibProperty<T> prop) {
            properties.add(prop);
            prop.addListener((o, n) -> {
                if (refreshing) { return; }
                updater.changed(fvs);
            });
            return prop;
        }
    }
}
