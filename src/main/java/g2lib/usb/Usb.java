package g2lib.usb;

import g2lib.CRC16;
import g2lib.Main;
import g2lib.Util;
import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static g2lib.usb.UsbService.ERRORS;

public class Usb {
    private static final Logger log = Util.getLogger(Usb.class);

    private final UsbService.UsbDevice device;

    public Usb(UsbService.UsbDevice device) {
        this.device = device;
    }


//    /**
//     * only supports 1 connected device
//     */
//    static Device getG2Device(Context context) {
//        // Read the USB device list
//        final DeviceList list = new DeviceList();
//
//        retcode(LibUsb.getDeviceList(context, list), "Unable to get device list");
//
//        // Iterate over all devices and dump them
//        for (Device device : list) {
//            final DeviceDescriptor descriptor = new DeviceDescriptor();
//            retcode(LibUsb.getDeviceDescriptor(device, descriptor), "Unable to read device descriptor");
//            if (descriptor.idVendor() == VENDOR_ID && descriptor.idProduct() == PRODUCT_ID) {
//                //dumpDevice(device);
//                return device;
//            } else {
//                LibUsb.unrefDevice(device);
//            }
//        }
//        return null;
//    }






    public synchronized int sendBulk(String msg, byte[] data) {


        int size = data.length + 4;
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(size);
        buffer.put((byte) (size / 256));
        buffer.put((byte) (size % 256));
        buffer.put(data);
        int crc = CRC16.crc16(data, 0, data.length);
        //dumpBytes(data);
        log.info(String.format("send crc: %x %x %x", crc, crc / 256, crc % 256));
        buffer.put((byte) (crc / 256));
        buffer.put((byte) (crc % 256));
        log.info(String.format("--------------- Send Bulk: %s ----------------", msg) + Util.dumpBufferString(buffer));
        IntBuffer transferred = BufferUtils.allocateIntBuffer();
        int r = LibUsb.bulkTransfer(device.handle(), (byte) 0x03, buffer, transferred, 10000);
        if (r >= 0) {
            //transferred.rewind();
            log.info("Sent: " + transferred.get(0));
        }
        return transferred.get();
    }

    @SuppressWarnings("unused")
    public UsbMessage readInterruptRetry() {
        UsbMessage r = new UsbMessage(-1,false,-1,null);
        for (int i = 0; i < 5; i++) {
            r = readInterrupt(2000);
            if (r.success()) { return r; }
        }
        log.info("Interrupt retries exhausted");
        return r;
    }
    public UsbMessage readInterrupt(int timeout) {
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(16);
        IntBuffer transferred = BufferUtils.allocateIntBuffer();
        int r = LibUsb.interruptTransfer(device.handle(), (byte) 0x81, buffer, transferred, timeout);
        if (r < 0) {
            if (r != -7) { //timeout
                log.info(String.format("--------------- Read Interrupt failure: %s ----------------",
                        ERRORS.get(r)));
            }
            return new UsbMessage(r,false,-1,null);
        } else {
            int type = buffer.get(0) & 0xf;
            boolean extended = type == 1;
            //String s = Util.dumpBufferString(buffer);
            boolean embedded = type == 2;
            int crc = 0;
            if (embedded) {
                int dil = (buffer.get(0) & 0xf0) >> 4;
                crc = CRC16.crc16(buffer, 1, dil - 2);
                log.info(String.format("--------------- Read Interrupt embedded, crc: %x %x", crc, buffer.position(dil - 1).getShort()) +
                        Util.dumpBufferString(buffer));

            }
            int size = buffer.position(1).getShort();
            if (extended) {
                log.info(String.format("--------------- Read Interrupt extended, size: %x", size) +
                        Util.dumpBufferString(buffer));
            }
            return new UsbMessage(size,extended,crc,buffer);
        }
    }

    public UsbMessage readBulkRetries(int size, int retries) {
        UsbMessage r = new UsbMessage(-1,true,-1,null);
        for (int i = 0; i < retries; i++) {
            r = readBulk(size);
            if (r.success()) {
                return r;
            }
        }
        return r;
    }

