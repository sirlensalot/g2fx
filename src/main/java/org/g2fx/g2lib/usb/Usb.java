package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.state.Slot;
import org.g2fx.g2lib.util.CRC16;
import org.g2fx.g2lib.util.Util;
import org.usb4java.BufferUtils;
import org.usb4java.LibUsb;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.g2fx.g2lib.usb.UsbService.ERRORS;

public class Usb {
    private static final Logger log = Util.getLogger(Usb.class);

    private final UsbService.UsbDevice device;
    private final UsbReadThread readThread;

    private MessageRecorder recorder;

    /**
     * Same-thread dispatcher
     */
    private Dispatcher dispatcher;

    public Usb(UsbService.UsbDevice device) {
        this.device = device;
        readThread = new UsbReadThread(this);
    }

    public void start() {
        readThread.start();
    }



    public synchronized int sendBulk(String msg, boolean dispatch, byte[] data) throws Exception {

        if (dispatch && dispatcher == null) {
            throw new IllegalArgumentException("sendBulk w dispatch but no dispatcher");
        }
        Future<UsbMessage> dispatchFuture = dispatch ? expect("dispatch future", m -> true) : null;

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

        if (dispatchFuture != null) {
            UsbMessage fm = dispatchFuture.get();
            try {
                dispatcher.dispatch(fm);
            } catch (Exception e) {
                log.severe("Failure dispatching message: " + fm);
                throw e;
            }
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
                final int fcrc = crc = CRC16.crc16(buffer, 1, dil - 2);
                log.info(() -> String.format("--------------- Read Interrupt embedded, crc: %x %x",
                        fcrc, buffer.position(dil - 1).getShort()) +
                        Util.dumpBufferString(buffer));

            }
            int size = buffer.position(1).getShort();
            if (extended) {
                log.info(() -> String.format("--------------- Read Interrupt extended, size: %x", size) +
                        Util.dumpBufferString(buffer));
            }
            UsbMessage m = new UsbMessage(size, extended, crc, buffer);
            if (!m.extended()) {
                record(m);
            }
            return m;
        }
    }

    public void startRecording(String sessionName, File dir) throws Exception {
        this.recorder = new MessageRecorder(sessionName,dir);
        log.info("Recording messages to " + recorder.getPath());
    }

    public void stopRecording() {
        log.info("Stopping recording to " + recorder.getPath());
        recorder.stop();
        this.recorder = null;
    }

    private UsbMessage record(UsbMessage msg) {
        if (recorder == null || msg == null) { return msg; }
        try {
            recorder.record(msg);
        } catch (Exception e) {
            log.log(Level.SEVERE,"Recording failed, stopping",e);
            stopRecording();
        }
        return msg;
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
                log.info(() -> String.format("--------------- Read Bulk size: %x crc: %x %x", tfrd, ecrc, buffer.position(len - 2).getShort()) +
                    Util.dumpBufferString(buffer));
                return record(new UsbMessage(size,true,ecrc,buffer));
            } else {
                return new UsbMessage(0,true,-1,null);
            }
        }
    }

    public int sendSystemRequest(String msg, int... cdata) throws Exception {
        return sendBulk(msg, true, Util.concat(Util.asBytes(
                0x01,
                0x20 + 0x0c,// CMD_REQ + CMD_SYS
                0x41
        ),Util.asBytes(cdata)));
    }

    public int sendPerfRequest(int perfVersion, String msg, int... cdata) throws Exception {
        return sendBulk(msg, true, Util.concat(Util.asBytes(
                0x01,
                0x20 + 0x0c,// CMD_REQ + CMD_SYS
                perfVersion
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
    public int sendSlotRequest(Slot slot, int version, String msg, int... cdata) throws Exception {
        return sendBulk(msg, true, Util.concat(Util.asBytes(
                0x01,
                0x20 + 0x08 + slot.ordinal(), // CMD_REQ + CMD_SLOT + slot index
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
    public int sendSlotCommand(int slot, int version, String msg, int... cdata) throws Exception {
        return sendBulk(msg, false, Util.concat(Util.asBytes(
                0x01,
                0x30 + 0x08 + slot, // CMD_NO_RESP + CMD_SLOT + slot index
                version
        ),Util.asBytes(cdata)));
    }

    public void shutdown() {

        readThread.shutdown();

        if (deviceInvalid()) { return; }

        log.info("Releasing handle");
        UsbService.retcode(LibUsb.releaseInterface(device.handle(),
                UsbService.IFACE), "Unable to release interface");

        log.info("Closing handle");
        LibUsb.close(device.handle());

    }

    public boolean deviceInvalid() {
        return device.handle().getPointer() == 0;
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

    public Future<UsbMessage> expect(String id, UsbReadThread.MsgP filter) {
        return readThread.expect(id,filter);
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void setThreadsafeDispatcher(Dispatcher dispatcher) {
        readThread.setDispatcher(dispatcher);
    }
}
