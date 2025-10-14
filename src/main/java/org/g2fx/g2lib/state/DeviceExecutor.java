package org.g2fx.g2lib.state;

import java.util.concurrent.Callable;

public interface DeviceExecutor {

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

    <V> V invoke(Callable<V> c);

    <V> V invokeWithCurrent(Devices.ThrowingFunction<Device, V> f);

    void runWithCurrent(Devices.ThrowingConsumer<Device> f);

    void execute(Devices.ThrowingRunnable r);
}
