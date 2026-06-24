package org.g2fx.g2lib.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.collect.Streams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    @FunctionalInterface
    public
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public
    interface ThrowingBiConsumer<T,U> {
        void accept(T t,U u) throws Exception;
    }

    @FunctionalInterface
    public
    interface ThrowingFunction<A,R> {
        R invoke(A a) throws Exception;
    }

    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    public record UsbPacket(long elapsedMicros, ByteBuffer data) {
        @Override
        public String toString() {
            return elapsedMicros + ": " + dumpBufferString(data);
        }
    }

    public static ObjectMapper mkYamlMapper() {
        return new ObjectMapper(
                new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    }

    public static void configureLogging() {
        try (InputStream in = Util.class.getResourceAsStream("/org/g2fx/g2gui/logging.properties")) {
            LogManager.getLogManager().readConfiguration(in);
        } catch (Exception e) {
            System.out.println("Logging configuration failed: " + e);
            e.printStackTrace();
        }
    }

    public static Logger getLogger(Class<?> c, Object... names) {
        return Logger.getLogger(names.length == 0 ? c.getName() :
                c.getName() + "." + c.getSimpleName() + ":" + String.join(":",
                Arrays.stream(names).map(Object::toString).toList()));
    }


    public static String dumpBufferString(ByteBuffer buffer1) {
        ByteBuffer buffer = buffer1.slice(0,buffer1.limit());
        StringBuilder hex = new StringBuilder();
        StringBuilder ascii = new StringBuilder();
        StringBuilder output = new StringBuilder("\n");
        int i = 0;
        while (buffer.hasRemaining()) {
            if (i % 16 == 0) {
                output.append(String.format("%04x  ",i));
            }
            byte d = buffer.get();
            hex.append(String.format("%02x ", d));
            ascii.append((d >= 33 && d < 126) ? String.format("%c", d) : ".");
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
        return output.toString();
    }

    public static ByteBuffer readBufferDump(String v) {
        String[] lines = v.split("\\n");
        ByteBuffer buf = ByteBuffer.allocateDirect(v.length()); //oversize
        int sz = 0;
        for (String line : lines) {
            Matcher m = Pattern.compile("^([0-9a-fA-F]+ +)").matcher(line);
            if (m.find()) {
                String[] ws = line.substring(m.end(1), 0x36).split("\\s+");
                sz += ws.length;
                Arrays.stream(ws).forEach(s ->
                        buf.put((byte) parseByte(s)));
            }
        }
        buf.limit(sz);
        return buf;
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

    /**
     * TODO could probably just use byte order BIG_ENDIAN + put(short)
     */
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

    public static ByteBuffer readFile(String path) throws Exception {
        ByteBuffer buf;
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] bs = fis.readAllBytes();
            buf = ByteBuffer.wrap(bs);
        }
        return buf;
    }


    /**
     * Make a slice of BUFFER at position of LENGTH, advance BUFFER position
     * to end of length, return slice.
     */
    public static ByteBuffer sliceAhead(ByteBuffer buffer, int length) {
        ByteBuffer slice = buffer.slice().limit(length);
        buffer.position(buffer.position()+ length);
        return slice;
    }

    public static void writeBuffer(ByteBuffer data, File name) throws Exception {
        int position = data.position();
        try (FileOutputStream fos = new FileOutputStream(name)) {
            data.rewind();
            byte[] bs = new byte[data.limit()];
            data.get(bs);
            fos.write(bs);
            fos.flush();
            data.position(position);
        }
    }


    public static byte expectWarn(Logger log, ByteBuffer buf, int expected, String filePath, String msg) {
        byte b = buf.get();
        if (b != expected) {
            log.warning(String.format("%s: expected %x, found %x at %s:%d",msg,expected,b,filePath,buf.position()-1));
        }
        return b;
    }

    public static int mapRange(int value, int fromMin, int fromMax, int toMin, int toMax) {
        if (value < fromMin) {
            return toMin;
        }
        if (value > fromMax) {
            return toMax;
        }
        // Scale value from old range to new range
        return (value - fromMin) * (toMax - toMin) / (fromMax - fromMin) + toMin;
    }

    public static <T> void forEachIndexed(Collection<T> coll, BiConsumer<T,Integer> f) {
        List<Integer> ignored = mapWithIndex(coll,(v,ix) -> { f.accept(v,ix); return 1; });
    }

    public static <T,R> List<R> mapWithIndex(Collection<T> coll, BiFunction<T,Integer,R> f) {
        return Streams.mapWithIndex(coll.stream(),(v,ix) -> f.apply(v,(int) ix)).toList();
    }

    public static <T> int count(Collection<T> items, Function<T,Integer> f) {
        return items.stream().reduce(0,(s,i) -> s + f.apply(i),Integer::sum);
    }

    public static ByteBuffer readTextColsByteBuffer(String v) {
        String[] value = v.split("\\s+");
        ByteBuffer buf = ByteBuffer.allocateDirect(value.length);
        Arrays.stream(value).forEach(s -> buf.put((byte) parseByte(s)));
        return buf;
    }

    /**
     * @throws NumberFormatException on failure
     */
    private static int parseByte(String s) {
        return Integer.parseUnsignedInt(s, 16);
    }


    /**
     * pcapng file packet reader.
     * only tested with macos XHC20 (linktype 266), timestamps are epoch micros.
     */
    public static List<UsbPacket> readPcapNg(ByteBuffer bb) {
        int boMagic = bb.getInt(8);
        bb.order(boMagic == 0x4d3c2b1a ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        int shbLen = bb.getInt(4);
        bb.position(shbLen); //skip shb
        bb.getInt(); //IDB magic
        int idbLen = bb.getInt();
        bb.position((shbLen + idbLen)); // skip shb,idb

        List<UsbPacket> bbs = new ArrayList<>();

        long now = 0;

        while (bb.hasRemaining()) {
            int pos = bb.position();
            bb.getInt(); //EPB magic
            int epbLen = bb.getInt();
            bb.getInt(); //interface
            long hi = Integer.toUnsignedLong(bb.getInt());
            long lo = Integer.toUnsignedLong(bb.getInt());
            long ts = (hi << 32) | lo; // treating as epoch micros
            if (now == 0) { now = ts; }
            long elapsed = ts - now;
            now = ts;
            int capLen = bb.getInt();
            bb.getInt(); //orgLen
            ByteBuffer pbb = bb.slice(bb.position(), capLen);
            bbs.add(new UsbPacket(elapsed,pbb));
            bb.position(pos + epbLen);
        }

        return bbs;
    }


    public static void writeCrc(ByteBuffer buf, int start) {
        buf.limit(buf.position());
        buf.rewind();
        int crc = CRC16.crc16(buf, start, buf.limit()- start);
        buf.position(buf.limit());
        buf.limit(buf.position()+2);
        putShort(buf,crc);
    }

    public static <T> T with(T t,Consumer<T> f) { f.accept(t); return t; }

    public static <T> void forEach(Collection<T> coll, ThrowingConsumer<T> f) throws Exception {
        for (T t : coll) {
            f.accept(t);
        }
    }

    public static <K,V> void forEach(Map<K,V> map, ThrowingBiConsumer<K,V> f) throws Exception {
        for (Map.Entry<K, V> e : map.entrySet()) {
            f.accept(e.getKey(),e.getValue());
        }
    }

    public static <T,K,V> Map<K,V> buildMap(Map<K,V> seed,Collection<T> vals,BiConsumer<Map<K,V>,T> builder) {
        vals.forEach(t -> builder.accept(seed,t));
        return seed;
    }
}
