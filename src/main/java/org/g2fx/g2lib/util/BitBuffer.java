package org.g2fx.g2lib.util;

import org.usb4java.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BitBuffer {
    private final ByteBuffer buffer;
    private int bpos;

    public BitBuffer(ByteBuffer buffer) {
        this(buffer,buffer.position());
    }

    private BitBuffer(ByteBuffer buffer, int position) {
        this.buffer = buffer;
        bpos = position*8;
    }

    public BitBuffer(int capacity) {
        this(BufferUtils.allocateByteBuffer(capacity));
    }

    /**
     * Make a new BitBuffer from a buffer that already has content,
     * to write from current position.
     */
    public static BitBuffer fromSlice(ByteBuffer buf) {
        int capacity = buf.capacity();
        int pos = buf.position();
        ByteBuffer slice = buf.slice(0,capacity);
        slice.position(pos);
        BitBuffer bb = new BitBuffer(slice,pos);
        return bb;
    }

    /**
     * Allocate a fresh ByteBuffer and write to it with BitBuffer, returning buffer
     * trimmed to the data written.
     */
    public static ByteBuffer writeBitBuffer(Util.ThrowingConsumer<BitBuffer> f) throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(0xffff);
        BitBuffer bb = fromSlice(buf);
        f.accept(bb);
        buf.limit(bb.getBytePosition());
        return buf;
    }

    public int getBytePosition() {
        return Math.ceilDiv(bpos, 8);
    }

    public int getBitPosition() { return bpos; }

    //READ
    public int getBitsRemaining() { return getBitLimit() - bpos; }

    public int getBitLimit() { return buffer.limit() * 8; }

    public int get() {
        return get(8);
    }

    public int peek(int len) {
        int bi = bpos;
        int r = get(len);
        bpos = bi;
        return r;
    }


    public int get(int len) {
        if (bpos + len > getBitLimit()) {
            throw new IllegalArgumentException
                    (String.format("underflow: %d %d %d", len, bpos, getBitLimit()));
        }
        int pos = bpos / 8;
        int b0 = Util.b2i(buffer.get(pos));
        int off = bpos % 8;
        bpos += len;

        int omask = 0xff >> off;
        int r = b0 & omask;
        int rem0 = 8 - off;
        if (len <= rem0) {
            return r >> (rem0 - len);
        }
        len -= rem0;
        while (len > 0) {
            int b1 = Util.b2i(buffer.get(++pos));
            if (len <= 8) {
                int rem1 = 8 - len;
                int b1s = b1 >> rem1;
                int rs = r << len;
                return rs | b1s;
            }
            len -= 8;
            int rs = r << 8;
            r = rs | b1;
        }
        throw new RuntimeException("Loop failure");
    }


    public void put(int len, int val) {
        if (val >= Math.pow(2, len)) {
            throw new IllegalArgumentException("invalid val for len: " + val + ", " + len);
        }
        int pos = bpos / 8;
        int off = bpos % 8;
        bpos += len;
        int b0;
        if (off > 0) {
            b0 = Util.b2i(buffer.get(pos));
        } else {
            b0 = 0;
            buffer.put(pos,(byte) b0);
        }
        int end = off + len;
        if (end <= 8) {
            int rem = 8 - end;
            int vs = val << rem;
            int r = b0 | vs;
            buffer.put(pos,(byte) r);
            return;
        }



        int rem = end - 8;
        int v0 = val >> rem;
        int r = b0 | v0;
        buffer.put(pos,(byte) r);
        while (rem > 0) {
            if (rem <= 8) {
                int v1 = (val << (8-rem)) & 0xff;
                buffer.put(++pos,(byte) v1);
                return;
            }

            rem = rem - 8;
            int r1 = (val >> rem) & 0xff;
            buffer.put(++pos,(byte) r1);
        }


    }

    //WRITE
    public ByteBuffer toBuffer() {
        return buffer.duplicate().limit(getBytePosition()).rewind();
    }

    //READ
    public ByteBuffer slice() {
        int pos = bpos /8;
        return buffer.slice(pos,buffer.limit()-pos);
    }

    /**
     * Adapt {@link Util#sliceAhead(ByteBuffer, int)} sliceAhead} with
     * BitBuffer made from slice.
     */
    //READ
    public static BitBuffer sliceAhead(ByteBuffer buffer, int length) {
        ByteBuffer slice = Util.sliceAhead(buffer, length);
        BitBuffer bb = new BitBuffer(slice);
        return bb;
    }

    /**
     * Move write head to byte boundary.
     */
    public void padToByte() {
        bpos = getBytePosition()*8;
    }

    /**
     * Write G2 length short to buffer.
     */
    public void writeLength(int pos, int len) {
        ByteOrder o = buffer.order();
        buffer.order(ByteOrder.BIG_ENDIAN); //TODO maybe we can just leave this?
        buffer.putShort(pos, (short) len);
        buffer.order(o);
    }

    /**
     * {@link #sliceAhead(ByteBuffer, int)} by G2 length short at position.
     */
    //READ
    public static BitBuffer sliceAheadLength(ByteBuffer buf) {
        return sliceAhead(buf,Util.getShort(buf));
    }



    }
