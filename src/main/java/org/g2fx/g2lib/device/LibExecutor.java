package org.g2fx.g2lib.device;

import org.g2fx.g2lib.state.Performance;

import java.util.concurrent.Callable;

/**
 * Executor API for running tasks on backend/"lib".
 */
public interface LibExecutor {

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    interface ThrowingFunction<A,R> {
        R invoke(A a) throws Exception;
    }

    <T>T withCurrentPerf(ThrowingFunction<Performance, T> f) throws Exception;

    <V> V invoke(Callable<V> c);

    <V> V invokeWithCurrent(Devices.ThrowingFunction<Device, V> f);

    <V> V invokeWithCurrentPerf(ThrowingFunction<Performance, V> f);

    void runWithCurrentPerf(ThrowingConsumer<Performance> f);

    void runWithCurrent(Devices.ThrowingConsumer<Device> f);

    void execute(Devices.ThrowingRunnable r);
}
