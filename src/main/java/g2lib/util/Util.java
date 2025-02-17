package g2lib.util;

import g2lib.usb.UsbMessage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.*;

public class Util {

    private static final Set<String> logNames = new HashSet<>();
    private static volatile Level DEFAULT_LEVEL = Level.FINE;

    private static final Logger log = getLogger(Util.class);

    static class DualConsoleHandler extends StreamHandler {

        private final ConsoleHandler stderrHandler = new ConsoleHandler();

        public DualConsoleHandler() {
            super(System.out, new SimpleFormatter());
            setLevel(DEFAULT_LEVEL);
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

    public static void configureLogging(Level defaultLevel) {
        String p = System.getProperty("g2lib.loglevel");
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT.%1$tL %4$s %3$s: %5$s%6$s%n");
        String ll = p != null ? p : defaultLevel.toString();
        try {
            DEFAULT_LEVEL = Level.parse(ll);
        } catch (Exception ignore) {
            DEFAULT_LEVEL = defaultLevel;
            ll = defaultLevel.toString();
        }
        final String fll = ll;
        try {
            LogManager.getLogManager().updateConfiguration(key -> (oldVal, newVal) ->
                    key.equals(".level") || key.equals("java.util.logging.ConsoleHandler.level")
                            ? fll : newVal);
        } catch (IOException ignore) {}
    }

    public static Logger getLogger(Class<?> c) {
        return getLogger(c.getName());
    }

    public static Logger getLogger(String name) {
        Logger l = Logger.getLogger(name);
        if (logNames.add(name)) {
            l.setUseParentHandlers(false);
            l.addHandler(new DualConsoleHandler());
        }
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


    public static UsbMessage writeMsg(String name, UsbMessage m) {
        if (m == null) { return null; }
        Util.writeBuffer(m.buffer().rewind(), String.format("msg_%s_%x.msg",name,m.crc()));
        return m;
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


    public static void expectWarn(ByteBuffer buf, int expected, String filePath, String msg) {
        byte b = buf.get();
        if (b != expected) {
            log.warning(String.format("%s: expected %x, found %x at %s:%d",msg,expected,b,filePath,buf.position()-1));
        }
    }

    public static Map<String,Object> withYamlMap(
            Consumer<Map<String,Object>> f) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        f.accept(m);
        return m;
    }
    public static List<Object> withYamlList(
            Consumer<List<Object>> f) {
        List<Object> m = new ArrayList<>();
        f.accept(m);
        return m;
    }
}
