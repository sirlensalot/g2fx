package g2lib;

import g2lib.state.Device;
import g2lib.usb.Usb;
import g2lib.usb.UsbMessage;
import g2lib.usb.UsbReadThread;
import g2lib.usb.UsbService;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.TerminalBuilder;
import org.usb4java.HotplugCallbackHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
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
    public static final String PROP_NO_REPL = "no-repl";

    public static void main(String[] args) throws Exception {


        final LineReader lineReader = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.terminal())
                .parser(new DefaultParser())
                .completer(new StringsCompleter("describe", "create"))
                .build();

        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        UsbService usb = new UsbService();

        Repl repl = new Repl(executorService);

        // should only be messaged via executor
        Devices devices = new Devices(executorService);

        usb.addListener(devices);

        usb.startListener();
        usb.start();


        Thread.sleep(20000);

        //device.initialize();

//        System.out.println("Received: " + readThread.recd.get());
//        System.out.println("queue size: " + readThread.q.size());

//        if (replEnabled) { replThread.join(); }
//
//        readThread.go.set(false);
        repl.stop();

        usb.stopListener();
        usb.stop();
//        System.out.println("joining");
//        readThread.thread.join();
//        usbEventThread.thread.join();



        usb.shutdown();


        System.out.println("Exit");
    }

    private void listPerfs() {

    }

    public static class Devices implements UsbService.UsbConnectionListener {

        private final ExecutorService executorService;
        private final Map<Integer,Device> devices = new HashMap<>();

        public Devices(ExecutorService executorService) {
            this.executorService = executorService;
        }

        protected void connected(UsbService.UsbDevice ud) {
            Usb usb = new Usb(ud);
            UsbReadThread rt = new UsbReadThread(usb);
            Device d = new Device(usb, rt);
            devices.put(ud.address(), d);
            rt.start();
            try {
                d.initialize();
            } catch (Exception e) {
                log.log(Level.SEVERE,"Device init failed!",e);
            }
        }
        protected void disconnected(UsbService.UsbDevice ud) {
            Device d = devices.remove(ud.address());
            if (d != null) {
                d.shutdown();
            }
        }

        @Override
        public void onConnectionEvent(UsbService.UsbDevice device, boolean connected) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    if (connected) {
                        connected(device);
                    } else {
                        disconnected(device);
                    }
                }
            });
        }
    }

    public static class Repl implements Runnable {

        private final ExecutorService executorService;
        private final LineReader reader;
        private final Thread thread;
        private volatile boolean running = true;

        public Repl(ExecutorService executorService) throws Exception {
            this.executorService = executorService;
            reader = LineReaderBuilder.builder()
                    .terminal(TerminalBuilder.terminal())
                    .parser(new DefaultParser())
                    .completer(new StringsCompleter("describe", "create"))
                    .build();
            thread = new Thread(this);
        }

        public PrintWriter getWriter() {
            return reader.getTerminal().writer();
        }

        public void start() {
            if (System.getProperty(PROP_NO_REPL) != null) {
                log.info("Repl disabled with " + PROP_NO_REPL);
                return;
            }
            thread.start();
        }

        public void stop() {
            running = false;
        }

        public void run() {

            while (running) {
                String line = reader.readLine("> ");
                if (line == null || line.equalsIgnoreCase("exit")) {
                    break;
                }
                List<String> words = new ArrayList<>(reader.getParsedLine().words());
                if (words.isEmpty()) continue;
                String cmd = words.removeFirst();
                if ("list".equals(cmd) && !words.isEmpty()) {
                    String type = words.removeFirst();

                    if (!words.isEmpty()) {
                        try {
                            int bank = Integer.parseUnsignedInt(words.removeFirst());
                        } catch (Exception e) {
                            System.out.println("Invalid bank");
                        }
                    }
                    if ("perfs".equals(type)) {
                        //Map<Integer, Map<Integer, String>> perfs = Device.readEntryList(usb, readThread, 8, false);
                        //Device.dumpEntries(false,perfs,bank);
                    } else if ("patches".equals(type)) {
                        //Map<Integer, Map<Integer, String>> patches = Device.readEntryList(usb, readThread, 32, true);
                        //Device.dumpEntries(true,patches,bank);
                    }
                }
            }
        }
    }

}