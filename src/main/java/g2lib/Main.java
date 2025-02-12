package g2lib;

import g2lib.repl.Repl;
import g2lib.state.Device;
import g2lib.state.Devices;
import g2lib.usb.UsbService;
import g2lib.util.Util;

import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static final String PROP_REPL = "repl";

    public static Logger log;

    public static void main(String[] args) throws Exception {

        Util.configureLogging(Level.INFO);
        log = Util.getLogger(Main.class);

        UsbService usb = new UsbService();

        Devices devices = new Devices();

        final CountDownLatch deviceInitialized = new CountDownLatch(1);

        Repl repl = new Repl(devices);
        repl.start();

        long is = System.currentTimeMillis();

        if (!repl.replEnabled()) {
            devices.addListener(d -> {
                // on devices thread, so can directly fire off stuff
                d.dumpEntries(new PrintWriter(System.out), Device.EntryType.Patch);
                d.dumpEntries(new PrintWriter(System.out), Device.EntryType.Perf);
                deviceInitialized.countDown();
            });
        }

        usb.addListener(devices);

        usb.startListener();
        usb.start();

        repl.join();

        if (!repl.replEnabled()) {
            log.info("awaiting init");
            log.info("init success: " + deviceInitialized.await(15, TimeUnit.SECONDS));
            log.info("init took " + (System.currentTimeMillis() - is) + "ms");
        }

        repl.stop();

        usb.stop();
        usb.stopListener();

        devices.shutdown();
        usb.shutdown();

        System.out.println("Exit");
        System.exit(0);
    }

}
