package org.g2fx.g2lib.device;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Executor API for running tasks on backend/"lib".
 */
public interface LibExecutor<P> {

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    interface ThrowingBiConsumer<T,U> {
        void accept(T t,U u) throws Exception;
    }

    @FunctionalInterface
    interface ThrowingFunction<A,R> {
        R invoke(A a) throws Exception;
    }

    <V> V invoke(Callable<V> c);

    <V> V invokeWithCurrent(ThrowingFunction<P, V> f);

    void runWithCurrent(ThrowingConsumer<P> f);

    void execute(Devices.ThrowingRunnable r);

    static <P,Q> LibExecutor<Q> adapt(LibExecutor<P> executor, Function<P,Q> adapter) {
        return new LibExecutor<Q>() {
            @Override
            public <V> V invoke(Callable<V> c) {
                return executor.invoke(c);
            }

            @Override
            public <V> V invokeWithCurrent(ThrowingFunction<Q, V> f) {
                return executor.invokeWithCurrent(p -> f.invoke(adapter.apply(p)));
            }

            @Override
            public void runWithCurrent(ThrowingConsumer<Q> f) {
                executor.runWithCurrent(p -> f.accept(adapter.apply(p)));
            }

            @Override
            public void execute(ThrowingRunnable r) {
                executor.execute(r);
            }
        };
    }
}
