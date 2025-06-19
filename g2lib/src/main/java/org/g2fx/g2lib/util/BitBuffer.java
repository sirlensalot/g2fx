package org.g2fx.g2lib.util;

import org.usb4java.BufferUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class BitBuffer {
    private final ByteBuffer buffer;
    private int bindex = 0;
    private int blength;

    public BitBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
        this.blength = buffer.limit() * 8;
    }

    public BitBuffer(int capacity) {
        this.buffer = BufferUtils.allocateByteBuffer(capacity);
        buffer.limit(0);
        blength = 0;
    }

    public int limit() { return buffer.limit(); }

    public int getBitLength() { return blength; }
    public int getBitIndex() { return bindex; }
    public int getBitsRemaining() { return blength - bindex; }

    public int get() {
        return get(8);
    }

    public int peek(int len) {
        int bi = bindex;
        int bl = blength;
        int r = get(len);
        bindex = bi;
        blength = bl;
        return r;
    }


    public int get(int len) {
        if (bindex + len > blength) {
            throw new IllegalArgumentException
                    (String.format("underflow: %d %d %d", len, bindex, blength));
        }
        int pos = bindex / 8;
        int b0 = Util.b2i(buffer.get(pos)); // 00110101
        int off = bindex % 8;               // 3
        bindex += len;

        int omask = 0xff >> off;             // 11111111 -> 00011111
        int r = b0 & omask;              //  00010101
        int rem0 = 8 - off;         // 5
        if (len <= rem0) { // len=4
            return r >> (rem0 - len);  // 00001010
        }
        len -= rem0;    //len=7 => 2, len=15 => 10
        while (len > 0) {
            int b1 = Util.b2i(buffer.get(++pos)); // 10011010, 11110000
            if (len <= 8) { // len=2
                int rem1 = 8 - len; // 6
                int b1s = b1 >> rem1; // 00000010, 00000011
                int rs = r << len; //   01010100,  00 01010110 01101000
                return rs | b1s;     // 01010110,  00 01010110 01101011
            }
            len -= 8; // len=10 => 2
            int rs = r << 8; // 00010101 00000000
            r = rs | b1;   // 00010101 10011010
        }
        throw new RuntimeException("Loop failure");
    }


    public void put(int len, int val) {
        if (val >= Math.pow(2, len)) {
            throw new IllegalArgumentException("invalid val for len: " + val + ", " + len);
        }
        int pos = bindex / 8;
        int off = bindex % 8; // 3
        bindex += len;
        blength += len;
        int b0;
        if (off > 0) {
            b0 = Util.b2i(buffer.get(pos)); // 10100000
        } else {
            b0 = 0;
            buffer.limit(buffer.limit() + 1);
            buffer.put((byte) b0);
        }
        int end = off + len; // off=3,len=4,val=00001010 => 7; off=0 => 4
        if (end <= 8) {
            int rem = 8 - end; // 1; 4
            int vs = val << rem;  //00010100; 10100000
            int r = b0 | vs; // 10110100; 10100000
            buffer.put(pos,(byte) r);
            return;
        }
        // off=3,len=7,end=10,val=01101101, r=10111011, v0=00011011
        // off=3,len=11,end=14,val=00000110 01101101, r=10111001, v0=00011001
        // off=3,len=15,end=18,val=01011010 11010110, r=10110110, v0=00010110
        int rem = end - 8; //2; 6; 10
        int v0 = val >> rem; // 00011011; 00011001; 00010110
        int r = b0 | v0; // 10111011; 10111001; 10110110
        buffer.put(pos,(byte) r);
        while (rem > 0) {
            if (rem <= 8) { // len=7,rem=2;len=11,rem=6;len=15,rem=2
                int v1 = (val << (8-rem)) & 0xff; // 01000000; 01000000; 10000000
                buffer.limit(buffer.limit()+1);
                buffer.put((byte) v1);
                return;
            }
            // len=15,rem=10,val=01011010 11010110,r=10110101,r=10 110101
            rem = rem - 8; // 2
            int r1 = (val >> rem) & 0xff; //10110101
            buffer.limit(buffer.limit()+1);
            buffer.put((byte) r1);
        }


    }

    public BitBuffer rewind() {
        bindex = 0;
        return this;
    }

    public ByteBuffer toBuffer() {
        return buffer.duplicate().rewind();
    }

    public ByteBuffer slice() {
        int pos = bindex/8;
        return buffer.slice(pos,buffer.limit()-pos);
    }

    public ByteBuffer shiftedSlice() {
        ByteBuffer buf = slice();
        int rem = bindex % 8;
        if (rem == 0) { return buf; }
        return shiftedBuffer(buf, rem);
    }

    public static ByteBuffer shiftedBuffer(ByteBuffer buf, int rem) {
        buf.rewind();
        byte[] b = new byte[buf.limit()];
        buf.get(b);
        BigInteger i = new BigInteger(b).shiftLeft(rem);
        return ByteBuffer.wrap(i.toByteArray());
    }

    public static BitBuffer sliceAhead(ByteBuffer buffer, int length) {
        ByteBuffer slice = Util.sliceAhead(buffer, length);
        BitBuffer bb = new BitBuffer(slice);
        return bb;
    }

}
