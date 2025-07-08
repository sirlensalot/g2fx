package org.g2fx.g2lib.state;

import org.g2fx.g2lib.repl.Repl;
import org.g2fx.g2lib.usb.Usb;
import org.g2fx.g2lib.usb.UsbService;
import org.g2fx.g2lib.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Devices implements UsbService.UsbConnectionListener, Executor {

    private static final Logger log = Util.getLogger(Devices.class);

    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public interface DeviceListener {
        void onDeviceInitialized(Device d) throws Exception;
        void onDeviceDisposal(Device d) throws Exception;
    }

    public List<DeviceListener> listeners = new CopyOnWriteArrayList<>();

    private final ExecutorService executorService;
    private final Map<Integer, Device> devices = new HashMap<>();
    private Device current;

    private UsbService usbService;

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
        listeners.forEach(l -> {
            try {
                l.onDeviceInitialized(d);
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error in device listener",e);
            }
        });
    }

    private void disconnected(UsbService.UsbDevice ud) {
        Device d = devices.remove(ud.address());
        if (d != null) {
            listeners.forEach(l -> {
                try {
                    l.onDeviceDisposal(d);
                } catch (Exception e) {
                    log.log(Level.SEVERE,"Error in device disposal listener",e);
                }
            });

            d.shutdown();

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
        CountDownLatch latch = new CountDownLatch(1);
        executorService.execute(() -> {
            devices.values().forEach(Device::shutdown);
            latch.countDown();
        });
        latch.await();

        executorService.shutdown();

        usbService.stop();
        usbService.stopListener();
        usbService.shutdown();


    }


    public Repl.Path getCurrentPath() {
        if (current == null) { return null; }
        return current.getPath();
    }

    public Repl.SlotPatch getSlotPatch(Slot slot) {
        return current != null ? current.getSlotPatch(slot) : null;
    }


    public Repl.Path loadFile(String path) {
        if (current == null) {
            log.fine("Initializing offline device");
            current = new Device();
        }
        try {
            if (path.endsWith("prf2")) {
                return current.loadPerfFile(path);
            }
            if (path.endsWith("pch2")) {
                throw new UnsupportedOperationException("Patch load TODO"); //TODO
            }
        } catch (Exception e) {
            log.log(Level.SEVERE,"File load failed",e);
        }
        return null;
    }


    private record FailableResult<R>(RuntimeException failure,R result) {
        static <R> FailableResult<R> failed(RuntimeException e) { return new FailableResult<>(e,null); }
        static <R> FailableResult<R> success(R r) { return new FailableResult<>(null,r); }
        public R get() {
            if (failure != null) { throw failure; }
            return result;
        }
    }
    public <V> V invoke(Callable<V> c) { return invoke(false,c); }

    public <V> V invoke(boolean offlineOk, Callable<V> c) {
        Future<FailableResult<V>> f = executorService.submit(() -> {
            if (!offlineOk && !online()) {
                return FailableResult.failed(new IllegalStateException("Device offline"));
            }
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
    public void execute(Runnable command) {
        execute((ThrowingRunnable) command::run);
    }

    public void execute(ThrowingRunnable r) {
        execute(false,r);
    }

    public void execute(boolean offlineOk, ThrowingRunnable r) {
        invoke(offlineOk,() -> {
            r.run();
            return 1;
        });
    }
}
