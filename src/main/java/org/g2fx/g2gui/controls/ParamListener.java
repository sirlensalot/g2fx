package org.g2fx.g2gui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.g2fx.g2gui.panel.ModulePane;
import org.g2fx.g2gui.ui.ControlDependencies;
import org.g2fx.g2gui.ui.UIElement;
import org.g2fx.g2gui.ui.UIElements;
import org.g2fx.g2gui.ui.UIParamControl;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ParamListener {

    public enum IntOrBool {
        Int, Bool
    }
    public interface ParamVal {
        int getInt();
        boolean getBool();
    }
    static class IntVal implements ParamVal {
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
    static class BoolVal implements ParamVal {
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


    public record ParamSpec(IntOrBool type,Object id) {}
    public static ParamSpec pInt(Object id) { return new ParamSpec(IntOrBool.Int,id); }
    public static ParamSpec pBool(Object id) { return new ParamSpec(IntOrBool.Bool,id); }


    public static class ParamVals {
        private final Map<Object,ParamVal> vs;
        private final Map<Object,IntOrBool> types;

        public ParamVals(ParamSpec... specs) {
            this.vs = new HashMap<>(specs.length);
            this.types = new HashMap<>(specs.length);
        }
        public void put(ParamSpec spec,ParamVal val) {
            vs.put(spec.id,val);
            types.put(spec.id,spec.type);
        }

        public int getInt(Object id) {
            return get(id,IntOrBool.Int).getInt();
        }

        public boolean getBool(Object id) {
            return get(id,IntOrBool.Bool).getBool();
        }

        private ParamVal get(Object id, IntOrBool type) {
            ParamVal v = vs.get(id);
            if (v == null) {
                throw new IllegalArgumentException("ParamVals.get: invalid key: " + id + ", " + vs);
            }
            if (type != types.get(id)) {
                throw new IllegalArgumentException("ParamVals.get: invalid type: " + id + ", expected " + types.get(id));
            }
            return v;
        }
    }

    private final Map<Integer, Property<Integer>> intProps = new TreeMap<>();
    private final Map<Integer,ObservableValue<Integer>> modeProps = new TreeMap<>();
    private final Map<Integer,Property<Boolean>> boolProps = new TreeMap<>();
    private final ModuleType type;
    private final ModulePane modulePane;

    public ParamListener(ModuleType type, ModulePane modulePane) {
        this.type = type;
        this.modulePane = modulePane;
    }


    public void build(ControlDependencies deps, Consumer<ParamVals> f, ParamSpec... depTypes) {
        ParamVals vs = new ParamVals(depTypes);
        Runnable listener = () -> f.accept(vs);
        for (int i = 0 ; i < depTypes.length ; i++) {
            vs.put(depTypes[i],depTypes[i].type == IntOrBool.Int ? new IntVal(resolveDepParam(deps, i), listener) :
                    new BoolVal(resolveBoolDepParam(deps, i), listener));
        }
    }


    public void listenInt2(ControlDependencies deps,
                           BiConsumer<Integer, Integer> f) {
        ObservableValue<Integer> p0 = resolveDepParam(deps, 0);
        ObservableValue<Integer> p1 = resolveDepParam(deps, 1);
        ChangeListener<Integer> listener = (c, o, n) ->
                f.accept(p0.getValue(), p1.getValue());
        p0.addListener(listener);
        p1.addListener(listener);
    }

    public void listenInt3(ControlDependencies deps,
                           Util.TriConsumer<Integer, Integer, Integer> f) {
        ObservableValue<Integer> p0 = resolveDepParam(deps, 0);
        ObservableValue<Integer> p1 = resolveDepParam(deps, 1);
        ObservableValue<Integer> p2 = resolveDepParam(deps, 2);
        ChangeListener<Integer> listener = (c,o,n) ->
                f.accept(p0.getValue(), p1.getValue(), p2.getValue());
        p0.addListener(listener);
        p1.addListener(listener);
        p2.addListener(listener);
    }

    public ObservableValue<Integer> resolveDepParam(ControlDependencies c, int ix) {
        ControlDependencies.Dependency d = c.Dependencies().get(ix);
        boolean isParam = d.type() == UIElements.DepType.Param;
        IndexParam ip = isParam ?
                resolveParam(d.index()) :
                resolveMode(d.index());
        ObservableValue<Integer> p = (isParam?intProps:modeProps).get(ip.index());
        if (p == null) { throw new IllegalArgumentException("resoveDepParam: no property found " + ip); }
        return p;
    }

    public Property<Boolean> resolveBoolDepParam(ControlDependencies c, int ix) {
        IndexParam ip = resolveParam(c.Dependencies().get(ix).index());
        Property<Boolean> p = boolProps.get(ip.index());
        if (p == null) { throw new IllegalArgumentException("resoveBoolDepParam: no property found " + ip); }
        return p;
    }

    public void addModeProp(IndexParam ip, ObservableValue<Integer> prop) {
        modeProps.put(ip.index(),prop);
    }


    public Property<Boolean> getBoolProp(int idx) { return boolProps.get(idx); }
    public Property<Integer> getIntProp(int idx) { return intProps.get(idx); }

    public void addIntProp(IndexParam ip, Property<Integer> property) {
        intProps.put(ip.index(),property);
    }

    public void addBoolProp(IndexParam ip, BooleanProperty selectedProperty) {
        boolProps.put(ip.index(),selectedProperty);
    }


    public IndexParam resolveParam(UIParamControl uc) {
        int cr = uc.CodeRef();
        if (cr > type.getParams().size()) { throw new IllegalArgumentException("resolveParam: bad index: " + uc); }
        NamedParam p = type.getParams().get(cr);
        return new IndexParam(p,cr,modulePane.toString());
    }

    public IndexParam resolveParam(int index) {
        if (index > type.getParams().size()) {
            throw new IllegalArgumentException("Bad param index: " + index + ", module: " + this);
        }
        return new IndexParam(type.getParams().get(index),index,modulePane.toString());
    }


    public IndexParam resolveMode(int index) {
        if (index > type.modes.size()) {
            throw new IllegalArgumentException("Bad param index: " + index + ", module: " + this);
        }
        return new IndexParam(type.modes.get(index),index,modulePane.toString());
    }

    public Node empty(UIElement e, String msg) {
        System.out.println(msg + " TODO: " + e + ": " + modulePane);
        return new Pane();
    }

}
