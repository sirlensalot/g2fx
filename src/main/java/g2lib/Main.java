package g2lib;

import g2lib.state.Device;
import g2lib.usb.Usb;
import g2lib.usb.UsbMessage;
import g2lib.usb.UsbReadThread;

import java.util.Map;
import java.util.logging.Logger;

public class Main {


    private static final Logger log = Util.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        Usb usb;
        int retry = 0;
        while (true) {
            try {
                usb = Usb.initialize();
                break;
            } catch (Exception e) {
                if (retry++ < 10) {
                    log.info("Failed to acquire USB device, retrying...");
                    Thread.sleep(2000);
                } else {
                    throw e;
                }
            }
        }

        UsbReadThread readThread = new UsbReadThread(usb);
        readThread.thread.start();


        // init message
        usb.sendBulk("Init", Util.asBytes(0x80)); // CMD_INIT
        //extended: 80 0a 03 00 -- 80/hello machine
        writeMsg("Init",readThread.expect("Init response", msg -> msg.head(0x80)));

        // perf version (?)
        usb.sendSystemCmd("perf version"
                ,0x35 // Q_VERSION_CNT
                ,0x04 // perf version??
            );
        //embedded: 82 01 0c 40 36 04 00 , "perf version" [04] 00
        writeMsg("PerfVersion",readThread.expect("perf version",msg -> msg.head(0x82,0x01,0x0c,0x40,0x36,0x04)));

        //stop comm 0x01
        usb.sendSystemCmd("Stop Comm"
                ,0x7d // S_START_STOP_COM
                ,0x01 // stop
                );
        //embedded: 62 01 0c 00 7f -- 62 01 (stop message/OK)
        writeMsg("CommStop",readThread.expect("Stop Comm",m -> m.head(0x62,0x01)));

        //synth settings
        usb.sendSystemCmd("Synth settings"
                ,0x02 // Q_SYNTH_SETTINGS
        );
        //extended: 01 0c 00 03 -- synth settings [03]
        writeMsg("SynthSettings",readThread.expect("Synth settings",m -> m.head(0x01,0x0c,0x00,0x03)));

        //unknown 1
        usb.sendSystemCmd("unknown 1"
                ,0x81 // M_UNKNOWN_1
        );
        //extended: 01 0c 00 80 -- 80/"unknown 1" (slot hello?)
        writeMsg("SlotInit",readThread.expect("slot init",m -> m.head(0x01,0x0c,0x00,0x80)));

        usb.sendSystemCmd("perf settings"
                ,0x10 // Q_PERF_SETTINGS
        );
        //extended: 01 0c 00 29 -- perf settings [29 "perf name"]
        //  then chunks in TG2FilePerformance.Read
        writeMsg("PerfSettings",readThread.expect("perf settings",m->m.head(0x01,0x0c,0x00,0x29)));

        usb.sendSystemCmd("unknown 2"
                ,0x59 // M_UNKNOWN_2
        );
        //embedded: 72 01 0c 00 1e -- "unknown 2" [1e]
        writeMsg("Reserved2",readThread.expect("reserved 2",m->m.head(0x72)));

        usb.sendSystemCmd("slot 1 version"
                ,0x35 // Q_VERSION_CNT
                ,1 // slot index
        );
        //embedded: 82 01 0c 40 36 01 -- slot version
        writeMsg("Slot1Version",readThread.expect("slot 1 version",m->m.head(0x82,0x01,0x0c,0x40,0x36,0x01)));

        usb.sendSlotRequest(1,0,"slot 1 patch",
                0x3c // Q_PATCH
        );
        //extended: 01 09 00 21 -- patch description, slot 1
        writeMsg("Slot1Patch",readThread.expect("slot 1 patch",m->m.head(0x01,0x09,0x00,0x21)));

        usb.sendSlotRequest(0,0,"slot 0 patch",
                0x3c // Q_PATCH
        );
        //extended: 01 09 00 21 -- patch description, slot 1
        writeMsg("Slot0Patch",readThread.expect("slot 1 patch",m->m.head(0x01,0x08,0x00,0x21)));

        usb.sendSlotRequest(1,0,"slot 1 name",
                0x28 // Q_PATCH_NAME
        );
        //extended: 01 09 00 27 -- patch name, slot 1
        writeMsg("Slot1Name",readThread.expect("slot 1 name",m->m.head(0x01,0x09,0x00,0x27)));

        usb.sendSlotRequest(1,0,"slot 1 note",
                0x68 // Q_CURRENT_NOTE
        );
        //extended: 01 09 00 69 -- cable list, slot 1
        writeMsg("Slot1Note",readThread.expect("slot 1 note",m->m.head(0x01,0x09,0x00,0x69)));


        usb.sendSlotRequest(1,0,"slot 1 text",
                0x6e //Q_PATCH_TEXT
        );
        //extended: 01 09 00 6f -- textpad, slot 1
        writeMsg("Slot1TextPad",readThread.expect("slot 1 text",m->m.head(0x01,0x09,0x00,0x6f)));


        //send list message:

        //patches: 32 Banks with 128 memory locations each
        Map<Integer, Map<Integer, String>> patches = Device.readEntryList(usb, readThread, 32, true);


        //perfs: 8 Banks with 128 memory locations each
        Map<Integer, Map<Integer, String>> perfs = Device.readEntryList(usb, readThread, 8, false);

        System.out.println("Received: " + readThread.recd.get());
        System.out.println("queue size: " + readThread.q.size());

        readThread.go.set(false);
        System.out.println("joining");
        readThread.thread.join();

        usb.shutdown();

        Device.dumpEntries(true,patches);
        Device.dumpEntries(false,perfs);

        System.out.println("Exit");
    }

    static UsbMessage writeMsg(String name,UsbMessage m) {
        if (m == null) { return null; }
        Util.writeBuffer(m.buffer().rewind(), String.format("msg_%s_%x.msg",name,m.crc()));
        return m;
    }


}