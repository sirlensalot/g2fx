package org.g2fx.g2lib.state;

import org.g2fx.g2lib.util.Util;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface LifecycleListener<D> {
    void onLifecycleInit(D d) throws Exception;

    void onLifecycleDispose(D d) throws Exception;

    public static final Logger log = Util.getLogger(LifecycleListener.class);

    static <T> void notifyLifecycleInit(List<LifecycleListener<T>> listeners, T perf) {
        for (LifecycleListener<T> l : listeners) {
            notifyLifecycleInit(l, perf);
        }
    }

    static <T> void notifyLifecycleInit(LifecycleListener<T> l, T perf) {
        try {
            l.onLifecycleInit(perf);
        } catch (Exception e) {
            log.log(Level.SEVERE,"Error in lifecycle init listener",e);
        }
    }

    static <T> void notifyLifecycleDispose(List<LifecycleListener<T>> listeners, T perf) {
        for (LifecycleListener<T> l : listeners) {
            notifyLifecycleDispose(l, perf);
        }
    }

    static <T> void notifyLifecycleDispose(LifecycleListener<T> l, T perf) {
        try {
            l.onLifecycleDispose(perf);
        } catch (Exception e) {
            log.log(Level.SEVERE,"Error in lifecycle dispose listener",e);
        }
    }

    static <T> LifecycleListener<T> noopListener() {
        return new LifecycleListener<T>() {
            @Override public void onLifecycleInit(T t) throws Exception {}
            @Override public void onLifecycleDispose(T t) throws Exception {}};
    }
}
