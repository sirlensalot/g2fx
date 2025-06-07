package g2lib;

import g2lib.util.BitBuffer;
import g2lib.util.Util;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BitBufferTest {


    @Test
    void roundTrip() throws Exception {
        byte[] data = Util.asBytes(0x45, 0xf2);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Util.dumpBuffer(buffer);
        BitBuffer bb = new BitBuffer(buffer);
        assertEquals(17, bb.get(6));
        assertEquals(15, bb.get(5));
        assertEquals(1, bb.get(1));
        assertEquals(2, bb.get(4));

        BitBuffer bb2 = new BitBuffer(4);
        bb2.put(6, 17);
        bb2.put(5, 15);
        bb2.put(1, 1);
        bb2.put(4, 2);
        ByteBuffer buffer2 = bb2.toBuffer();
        Util.dumpBuffer(buffer2);
        byte[] data2 = new byte[buffer2.limit()];
        buffer2.get(data2);
        assertArrayEquals(data, data2);
    }

    @Test
    void roundTrip2() throws Exception {
        byte[] data = Util.asBytes(0x45, 0xf2, 0xe7);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Util.dumpBuffer(buffer);
        System.out.printf("%s %s %s\n", Util.asBinary(0x45), Util.asBinary(0xf2), Util.asBinary(0xe7));
        BitBuffer bb = new BitBuffer(buffer);
        assertEquals(17, bb.get(6));
        assertEquals(3991, bb.get(13));
        assertEquals(7, bb.get(5));

        buffer.rewind();
        int s = Util.getShort(buffer);
        ByteBuffer sb = ByteBuffer.allocate(2);
        Util.putShort(sb,s);
        Util.dumpBuffer(sb);

        BitBuffer bb2 = new BitBuffer(4);
        bb2.put(6, 17);
        bb2.put(13, 3991);
        bb2.put(5, 7);
        ByteBuffer buffer2 = bb2.toBuffer();
        Util.dumpBuffer(buffer2);
        byte[] data2 = new byte[buffer2.limit()];
        buffer2.get(data2);
        assertArrayEquals(data, data2);


    }

}
