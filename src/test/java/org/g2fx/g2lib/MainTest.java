package org.g2fx.g2lib;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.g2fx.g2gui.panel.AreaPane;
import org.g2fx.g2gui.panel.MoveableModule;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.state.Coords;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.util.CRC16;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void crcClavia() throws  Exception {


        assertEquals(0x9188, CRC16.crc16(new byte[]{(byte) 0x80}, 0, 1));
        byte[] msg = Util.asBytes(
                0x80, 0x0a, 0x03, 0x00, 0x00, 0x1a, 0x00, 0x8c, 0x00, 0x12, 0x4d, 0x6f, 0x64, 0x75, 0x6c, 0x61,
                0x72, 0x47, 0x32, 0x00, 0x30, 0x03, 0x4c, 0x52, 0x00, 0x00, 0x01, 0x96, 0x28, 0x61, 0x00, 0x05,
                0x01, 0x0a, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        );
        assertEquals(0x3e24, CRC16.crc16(msg, 0, msg.length));

        assertEquals(0xb017, CRC16.crc16(Util.readTextColsByteBuffer(
                "01 28 01 21 00 0f 00 00 00 00 00 00 00 00 " +
                        "c2 58 4f c0 00 00 00"
        ).rewind()));
    }

    @Test
    void utils() throws Exception {

        assertEquals(8, Util.b2i((byte) 0x82) >> 4);

        assertEquals(0x1bd6, Util.addb((byte) 0x1b, (byte) 0xd6));

        assertArrayEquals(Util.asBytes(0,1,2,3,4,5),
                Util.concat(Util.asBytes(0,1,2),Util.asBytes(3,4),Util.asBytes(5)));

    }

    @Test
    void modPages() throws Exception {

        assertEquals("S&H", ModuleType.M_S_and_H.shortName);
        assertEquals("T&H", ModuleType.M_T_and_H.shortName);
        assertEquals("2-In",ModuleType.M_2_In.shortName);

        TreeSet<ModuleType.ModPageIx> ixs = new TreeSet<>();
        for (ModuleType mt : ModuleType.values()) {
            if (!ixs.add(mt.modPageIx)) {
                fail("Bad mod page ix: " + mt);
            }
        }

        int i = -1;
        ModuleType.ModPage page = null;
        for (ModuleType.ModPageIx ix : ixs) {
            if (ix.page() != page) {
                page = ix.page();
                i = ix.ix();
                if (i != 0) {
                    fail("Bad starting index: " + ix);
                }

            } else {
                if (++i != ix.ix()) {
                    fail("Non-monotonic ix: " + ix);
                }
            }
        }


    }
    @Test
    void loadRecording() throws Exception {
        List<MessageRecorder.RecordedUsbMessage> msgs = MessageRecorder.readSessionFile(new File("data/recording1.yaml"));
        assertEquals(192,msgs.size());
    }

    @Test
    void coords() {
        assertEquals(List.of(new Coords(0,0),
                new Coords(0,1),
                new Coords(1,0),
                new Coords(1,1)),
                Stream.of(new Coords(1,0),
                new Coords(0,0),
                new Coords(0,1),
                new Coords(1,1)).sorted().toList());
    }


    record MockMoveable(Property<Coords> coords, int getHeight, boolean isSelected) implements MoveableModule {
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MoveableModule c) {
                return coords.getValue().equals(c.coords().getValue()) && getHeight == c.getHeight() && isSelected == c.isSelected();
            }
            return false;
        }
    }

    @Test
    void moveModule() {
        {
            //trivial
            List<MockMoveable> modules = List.of(mkModule(0, 0, 1, true));
            AreaPane.resolveCollisions(modules);
            assertEquals(List.of(mkModule(0, 0, 1, true)),modules);
        }
        {
            //2 iso overlap, selected vs non
            List<MockMoveable> modules = List.of(
                    mkModule(0,0,1,true),
                    mkModule(0,0,1,false));
            AreaPane.resolveCollisions(modules);
            assertEquals(List.of(
                    mkModule(0,0,1,true),
                    mkModule(0,1,1,false)),
                    modules);
        }
        {
            //2 diff overlap, selected vs non
            List<MockMoveable> modules = List.of(
                    mkModule(0,0,1,true),
                    mkModule(0,0,2,false));
            AreaPane.resolveCollisions(modules);
            assertEquals(List.of(
                            mkModule(0,0,1,true),
                            mkModule(0,2,2,false)),
                    modules);
        }
    }

    private static MockMoveable mkModule(int column, int row, int height, boolean selected) {
        return new MockMoveable(new SimpleObjectProperty<>(new Coords(column, row)), height, selected);
    }


}
