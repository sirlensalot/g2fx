package g2lib;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.*;

public class Util {

    static final Logger log = getLogger(Util.class);

    static class DualConsoleHandler extends StreamHandler {

        private final ConsoleHandler stderrHandler = new ConsoleHandler();

        public DualConsoleHandler() {
            super(System.out, new SimpleFormatter());
        }

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() <= Level.INFO.intValue()) {
                super.publish(record);
                super.flush();
            } else {
                stderrHandler.publish(record);
                stderrHandler.flush();
            }
        }
    }

    public static final Logger getLogger(Class<?> c) {
        Logger l = Logger.getLogger(c.getName());
        l.setUseParentHandlers(false);
        l.addHandler(new DualConsoleHandler());
        return l;
    }

    public static void dumpBuffer(ByteBuffer buffer) {
        log.info(dumpBufferString(buffer));
    }

    public static String dumpBufferString(ByteBuffer buffer) {
        StringBuilder hex = new StringBuilder();
        StringBuilder ascii = new StringBuilder();
        StringBuilder output = new StringBuilder("\n");
        int pos = buffer.position();
        buffer.rewind();
        int i = 0;
        while (buffer.hasRemaining()) {
            byte d = buffer.get();
            hex.append(String.format("%02x ", d));
            ascii.append((d >= 33 && d < 126) ? String.format("%c ", d) : ". ");
            if (i % 16 == 15) {
                output.append(String.format("%s  %s\n", hex, ascii));
                hex = new StringBuilder();
                ascii = new StringBuilder();
            }
            i++;
        }
        if (i % 16 > 0) {
            int pad = 3 * (16 - (i % 16));
            //System.out.printf("pad %d %d\n",pad,i % 16);
            output.append(String.format("%s %" + pad + "s %s\n", hex, "", ascii));
        }
        buffer.position(pos);
        return output.toString();
    }

    public static int b2i(byte b) {
        return b & 0xff;
    }

    public static int addb(byte msb, byte lsb) {
        return (b2i(msb) << 8) + b2i(lsb);
    }

    public static int getShort(ByteBuffer buffer) {
        return addb(buffer.get(),buffer.get());
    }

    public static void putShort(ByteBuffer buffer, int value) {
        buffer.put((byte) ((value >> 8) & 0xff));
        buffer.put((byte) (value & 0xff));
    }

    public static byte[] asBytes(int... vals) {
        byte[] bytes = new byte[vals.length];
        for (int i = 0; i < vals.length; i++) {
            bytes[i] = (byte) vals[i];
        }
        return bytes;
    }

    public static byte[] concat(byte[]... byteArrays) {
        if (byteArrays.length == 0) { return new byte[0]; }
        int len = 0;
        for (byte[] a : byteArrays) {
            len += a.length;
        }
        byte[] r = new byte[len];
        int i = 0;
        for (byte[] a : byteArrays) {
            for (byte b : a) {
                r[i++] = b;
            }
        }
        return r;
    }

    public static String asBinary(int i) {
        StringBuilder b = new StringBuilder(Integer.toBinaryString(i));
        while (b.length() < 8) {
            b.insert(0, '0');
        }
        return b.toString();
    }

    public static ByteBuffer readFile(String path) throws Exception {
        ByteBuffer buf;
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] bs = fis.readAllBytes();
            buf = ByteBuffer.wrap(bs);
        }
        return buf;
    }

    public static void dumpAllShifts(ByteBuffer buf) {
        buf.rewind();
        Util.dumpBuffer(buf);
        for (int i = 1; i < 7; i++) {
            buf.rewind();
            System.out.println("Shift " + i);
            Util.dumpBuffer(BitBuffer.shiftedBuffer(buf,i));
        }
        buf.rewind();
    }

    public static ByteBuffer sliceAhead(ByteBuffer buffer, int length) {
        ByteBuffer slice = buffer.slice().limit(length);
        advanceBuffer(buffer, length);
        return slice;
    }

    public static void advanceBuffer(ByteBuffer buffer, int length) {
        buffer.position(buffer.position()+ length);
    }

    public static void writeBuffer(ByteBuffer data, String name) {
        try (FileOutputStream fos = new FileOutputStream("data/" + name)) {
            data.rewind();
            byte[] bs = new byte[data.limit()];
            data.get(bs);
            fos.write(bs);
            fos.flush();
        } catch (Exception e) {
            throw new RuntimeException("error writing patch desc", e);
        }
    }
}
