package org.g2fx.g2gui.bridge;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.function.Consumer;

public abstract class FxProperty<T> {

    public static class SimpleFxProperty<T> extends FxProperty<T> {

        private Property<T> p;
        public SimpleFxProperty(Property<T> p) {
            this(p,null);
        }

        /**
         * @param p target property
         * @param changing boolean prop for commit undos, can be null.
         */
        public SimpleFxProperty(Property<T> p, ObservableValue<Boolean> changing) {
            super(p,changing);
            this.p = p;
        }
        @Override public void setValue(T value) { p.setValue(value); }
    }

    public static <T> FxProperty<T> adaptReadOnly(ObservableValue<T> readProperty, Consumer<T> setter) {
        return new FxProperty<>(readProperty) {
            @Override public void setValue(T value) {
                setter.accept(value);
            }
        };
    }

    private ChangeListener<T> valueListener;
    private final ObservableValue<Boolean> changing;
    protected final ObservableValue<T> observable;
    private String name;

    public FxProperty(ObservableValue<T> observable) {
        this(observable,null); // never changing
    }

    public FxProperty(ObservableValue<T> observable, ObservableValue<Boolean> changingArg) {
        this.observable = observable;
        if (observable instanceof Property<T> po) {
            name = po.getName();
        } else {
            name = observable.toString();
        }
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

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "FxProperty: " + name;
    }
}