    public UsbMessage readBulk(int size) {
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(size);
        IntBuffer transferred = BufferUtils.allocateIntBuffer();
        int r = LibUsb.bulkTransfer(device.handle(), (byte) 0x82, buffer, transferred, 5000);
        if (r < 0) {
            log.info("--------------- Read Bulk failure: " + ERRORS.get(r) + " ---------------");
            return new UsbMessage(r,true,-1,null);
        } else {
            int tfrd = transferred.get();
            if (tfrd > 0) {
                // buffer.rewind();
                int len = buffer.limit();
                //dumpBytes(recd);
                int ecrc = CRC16.crc16(buffer, 0, len - 2);
                log.info(String.format("--------------- Read Bulk size: %x crc: %x %x", tfrd, ecrc, buffer.position(len - 2).getShort()) +
                    Util.dumpBufferString(buffer));
                return new UsbMessage(size,true,ecrc,buffer);
            } else {
                return new UsbMessage(0,true,-1,null);
            }
        }
    }

    public int sendSystemCmd(String msg, int... cdata) {
        return sendBulk(msg,Util.concat(Util.asBytes(
                0x01,
                0x20 + 0x0c,// CMD_REQ + CMD_SYS
                0x41
                ),Util.asBytes(cdata)));
    }

    /**
     * A slot request expects a response.
     * @param slot 0-3
     * @param version patch version
     * @param msg log msg
     * @param cdata request data
     * @return success code
     */
    public int sendSlotRequest(int slot, int version, String msg, int... cdata) {
        return sendBulk(msg,Util.concat(Util.asBytes(
                0x01,
                0x20 + 0x08 + slot, // CMD_REQ + CMD_SLOT + slot index
                version
                ),Util.asBytes(cdata)));
    }

    /**
     * a slot command does not expect a response
     * @param slot 0-3
     * @param version patch version
     * @param msg log msg
     * @param cdata cmd data
     * @return success code
     */
    public int sendSlotCommand(int slot, int version, String msg, int... cdata) {
        return sendBulk(msg,Util.concat(Util.asBytes(
                0x01,
                0x30 + 0x08 + slot, // CMD_NO_RESP + CMD_SLOT + slot index
                version
        ),Util.asBytes(cdata)));
    }
    /*
S_SET_PARAM :
  begin
    Size := 13;
    [ 0] := 0; // size msb
    [ 1] := Size; // size lsb
    [ 2] := $01;
    [ 3] := CMD_NO_RESP + CMD_SLOT + SlotIndex; // CMD_NO_RESP = 0x30, CMD_SLOT = 0x08
    [ 4] := Slot.PatchVersion; // Current patch version!
    [ 5] := S_SET_PARAM; // 0x40
    [ 6] := Slot.FParamUpdBuf[i].Location;
    [ 7] := Slot.FParamUpdBuf[i].Module;
    [ 8] := Slot.FParamUpdBuf[i].Param;
    [ 9] := Slot.FParamUpdBuf[i].Value;
    [10] := Slot.FParamUpdBuf[i].Variation;
  end;
S_SEL_PARAM :
  begin
    Size := 12;
  [ 0] := 0;
  [ 1] := Size;
  [ 2] := $01;
  [ 3] := CMD_NO_RESP + CMD_SLOT + SlotIndex; // CMD_NO_RESP = 0x30, CMD_SLOT = 0x08
  [ 4] := Slot.PatchVersion; // Current patch version!
  [ 5] := S_SEL_PARAM; // 0x2f
  [ 6] := 00;
  [ 7] := Slot.FParamUpdBuf[i].Location;
  [ 8] := Slot.FParamUpdBuf[i].Module;
  [ 9] := Slot.FParamUpdBuf[i].Param;
  end;
S_SET_MORPH_RANGE :
  begin
    Size := 15;
   [ 0] := 0;
   [ 1] := Size;
   [ 2] := $01;
   [ 3] := CMD_NO_RESP + CMD_SLOT + SlotIndex; // CMD_NO_RESP = 0x30, CMD_SLOT = 0x08
   [ 4] := Slot.PatchVersion; // Current patch version!
   [ 5] := S_SET_MORPH_RANGE; // 0x43
   [ 6] := Slot.FParamUpdBuf[i].Location;
   [ 7] := Slot.FParamUpdBuf[i].Module;
   [ 8] := Slot.FParamUpdBuf[i].Param;
   [ 9] := Slot.FParamUpdBuf[i].Morph;
   [10] := Slot.FParamUpdBuf[i].Value;
   [11] := Slot.FParamUpdBuf[i].Negative;
   [12] := Slot.FParamUpdBuf[i].Variation;
     */



}
