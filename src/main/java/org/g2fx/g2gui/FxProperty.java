package org.g2fx.g2gui;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.function.Consumer;

public abstract class FxProperty<T> {

    private ChangeListener<T> valueListener;

    public static class SimpleFxProperty<T> extends FxProperty<T> {

        private Property<T> p;
        public SimpleFxProperty(Property<T> p) {
            this(p,null);
        }
        public SimpleFxProperty(Property<T> p, ObservableValue<Boolean> changing) {
            super(p,changing);
            this.p = p;
        }
        @Override public void setValue(T value) { p.setValue(value); }
    }

    private final ObservableValue<Boolean> changing;
    protected final ObservableValue<T> observable;

    public FxProperty(ObservableValue<T> observable) {
        this(observable,null); // never changing
    }

    public static <T> FxProperty<T> adaptReadOnly(ObservableValue<T> readProperty, Consumer<T> setter) {
        return new FxProperty<>(readProperty) {
            @Override public void setValue(T value) {
                setter.accept(value);
            }
        };
    }

    public FxProperty(ObservableValue<T> observable, ObservableValue<Boolean> changingArg) {
        this.observable = observable;
        this.changing = changingArg == null ? new SimpleBooleanProperty(false) : changingArg;
        changing.addListener(new ChangeListener<>() {
            private T changeStartValue;
            @Override
            public void changed(ObservableValue<? extends Boolean> co, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    changeStartValue = observable.getValue();
                } else {
                    if (valueListener != null && changeStartValue != null) {
                        valueListener.changed(observable,changeStartValue,observable.getValue());
                    }
                }
            }
        });
    }

    public void addListener(ChangeListener<T> listener) {
        valueListener = listener;
        observable.addListener(listener);
    }

    public void removeListener(ChangeListener<T> listener) {
        valueListener = null;
        observable.removeListener(listener);
    }

    public ObservableValue<T> getObservable() {
        return observable;
    }

    public boolean isChanging() {
        return changing.getValue();
    }

    public abstract void setValue(T value);
}
