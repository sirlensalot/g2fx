package g2lib.usb;

import g2lib.Util;

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

    public boolean head(int... values) {
        return test(0,values);
    }

    public boolean test(int index,int... values) {
        if (buffer.limit() > index + values.length) {
            for (int i = 0; i < values.length; i++) {
                byte b = buffer.get(index + i);
                if (b != (byte) values[i]) {
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
}

