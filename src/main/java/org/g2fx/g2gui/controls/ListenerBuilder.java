package org.g2fx.g2gui.controls;

import javafx.beans.value.ObservableValue;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.ui.ControlDependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface ListenerBuilder {

    enum IntOrBool {
        Int, Bool;
    }
    interface ParamVal {
        int getInt();
        boolean getBool();
    }
    class IntVal implements ParamVal {
        private final ObservableValue<Integer> val;
        private IntVal(ObservableValue<Integer> val, Runnable listener) {
            this.val = val;
            val.addListener((c,o,n) -> listener.run());
        }
        @Override
        public int getInt() {
            return val.getValue();
        }
        @Override
        public boolean getBool() {
            return val.getValue()!=0;
        }
    }
    class BoolVal implements ParamVal {
        private final ObservableValue<Boolean> val;
        private BoolVal(ObservableValue<Boolean> val, Runnable listener) {
            this.val = val;
            val.addListener((c,o,n) -> listener.run());
        }
        @Override
        public int getInt() {
            return val.getValue() ? 1 : 0;
        }
        @Override
        public boolean getBool() {
            return val.getValue();
        }
    }

    static void build(Consumer<List<ParamVal>> f, ModulePane mp, ControlDependencies deps, IntOrBool... depTypes) {
        List<ParamVal> vs = new ArrayList<>(depTypes.length);
        Runnable listener = () -> f.accept(vs);
        for (int i = 0 ; i < depTypes.length ; i++) {
            vs.add(depTypes[i] == IntOrBool.Int ? new IntVal(mp.resolveDepParam(deps,i),listener) :
                    new BoolVal(mp.resolveBoolDepParam(deps,i),listener));
        }
    }
}
