package g2lib.usb;

import g2lib.CRC16;
import g2lib.Util;
import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

public class Usb {
    public static final int VENDOR_ID = 0xffc;
    public static final int PRODUCT_ID = 2;
    public static final int IFACE = 0;
    public static final Map<Integer, String> ERRORS = errorMap();

    private final Context context;
    private final Device device;
    private final DeviceHandle handle;

    private final Logger log = Util.getLogger(getClass());

    public Usb(Context context, Device device, DeviceHandle handle) {
        this.context = context;
        this.device = device;
        this.handle = handle;
    }
    

    private static Map<Integer, String> errorMap() {
        TreeMap<Integer, String> m = new TreeMap<>();
        m.put(-3, "ERROR_ACCESS");
        m.put(-6, "ERROR_BUSY");
        m.put(14, "ERROR_COUNT");
        m.put(-10, "ERROR_INTERRUPTED");
        m.put(-2, "ERROR_INVALID_PARAM");
        m.put(-1, "ERROR_IO");
        m.put(-4, "ERROR_NO_DEVICE");
        m.put(-11, "ERROR_NO_MEM");
        m.put(-5, "ERROR_NOT_FOUND");
        m.put(-12, "ERROR_NOT_SUPPORTED");
        m.put(-99, "ERROR_OTHER");
        m.put(-8, "ERROR_OVERFLOW");
        m.put(-9, "ERROR_PIPE");
        m.put(-7, "ERROR_TIMEOUT");
        return m;
    }

    static void retcode(int result, String msg) {
        if (result < 0) {
            throw new LibUsbException(msg + ": " + ERRORS.get(result), result);
        }
    }

    /**
     * only supports 1 connected device
     */
    static Device getG2Device(Context context) {
        // Read the USB device list
        final DeviceList list = new DeviceList();

        retcode(LibUsb.getDeviceList(context, list), "Unable to get device list");

        // Iterate over all devices and dump them
        for (Device device : list) {
            final DeviceDescriptor descriptor = new DeviceDescriptor();
            retcode(LibUsb.getDeviceDescriptor(device, descriptor), "Unable to read device descriptor");
            if (descriptor.idVendor() == VENDOR_ID && descriptor.idProduct() == PRODUCT_ID) {
                //dumpDevice(device);
                return device;
            } else {
                LibUsb.unrefDevice(device);
            }
        }
        return null;
    }

    public static Usb initialize() {
        // Create the libusb context
        final Context context = new Context();

        // Initialize the libusb context
        Usb.retcode(LibUsb.init(context),"Unable to initialize libusb");

        Device device = Usb.getG2Device(context);
        if (device == null) {
            throw new RuntimeException("No G2 device found");
        }

        DeviceHandle handle = new DeviceHandle();
        Usb.retcode(LibUsb.open(device, handle), "Unable to acquire handle");

        Usb.retcode(LibUsb.claimInterface(handle, Usb.IFACE), "Unable to claim interface");

        return new Usb(context,device,handle);
    }

    public void shutdown() {

        Usb.retcode(LibUsb.releaseInterface(handle, Usb.IFACE), "Unable to release interface");

        LibUsb.close(handle);

        LibUsb.unrefDevice(device);

        // Deinitialize the libusb context
        LibUsb.exit(context);
    }

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
        int r = LibUsb.bulkTransfer(handle, (byte) 0x03, buffer, transferred, 10000);
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
        int r = LibUsb.interruptTransfer(handle, (byte) 0x81, buffer, transferred, timeout);
        if (r < 0) {
            if (r != -7) { //timeout
                log.info(String.format("--------------- Read Interrupt failure: %s ----------------",
                        ERRORS.get(r)));
            }
            return new UsbMessage(r,false,-1,null);
        } else {
            int type = buffer.get(0) & 0xf;
            boolean extended = type == 1;
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
        int r = LibUsb.bulkTransfer(handle, (byte) 0x82, buffer, transferred, 5000);
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


    /**
     * Dumps the specified device to stdout.
     *
     * @param device The device to dump.
     */
    @SuppressWarnings("unused")
    public static void dumpDevice(final Device device) {
        // Dump device address and bus number
        final int address = LibUsb.getDeviceAddress(device);
        final int busNumber = LibUsb.getBusNumber(device);
        System.out.printf("Device %03d/%03d%n", busNumber, address);

        // Dump port number if available
        final int portNumber = LibUsb.getPortNumber(device);
        if (portNumber != 0)
            System.out.println("Connected to port: " + portNumber);

        // Dump parent device if available
        final Device parent = LibUsb.getParent(device);
        if (parent != null) {
            final int parentAddress = LibUsb.getDeviceAddress(parent);
            final int parentBusNumber = LibUsb.getBusNumber(parent);
            System.out.printf("Parent: %03d/%03d%n",
                    parentBusNumber, parentAddress);
        }

        // Dump the device speed
        System.out.println("Speed: "
                + DescriptorUtils.getSpeedName(LibUsb.getDeviceSpeed(device)));

        // Read the device descriptor
        final DeviceDescriptor descriptor = new DeviceDescriptor();
        retcode(LibUsb.getDeviceDescriptor(device, descriptor), "Unable to read device descriptor");

        // Try to open the device. This may fail because user has no
        // permission to communicate with the device. This is not
        // important for the dumps, we are just not able to resolve string
        // descriptor numbers to strings in the descriptor dumps.
        DeviceHandle handle = new DeviceHandle();
        retcode(LibUsb.open(device, handle), "Unable to open device");


        // Dump the device descriptor
        System.out.print(descriptor.dump(handle));

        // Dump all configuration descriptors
        dumpConfigurationDescriptors(device, descriptor.bNumConfigurations());

        // Close the device if it was opened
        LibUsb.close(handle);

    }

    public static void dumpConfigurationDescriptors(final Device device,
                                                    final int numConfigurations) {
        for (byte i = 0; i < numConfigurations; i += 1) {
            final ConfigDescriptor descriptor = new ConfigDescriptor();
            retcode(LibUsb.getConfigDescriptor(device, i, descriptor), "Unable to read config descriptor");
            try {
                System.out.println(descriptor.dump().replaceAll("(?m)^",
                        "  "));
            } finally {
                // Ensure that the config descriptor is freed
                LibUsb.freeConfigDescriptor(descriptor);
            }
        }
    }
}
