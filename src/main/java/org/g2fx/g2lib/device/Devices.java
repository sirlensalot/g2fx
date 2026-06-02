package org.g2fx.g2lib.device;

import org.g2fx.g2lib.state.LifecycleListener;
import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.usb.DynamicUsbSender;
import org.g2fx.g2lib.usb.Usb;
import org.g2fx.g2lib.usb.UsbService;
import org.g2fx.g2lib.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton service/facade representing G2 devices and USB service and current Performance.
 */
public class Devices implements UsbService.UsbConnectionListener, LibExecutor {

    private static final Logger log = Util.getLogger(Devices.class);

    public List<DeviceListener> listeners = new CopyOnWriteArrayList<>();
    public List<LifecycleListener<Performance>> perfListeners = new CopyOnWriteArrayList<>();

    private final ExecutorService executorService;
    /**
     * Multi-device support is not really a thing yet ...
     */
    private final Map<Integer, Device> devices = new HashMap<>();
    /**
     * "Current" indicates a connected, online device. DeviceListener
     * only services this instance for now.
     */
    private Device currentDevice;

    private final DynamicUsbSender sender;

    private Performance currentPerf;

    public Devices(UsbService usbService) {
        this.executorService = Executors.newSingleThreadExecutor();
        usbService.addListener(this);
        sender = new DynamicUsbSender();
        addListener(sender);
    }


    public void addListener(DeviceListener listener) {
        listeners.add(listener);
    }

    public void addPerfListener(LifecycleListener<Performance> listener) {
        perfListeners.add(listener);
    }

    private void connected(UsbService.UsbDevice ud) {

        if (currentDevice != null) { return; }

        Usb usb = new Usb(ud);
        final Device d = new Device(usb);
        currentDevice = d;
        usb.setThreadsafeDispatcher(msg -> {
            executorService.execute(() -> {
                try {
                    d.dispatch(msg);
                } catch (Exception e) {
                    log.log(Level.SEVERE,"Error in dispatcher",e);
                }
            });
            return true;
        });
        usb.start();
        if (currentPerf != null) {
            disposePerf();
        }
        currentPerf = new Performance(sender);
        d.setPerf(currentPerf);
        try {
            d.initialize();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Device init failed!", e);
            return;
        }
        devices.put(ud.address(), d);

        if (currentDevice != null) {
            log.info("Setting current device to " + ud.address());
            currentDevice = d;
            //only notify for current
            notifyDeviceInit(d);
            try {
                currentPerf.initialize();
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error in initializing performance",e);
            }
            initPerf();
        }

        try {
            d.sendStartStopComm(true);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Device start comm failed", e);
        }

    }

    private void initPerf() {
        for (LifecycleListener<Performance> l : perfListeners) {
            try {
                l.onLifecycleInit(currentPerf);
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error in perf listener",e);
            }
        }
    }

    private void disconnected(UsbService.UsbDevice ud) {
        Device d = devices.remove(ud.address());
        notifyDeviceDispose(d);
        d.shutdown(false);
    }

    private void notifyDeviceInit(Device d) {
        listeners.forEach(l -> {
            try {
                l.onDeviceInitialized(d);
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error in device listener",e);
            }
        });
    }

    private void notifyDeviceDispose(Device d) {

        //only act on current
        if (d != currentDevice || d == null) { return; }

//        disposePerf(); TODO might need this

        listeners.forEach(l -> {
            try {
                l.onDeviceDisposal(d);
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error in device disposal listener",e);
            }
        });

        currentDevice = null;

    }

    public Device getCurrent() {
        return currentDevice;
    }

    @Override
    public void onConnectionEvent(UsbService.UsbDevice device, boolean connected) {
        executorService.execute(() -> {
            if (connected) {
                connected(device);
            } else {
                disconnected(device);
            }
        });
    }

    public void shutdown() throws Exception {

        //blocking shutdown of devices
        Future<Boolean> f = executorService.submit(() -> {
            for (Device d : devices.values()) {
                d.shutdown(true);
            }
            return true;
        });
        f.get();

        executorService.shutdown();


    }


    public void loadFile(String path) {
        try {

            if (path.endsWith("prf2")) {
                disposePerf();
                currentPerf = Performance.readFromFile(path,sender);
                if (currentDevice != null) { currentDevice.setPerf(currentPerf); }
                initPerf();

                currentPerf.sendPerf();

            }
            if (path.endsWith("pch2")) {
                throw new UnsupportedOperationException("Patch load TODO"); //TODO
            }
        } catch (Exception e) {
            log.log(Level.SEVERE,"File load failed",e);
        }
    }

    private void disposePerf() {
        if (currentPerf == null) return;

        for (LifecycleListener<Performance> l : perfListeners) {
            try {
                l.onLifecycleDispose(currentPerf);
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error in perf listener",e);
            }
        }

        currentPerf = null;
    }

    public <T>T withCurrent(ThrowingFunction<Device,T> f) throws Exception {
        if (currentDevice == null) { throw new IllegalStateException("Current device not initialized"); }
        return f.invoke(currentDevice);
    }

    @Override
    public <T>T withCurrentPerf(ThrowingFunction<Performance, T> f) throws Exception {
        if (currentPerf == null) { throw new IllegalStateException("Current device/perf not initialized"); }
        return f.invoke(currentPerf);
    }



    private record FailableResult<R>(RuntimeException failure,R result) {
        static <R> FailableResult<R> failed(RuntimeException e) { return new FailableResult<>(e,null); }
        static <R> FailableResult<R> success(R r) { return new FailableResult<>(null,r); }
        public R get() {
            if (failure != null) { throw failure; }
            return result;
        }
    }

    @Override
    public <V> V invoke(Callable<V> c) {
        Future<FailableResult<V>> f = executorService.submit(() -> {
            try {
                return FailableResult.success(c.call());
            } catch (RuntimeException e) {
                return FailableResult.failed(e);
            } catch (Exception e) {
                return FailableResult.failed(new RuntimeException(e));
            }
        });
        try {
            return f.get().get();
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke callable",e);
        }

    }

    @Override
    public <V> V invokeWithCurrent(ThrowingFunction<Device, V> f) {
        return invoke(() -> withCurrent(f));
    }

    @Override
    public <V> V invokeWithCurrentPerf(ThrowingFunction<Performance, V> f) {
        return invoke(() -> withCurrentPerf(f));
    }

    @Override
    public void runWithCurrentPerf(ThrowingConsumer<Performance> f) {
        execute(() -> {
            if (currentPerf == null) {
                throw new IllegalStateException("Current device/perf not initialized");
            }
            f.accept(currentPerf);
        });
    }

    @Override
    public void runWithCurrent(ThrowingConsumer<Device> f) {
        execute(() -> {
            if (currentDevice == null) {
                throw new IllegalStateException("Current device not initialized");
            }
            f.accept(currentDevice);
        });
    }


    @Override
    public void execute(ThrowingRunnable r) {
        executorService.execute(() -> {
            try {
                r.run();
            } catch (Exception e) {
                log.log(Level.SEVERE,"execute: unexpected error",e);
            }
        });
    }
}
