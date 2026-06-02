package org.g2fx.g2lib.state;

public interface LifecycleListener<D> {
    void onLifecycleInit(D d) throws Exception;
    void onLifecycleDispose(D d) throws Exception;
}
