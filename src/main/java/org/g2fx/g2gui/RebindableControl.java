package org.g2fx.g2gui;

import javafx.beans.property.Property;

import java.util.function.Function;

public class RebindableControl<T, P> {
    private final Property<P> control;
    private final Function<T, Property<P>> targetProps;
    private Property<P> last;

    public RebindableControl(Property<P> control, Function<T, Property<P>> targetProps) {
        this.control = control;
        this.targetProps = targetProps;
    }

    public void bind(T target) {
        if (last != null) {
            control.unbindBidirectional(last);
        }
        control.bindBidirectional(last = targetProps.apply(target));
    }
}
