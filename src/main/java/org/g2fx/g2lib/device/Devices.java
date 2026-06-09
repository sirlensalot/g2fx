package org.g2fx.g2lib.device;

import org.g2fx.g2lib.repl.Path;
import org.g2fx.g2lib.state.LifecycleListener;
import org.g2fx.g2lib.state.Patch;
import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.state.Slot;
import org.g2fx.g2lib.usb.*;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton facade/pub-sub representing G2 device(s) and current Performance.
 */
public class Devices implements UsbService.UsbConnectionListener, LibExecutor {

    private static final Logger log = Util.getLogger(Devices.class);

    public List<DeviceListener> listeners = new CopyOnWriteArrayList<>();
    public List<LifecycleListener<Performance>> perfListeners = new CopyOnWriteArrayList<>();
    public List<LifecycleListener<Patch>> patchListeners = new CopyOnWriteArrayList<>();

    private final ExecutorService executorService;

    /**
     * Multi-device support not supported beyond usb connection/disconnection atm.
     */
    private final Map<Integer, Device> devices = new HashMap<>();

    private Performance currentPerf;

    private UsbSender currentSender = new OfflineSender();

    private final UsbSender delegatingSender = new UsbSender() {
        @Override
        public int sendBulk(String msg, boolean dispatch, ByteBuffer data) throws Exception {
            return currentSender.sendBulk(msg,dispatch,data);
        }

        @Override
        public void shutdown() {
            currentSender.shutdown();
        }

        @Override
        public void setDispatcher(Dispatcher dispatcher) {
            currentSender.setDispatcher(dispatcher);
        }
    };


    /**
     * Callback for Device/Dispatcher on inbound perf load events.
     */
    private final LifecycleListener<Performance> perfLoadListener = new LifecycleListener<>() {
        @Override
        public void onLifecycleInit(Performance performance) throws Exception {
            setCurrentPerf(performance); //redundant set on device but oh well
            notifyPerfInit();
        }

        @Override
        public void onLifecycleDispose(Performance performance) throws Exception {
            disposePerf();
        }
    };

    /**
     * Callback for Device/Dispatcher on inbound patch load events.
     */
    private final LifecycleListener<Patch> patchLoadListener = new LifecycleListener<>() {
        @Override
        public void onLifecycleInit(Patch patch) throws Exception {
            LifecycleListener.notifyLifecycleInit(patchListeners,patch);
        }

        @Override
        public void onLifecycleDispose(Patch patch) throws Exception {
            LifecycleListener.notifyLifecycleDispose(patchListeners,patch);
        }
    };

    /**
     * Persistent "fake device" when G2 not connected.
     */
    private final Device offlineDevice = new Device(currentSender, perfLoadListener, patchLoadListener);

    private Device currentDevice = offlineDevice;

    public Devices(UsbService usbService) {
        this.executorService = Executors.newSingleThreadExecutor();
        usbService.addListener(this);
    }


    public void addListener(DeviceListener listener) {
        listeners.add(listener);
    }

    public void addPerfListener(LifecycleListener<Performance> listener) {
        perfListeners.add(listener);
    }

    private void connected(UsbService.UsbDevice ud) {

        if (devices.get(ud.address()) != null) {
            log.severe("connected: already have device at address! " + ud.address());
            return;
        }

        Usb usb = new Usb(ud);
        // ugly workaround for multi-device non-support
        UsbSender tmpSender = currentSender;
        currentSender = usb;
        Device d = new Device(delegatingSender, perfLoadListener, patchLoadListener);
        devices.put(ud.address(), d);

        if (currentDevice.online()) {
            log.warning("connected: current device already connected, ignoring: " + ud.address());
            currentSender = tmpSender;
            return;
        }

        notifyDeviceDispose(offlineDevice);
        currentDevice = d;
        log.info("Setting current device to address: " + ud.address());

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

        if (currentPerf != null) { //TODO this is not consistent with legacy app which will send current perf
            disposePerf();
        }
        setCurrentPerf(new Performance(delegatingSender));
        d.setPerf(currentPerf);

        try {
            d.initialize();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Device init failed!", e);
            return;
        }

        notifyDeviceInit(d);

        try {
            currentPerf.initialize();
        } catch (Exception e) {
            log.log(Level.SEVERE,"Error in initializing performance",e);
        }

        notifyPerfInit();

        try {
            usb.sendStartStopComm(true);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Device start comm failed", e);
        }

    }


    private void disconnected(UsbService.UsbDevice ud) {
        Device d = devices.remove(ud.address());
        if (d == null) {
            log.severe("disconnected: received unknown address! " + ud.address());
        }
        if (d != currentDevice) {
            log.info("disconnected non-current device, ignoring: " + ud.address());
            return;
        }

        currentDevice = offlineDevice;
        currentSender = new OfflineSender();

        notifyDeviceDispose(d);

        d.shutdown(false);

    }

    public void initOfflineDevice() {
        if (currentDevice == offlineDevice) {
            notifyDeviceInit(offlineDevice);
        }
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
        listeners.forEach(l -> {
            try {
                l.onDeviceDisposal(d);
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error in device disposal listener",e);
            }
        });
    }

    /**
     * Exposed for testing/used internally.
     */
    public void setCurrentPerf(Performance currentPerf) {
        this.currentPerf = currentPerf;
        currentDevice.setPerf(currentPerf);
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


    /**
     * @param path extension determines if perf or patch loaded
     * @param slot optional slot, otherwise perf "current slot" used
     */
    public void loadFile(String path, Slot slot) {
        try {

            if (path.endsWith("prf2")) {
                disposePerf();
                setCurrentPerf(Performance.readFromFile(path, delegatingSender));
                notifyPerfInit();

                currentPerf.sendPerf();

            } else if (path.endsWith("pch2")) {
                if (currentPerf == null) {
                    newPerformance();
                }
                if (slot == null) { slot = currentPerf.getSelectedSlot(); }
                patchLoadListener.onLifecycleDispose(currentPerf.getSlot(slot));
                Patch p = currentPerf.readPatchFromFile(slot,path);
                patchLoadListener.onLifecycleInit(p);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE,"File load failed",e);
        }
    }


    public void newPerformance() {
        disposePerf();
        setCurrentPerf(new Performance(delegatingSender));
        try {
            currentPerf.initNew();
            notifyPerfInit();
            currentPerf.sendPerf();
        } catch (Exception e) {
            log.log(Level.SEVERE,"newPerformance: failure",e);
        }
    }

    private void disposePerf() {
        if (currentPerf == null) return;

        notifyPerfDispose();

        currentPerf = null;
    }


    private void notifyPerfInit() {
        LifecycleListener.notifyLifecycleInit(perfListeners, currentPerf);
    }

    private void notifyPerfDispose() {
        LifecycleListener.notifyLifecycleDispose(perfListeners,currentPerf);
    }



    public Path getPath() {
        return Path.mkPath(currentDevice,currentPerf);
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
        return invoke(() -> f.invoke(currentDevice));
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
        execute(() -> f.accept(currentDevice));
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

    public void addPatchListener(LifecycleListener<Patch> lifecycleListener) {
        patchListeners.add(lifecycleListener);
    }

    /**
     * exposed for testing
     */
    public LifecycleListener<Performance> getPerfLoadListener() {
        return perfLoadListener;
    }

    /**
     * exposed for testing
     */
    public LifecycleListener<Patch> getPatchLoadListener() {
        return patchLoadListener;
    }

    /**
     * exposed for testing
     */
    public Performance getCurrentPerf() {
        return currentPerf;
    }

}
