package g2lib.state;

import g2lib.Main;
import g2lib.Util;
import g2lib.repl.Repl;
import g2lib.usb.Usb;
import g2lib.usb.UsbReadThread;
import g2lib.usb.UsbService;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Devices implements UsbService.UsbConnectionListener {

    private static final Logger log = Util.getLogger(Devices.class);



    public interface DeviceListener {
        public void onDeviceInitialized(Device d) throws Exception;
    }

    public List<DeviceListener> listeners = new CopyOnWriteArrayList<>();

    private final ExecutorService executorService;
    private final Map<Integer, Device> devices = new HashMap<>();
    private Device current;

    public Devices(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void addListener(DeviceListener listener) {
        listeners.add(listener);
    }

    protected void connected(UsbService.UsbDevice ud) {
        Usb usb = new Usb(ud);
        UsbReadThread rt = new UsbReadThread(usb);
        final Device d = new Device(usb, rt);
        rt.start();
        try {
            d.initialize();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Device init failed!", e);
            return;
        }
        devices.put(ud.address(), d);
        log.info("Setting current device to " + ud.address());
        current = d;
        listeners.forEach(l -> {
            try {
                l.onDeviceInitialized(d);
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error in device listener",e);
            }
        });
    }

    protected void disconnected(UsbService.UsbDevice ud) {
        Device d = devices.remove(ud.address());
        if (d != null) {
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
    }


    public Repl.Path getCurrentPath() {
        if (current == null) { return null; }
        return current.getPath();
    }


    public Repl.Path loadFile(String path) {
        if (current == null) {
            log.info("Initializing offline device");
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
}
