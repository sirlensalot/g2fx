package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.state.Patch;
import org.g2fx.g2lib.util.CRC16;
import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;

public class UsbSlotSender {

    private final UsbSender sender;
    private final Patch patch;

    public UsbSlotSender(UsbSender sender,Patch patch) {
        this.sender = sender;
        this.patch = patch;
    }

    /**
     * A slot request expects a response.
     */
    public int sendSlotRequest(String msg, int... cdata) throws Exception {
        return sendSlotRequest(msg, Util.asBytes(cdata));
    }

    private int sendSlotRequest(String msg, byte[] bytes) throws Exception {
        return sender.sendBulk(msg, true, Util.concat(Util.asBytes(
                0x01,
                0x20 + 0x08 + patch.getSlot().ordinal(), // CMD_REQ + CMD_SLOT + slot index
                patch.getVersion() // TODO can probably be managed here
        ), bytes));
    }

    /**
     * a slot command does not expect a response
     */
    public int sendSlotCommand(String msg, int... cdata) throws Exception {
        return sender.sendBulk(msg, false, Util.concat(Util.asBytes(
                0x01,
                0x30 + 0x08 + patch.getSlot().ordinal(), // CMD_NO_RESP + CMD_SLOT + slot index
                patch.getVersion()
        ),Util.asBytes(cdata)));
    }

    public ByteBuffer writeSectionMessage(Patch.Section s) throws Exception {

        ByteBuffer buf = ByteBuffer.allocateDirect(0xffff);

        Patch.writeSection(buf,s);

        buf.limit(buf.position());
        int crc = CRC16.crc16(buf.rewind());
        buf.position(buf.limit());
        buf.limit(buf.position()+2);
        Util.putShort(buf,crc);

        return buf;
    }

    public int sendSectionMessage(Patch.Section s) throws Exception {
        ByteBuffer buf = ByteBuffer.allocateDirect(0xffff);
        Patch.writeSection(buf,s);
        buf.limit(buf.position());
        return sendSlotRequest("sendSectionMessage:" + s.sections(),
                Util.getBytes(buf.rewind()));
    }

}
