package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.util.Util;

import java.io.File;

public class MessageRecorder {
    private final String session;
    private final File dir;
    private final long time;
    private int ctr = 0;

    public MessageRecorder(String session, File dir) {
        this.session = session;
        this.dir = dir;
        this.time = System.currentTimeMillis();
    }

    public String getSession() {
        return session;
    }

    public File getDir() {
        return dir;
    }

    public long getElapsed() {
        return System.currentTimeMillis() - time;
    }

    public void record(UsbMessage msg) throws Exception {
        String name = String.format("%05d_%s_%x_%x.msg", ctr++, session, msg.crc(), getElapsed());
        Util.writeBuffer(msg.buffer().rewind(), new File(dir, name));
    }
}
