package org.g2fx.g2lib.state;

import org.g2fx.g2lib.usb.Usb;
import org.g2fx.g2lib.usb.UsbService;
import org.g2fx.g2lib.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Devices implements UsbService.UsbConnectionListener, DeviceExecutor {

    private static final Logger log = Util.getLogger(Devices.class);

    public interface DeviceListener {
        void onDeviceInitialized(Device d) throws Exception;
        void onDeviceDisposal(Device d) throws Exception;
    }

    public List<DeviceListener> listeners = new CopyOnWriteArrayList<>();

    private final ExecutorService executorService;
    private final Map<Integer, Device> devices = new HashMap<>();
    private Device current;

    private final UsbService usbService;


    public Devices() {
        this.executorService = Executors.newSingleThreadExecutor();
        usbService = new UsbService();
        usbService.addListener(this);
    }

    public void start() {
        usbService.startListener();
        usbService.start();
    }

    public void addListener(DeviceListener listener) {
        listeners.add(listener);
    }

    private void connected(UsbService.UsbDevice ud) {

        if (current != null && !current.online()) {
            notifyDeviceDispose(current);
        }

        Usb usb = new Usb(ud);
        final Device d = new Device(usb);
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
        try {
            d.initialize();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Device init failed!", e);
            return;
        }
        devices.put(ud.address(), d);
        log.fine("Setting current device to " + ud.address());
        current = d;
        notifyDeviceInit(d);
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

        if (d != null) {
            listeners.forEach(l -> {
                try {
                    l.onDeviceDisposal(d);
                } catch (Exception e) {
                    log.log(Level.SEVERE,"Error in device disposal listener",e);
                }
            });
        }

        if (current == d) {
            current = null;
        }
    }

    public Device getCurrent() {
        return current;
    }

    public boolean online() { return current != null && current.online(); }

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

        usbService.stop();
        usbService.stopListener();
        usbService.shutdown();


    }


    public void loadFile(String path) {

        notifyDeviceDispose(current); //null safe

        if (current == null) {
            log.info("Initializing offline device");
            current = new Device();
        }
        try {
            if (path.endsWith("prf2")) {
                current.loadPerfFile(path);
                notifyDeviceInit(current);
            }
            if (path.endsWith("pch2")) {
                throw new UnsupportedOperationException("Patch load TODO"); //TODO
            }
        } catch (Exception e) {
            log.log(Level.SEVERE,"File load failed",e);
        }
    }

    public <T>T withCurrent(ThrowingFunction<Device,T> f) throws Exception {
        if (current == null) { throw new IllegalStateException("Current device not initialized"); }
        return f.invoke(current);
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
    public void runWithCurrent(ThrowingConsumer<Device> f) {
        execute(() -> {
            if (current == null) {
                throw new IllegalStateException("Current device not initialized");
            }
            f.accept(current);
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
