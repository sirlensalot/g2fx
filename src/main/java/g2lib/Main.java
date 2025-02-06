package g2lib;

import g2lib.repl.Repl;
import g2lib.state.Device;
import g2lib.state.Devices;
import g2lib.usb.UsbService;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

public class Main {

    public static final String PROP_REPL = "repl";

    public static void main(String[] args) throws Exception {

        Util.configureLogging(Level.INFO);

        UsbService usb = new UsbService();

        Devices devices = new Devices();

        CountDownLatch deviceInitialized = new CountDownLatch(1);

        Repl repl = new Repl(devices);
        repl.start();

        if (!repl.replEnabled()) {
            devices.addListener(d -> {
                // on devices thread, so can directly fire off stuff
                Map<Integer, Map<Integer, String>> perfs = d.readEntryList(8, false);
                Device.dumpEntries(new PrintWriter(System.out),false,perfs,0);
                deviceInitialized.countDown();
            });
        }

        usb.addListener(devices);

        usb.startListener();
        usb.start();

        repl.join();

        if (!repl.replEnabled()) {
            deviceInitialized.await();
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
