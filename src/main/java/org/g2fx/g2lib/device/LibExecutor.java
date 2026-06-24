package org.g2fx.g2lib.device;

import org.g2fx.g2lib.util.Util;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Executor API for running tasks on backend/"lib".
 */
public interface LibExecutor<P> {

    <V> V invoke(Callable<V> c);

    <V> V invokeWithCurrent(Util.ThrowingFunction<P, V> f);

    void runWithCurrent(Util.ThrowingConsumer<P> f);

    void execute(Util.ThrowingRunnable r);

    static <P,Q> LibExecutor<Q> adapt(LibExecutor<P> executor, Function<P,Q> adapter) {
        return new LibExecutor<Q>() {
            @Override
            public <V> V invoke(Callable<V> c) {
                return executor.invoke(c);
            }

            @Override
            public <V> V invokeWithCurrent(Util.ThrowingFunction<Q, V> f) {
                return executor.invokeWithCurrent(p -> f.invoke(adapter.apply(p)));
            }

            @Override
            public void runWithCurrent(Util.ThrowingConsumer<Q> f) {
                executor.runWithCurrent(p -> f.accept(adapter.apply(p)));
            }

            @Override
            public void execute(Util.ThrowingRunnable r) {
                executor.execute(r);
            }
        };
    }
}
