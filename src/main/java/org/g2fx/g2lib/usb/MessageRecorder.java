package org.g2fx.g2lib.usb;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.g2fx.g2lib.util.Util;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MessageRecorder {
    private long time = -1;
    private final File path;
    private final PrintWriter pw;
    private int count = 0;



    public MessageRecorder(String session, File dir) throws Exception {
        this.path = new File(dir,session + ".yaml");
        this.pw = new PrintWriter(new FileWriter(path));
        pw.println("msgs:");
    }

    public File getPath() {
        return path;
    }

    public long getElapsed() {
        long t = System.currentTimeMillis();
        if (time == -1) {
            time = t;
            return 0;
        }
        long l = t - time;
        time = t;
        return l;
    }

    public void record(UsbMessage msg) throws Exception {
        if (!msg.success()) { return; }
        count++;
        dumpMsgYaml(msg, getElapsed(), pw);
    }

    public static void dumpMsgYaml(UsbMessage msg, long time, PrintWriter pw) {
        pw.format("- time: %s\n", time);
        pw.format("  extended: %s\n", msg.extended());
        pw.format("  crc: %04x\n", msg.crc());
        pw.print( "  data: >-");
        ByteBuffer buf = msg.buffer().rewind();
        int i = 0;
        while (buf.hasRemaining()) {
            if (i == 0) { pw.println(); pw.print("   "); }
            pw.format(" %02x", buf.get());
            if (++i == 16) { i = 0; }
        }
        pw.println();
        pw.println();
    }

    public int stop() {
        pw.flush();
        pw.close();
        return count;
    }

    record RecordMsg (
            long time,
            boolean extended,
            @JsonDeserialize(using = CrcDesz.class)
            int crc,
            @JsonDeserialize(using = BytesDesz.class)
            ByteBuffer data) {}

    public record RecordedUsbMessage (long time,UsbMessage msg) {
        @Override
        public String toString() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            dumpMsgYaml(msg,time,pw);
            pw.flush();
            return sw.toString();
        }
    }

    public static List<RecordedUsbMessage> readSessionFile(File f) throws Exception {
        ObjectMapper mapper = Util.mkYamlMapper();
        HashMap<String, List<RecordMsg>> y = mapper.readValue(f, new TypeReference<>() {});
        List<RecordMsg> rms = y.get("msgs");
        return rms.stream().map(rm -> new RecordedUsbMessage(rm.time,new UsbMessage(rm.data.limit(),rm.extended,rm.crc,rm.data))).toList();
    }

    public static class CrcDesz extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            return Integer.parseUnsignedInt(value,16);
        }
    }

    public static class BytesDesz extends JsonDeserializer<ByteBuffer> {
        @Override
        public ByteBuffer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String[] value = p.getValueAsString().split(" ");
            ByteBuffer buf = ByteBuffer.allocateDirect(value.length);
            Arrays.stream(value).forEach(s -> buf.put((byte)Integer.parseUnsignedInt(s,16)));
            return buf;
        }
    }

}
