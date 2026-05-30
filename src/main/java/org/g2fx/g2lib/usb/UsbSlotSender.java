package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.protocol.Codes;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.state.Patch;
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

    public int sendSlotRequest(String msg, byte[] bytes) throws Exception {
        return sender.sendBulk(msg, true, Util.concat(Util.asBytes(
                Codes.M_CMD,
                Codes.S_SLOT_REQ + patch.getSlot().ordinal(), // CMD_REQ + CMD_SLOT + slot index
                patch.getVersion()
        ), bytes));
    }

    /**
     * a slot command does not expect a response
     */
    public int sendSlotCommand(String msg, int... cdata) throws Exception {
        return sender.sendBulk(msg, false, Util.concat(Util.asBytes(
                Codes.M_CMD,
                Codes.S_SLOT_CMD + patch.getSlot().ordinal(),
                patch.getVersion()
        ),Util.asBytes(cdata)));
    }

    public int sendSectionMessage(Sections.Section s) throws Exception {
        ByteBuffer buf = ByteBuffer.allocateDirect(0xffff);
        Sections.writeSection(buf,s);
        buf.limit(buf.position());
        return sendSlotRequest("sendSectionMessage:" + s.sections(),
                Util.getBytes(buf.rewind()));
    }

    public UsbSender getSender() {
        return sender;
    }
}
