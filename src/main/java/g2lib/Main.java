package g2lib;

import g2lib.repl.Repl;
import g2lib.state.Device;
import g2lib.state.Devices;
import g2lib.usb.UsbService;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Main {
    /*
    THREADS and DATA SHARING
    THREADS:
    - Usb read thread
    - Usb event thread
    - UI thread (repl, or GUI)
    - Executor thread for driving device initialization etc.
      This is probably an Executor, for e.g. kicking off device initialization,
      on a single thread.

    Application lifecycle:
      1. Usb: initialize context, attach device connection listeners
      2. UI: paint, start interaction thread
      3. Usb: start receiving connect events. This presumably should
         not be doing anything else.

    On connect event:
      1. Initialize device on executor.
      2. Start read thread.

    DATA STORE (where persistent data [device current performance, settings]
    lives, should go ahead and support multiple devices why not): probably best
    owned by the executor thread, which is command driven. Thus, UI will send
    inbox messages for data, and commands as tasks.

     */


    private static final Logger log = Util.getLogger(Main.class);
    public static final String PROP_REPL = "repl";

    public static void main(String[] args) throws Exception {



        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        UsbService usb = new UsbService();

        // should only be messaged via executor
        Devices devices = new Devices(executorService);

        CountDownLatch deviceInitialized = new CountDownLatch(1);


        Repl repl = new Repl(executorService,devices);
        repl.start();

        if (!repl.replEnabled()) {
            devices.addListener(d -> {
                // on devices thread, so can directly fire off stuff
                Map<Integer, Map<Integer, String>> perfs = d.readEntryList(8, false);
                Device.dumpEntries(false,perfs,0);
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
        //device.initialize();

//        System.out.println("Received: " + readThread.recd.get());
//        System.out.println("queue size: " + readThread.q.size());

//        if (replEnabled) { replThread.join(); }
//
//        readThread.go.set(false);
        repl.stop();

        usb.stop();
        usb.stopListener();
//        System.out.println("joining");
//        readThread.thread.join();
//        usbEventThread.thread.join();


        devices.shutdown();
        usb.shutdown();


        System.out.println("Exit");
        System.exit(0);
    }

    private void listPerfs() {

    }

}