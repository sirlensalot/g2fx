package g2lib;

import g2lib.state.Device;
import g2lib.usb.Usb;
import g2lib.usb.UsbMessage;
import g2lib.usb.UsbReadThread;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.TerminalBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Main {


    private static final Logger log = Util.getLogger(Main.class);
    public static final String PROP_NO_REPL = "no-repl";

    public static void main(String[] args) throws Exception {


        final LineReader lineReader = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.terminal())
                .parser(new DefaultParser())
                .completer(new StringsCompleter("describe", "create"))
                .build();

        Usb usb;
        int retry = 0;
        while (true) {
            try {
                usb = Usb.initialize();
                break;
            } catch (Exception e) {
                if (retry++ < 10) {
                    lineReader.printAbove(
                            String.format("Failed to acquire USB device, retrying [%s of %s]...",
                                    retry,10));
                    Thread.sleep(2000);
                } else {
                    throw e;
                }
            }
        }

        final Usb _usb = usb;



        final UsbReadThread readThread = new UsbReadThread(usb);
        readThread.start();

        boolean replEnabled = System.getProperty(PROP_NO_REPL) != null;

        Thread replThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    doRepl(lineReader,_usb,readThread);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        if (replEnabled) { replThread.start(); }

        Device device = new Device(usb,readThread);
        device.initialize();

        System.out.println("Received: " + readThread.recd.get());
        System.out.println("queue size: " + readThread.q.size());

        if (replEnabled) { replThread.join(); }

        readThread.go.set(false);
        System.out.println("joining");
        readThread.thread.join();


        usb.shutdown();


        System.out.println("Exit");
    }

    private void listPerfs() {

    }

    private static void doRepl(LineReader reader, Usb usb, UsbReadThread readThread) throws Exception {
        if (System.getProperty(PROP_NO_REPL) != null) {
            log.info("Repl disabled with " + PROP_NO_REPL);
            return;
        }

        while (true) {
            String line = reader.readLine("> ");
            if (line == null || line.equalsIgnoreCase("exit")) {
                break;
            }
            List<String> words = new ArrayList<>(reader.getParsedLine().words());
            if (words.isEmpty()) continue;
            String cmd = words.removeFirst();
            if ("list".equals(cmd) && !words.isEmpty()) {
                String type = words.removeFirst();
                Integer bank = null;
                if (!words.isEmpty()) {
                    try {
                        bank = Integer.parseUnsignedInt(words.removeFirst());
                    } catch (Exception e) {
                        System.out.println("Invalid bank");
                    }
                }
                if ("perfs".equals(type)) {
                    Map<Integer, Map<Integer, String>> perfs = Device.readEntryList(usb, readThread, 8, false);
                    Device.dumpEntries(false,perfs,bank);
                } else if ("patches".equals(type)) {
                    Map<Integer, Map<Integer, String>> patches = Device.readEntryList(usb, readThread, 32, true);
                    Device.dumpEntries(true,patches,bank);
                }
            }
        }
    }

    static UsbMessage writeMsg(String name,UsbMessage m) {
        if (m == null) { return null; }
        Util.writeBuffer(m.buffer().rewind(), String.format("msg_%s_%x.msg",name,m.crc()));
        return m;
    }


}