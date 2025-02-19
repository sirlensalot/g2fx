package g2lib.usb;

import g2lib.util.Util;

import java.nio.ByteBuffer;

public record UsbMessage(int size, boolean extended, int crc, ByteBuffer buffer) {

    public boolean success() {
        return size > 0 && buffer != null;
    }

    public String dump() {
        return String.format("%s size=%x crc=%x %s",
                extended ? "extended" : "embedded",
                size, crc, Util.dumpBufferString(buffer));
    }

    @Override
    public String toString() {
        return String.format("UsbMessage[crc=%x, extended=%s, size=%s]",crc,extended,size);
    }

    public boolean head(int... values) {
        boolean test = test(0, values);
        buffer.position(values.length); // side-effect for easier parsing
        return test;
    }

    public boolean headx(int... values) {
        int off = extended ? 0 : 1;
        boolean test = test(off, values);
        buffer.position(values.length + off); // side-effect for easier parsing
        return test;
    }

    public boolean test(int index,int... values) {
        if (buffer.limit() > index + values.length) {
            for (int i = 0; i < values.length; i++) {
                int b = Util.b2i(buffer.get(index + i));
                //String s = Util.dumpBufferString(buffer);
                if (b != values[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /*
    extended: 80 0a 03 00 -- 80/hello machine
    embedded: 82 01 0c 40 36 04 -- perf version
    embedded: 62 01 0c 00 7f -- 62 01 (stop message)
    extended: 01 0c 00 03 -- synth settings [03]
    extended: 01 0c 00 80 -- 80/"unknown 1" (slot hello?)
    extended: 01 0c 00 29 -- perf settings [29 "perf name"]
    embedded: 72 01 0c 00 1e -- "unknown 2" 1e?
    embedded: 82 01 0c 40 36 01 -- slot version
    extended: 01 09 00 21 -- patch description, slot 1
    extended: 01 09 00 27 -- patch name, slot 1
    extended: 01 09 00 69 -- cable list, slot 1
    extended: 01 09 00 6f -- textpad, slot 1
     */

    public ByteBuffer getBufferx() {
        return buffer.position(extended ? 0 : 1);
    }
}
