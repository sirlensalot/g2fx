package org.g2fx.g2lib;

import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.device.DeviceListener;
import org.g2fx.g2lib.device.Devices;
import org.g2fx.g2lib.repl.Repl;
import org.g2fx.g2lib.usb.UsbService;
import org.g2fx.g2lib.util.Util;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {

    public static Logger log;

    public static void main(String[] args) throws Exception {

        boolean replEnabled = true;
        File scriptFile = null;
        String usage = "ARGS: (--no-repl|SCRIPT_FILE)";
        if (args.length > 0) {
            String a1 = args[0];
            if ("--no-repl".equals(a1)) {
                replEnabled = false;
            } else {
                scriptFile = new File(a1);
                if (!scriptFile.isFile()) {
                    throw new IllegalArgumentException("Invalid script file!\n" + usage);
                }
            }
        }

        Util.configureLogging();  // WARNING is quiet, INFO is pretty loud
        log = Util.getLogger(Main.class);

        UsbService usbService = new UsbService();
        Devices devices = new Devices(usbService);

        final CountDownLatch deviceInitialized = new CountDownLatch(1);

        Repl repl = new Repl(devices,replEnabled,scriptFile);

        devices.addListener(
                new DeviceListener() {
                    @Override
                    public void onDeviceInitialized(Device d) throws Exception {
                        deviceInitialized.countDown();
                    }

                    @Override
                    public void onDeviceDisposal(Device d) throws Exception {

                    }
                });

        usbService.start();

        log.info(() -> "Awaiting initialization ...");
        boolean initSuccess = deviceInitialized.await(2000,TimeUnit.MILLISECONDS);
        repl.start(initSuccess);

        repl.join();

        repl.stop();

        devices.shutdown();
        usbService.shutdown();

        log.info("Exit");
    }

}
