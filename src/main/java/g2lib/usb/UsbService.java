package g2lib.usb;


import g2lib.Util;
import org.usb4java.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class UsbService implements Runnable, HotplugCallback {

    private static final Logger log = Util.getLogger(UsbService.class);


    public static final int VENDOR_ID = 0xffc;
    public static final int PRODUCT_ID = 2;
    public static final int IFACE = 0;
    public static final Map<Integer, String> ERRORS = errorMap();


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

    public record UsbDevice(int address, Device device, DeviceHandle handle) {

    }


    public interface UsbConnectionListener {
        public void onConnectionEvent(UsbDevice device, boolean connected);
    }

    private final Context context = new Context();

    private volatile boolean running = true;
    private Thread thread = new Thread(this);

    private List<UsbConnectionListener> listeners =
            new CopyOnWriteArrayList<>();

    private HotplugCallbackHandle callbackHandle = new HotplugCallbackHandle();

    private final Map<Integer, UsbDevice> devices = new HashMap<>();


    public UsbService() {
        retcode(LibUsb.init(context),"Unable to initialize libusb");
    }

    public void start() { thread.start(); }

    public void stop() throws Exception {
        running = false;
        thread.join();
    }
    @Override
    public void run() {
        while (running) {
            int result = LibUsb.handleEventsTimeout(null, 250000);
            if (result != LibUsb.SUCCESS)
                throw new LibUsbException("Unable to handle events", result);
        }
    }

    public void addListener(UsbConnectionListener listener) {
        log.info("addListener");
        listeners.add(listener);
    }

    public void startListener() {
        LibUsb.hotplugRegisterCallback(context,
                LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED |
                        LibUsb.HOTPLUG_EVENT_DEVICE_LEFT,
                LibUsb.HOTPLUG_ENUMERATE,
                VENDOR_ID,
                PRODUCT_ID,
                LibUsb.HOTPLUG_MATCH_ANY,
                this,null,callbackHandle
        );
    }

    public void stopListener() {
        LibUsb.hotplugDeregisterCallback(context, this.callbackHandle);
    }

    public void shutdown() {
        LibUsb.exit(context);
    }


    @Override
    public int processEvent(Context context,
                            Device device,
                            int event,
                            Object userData) {
        int address = LibUsb.getDeviceAddress(device);
        if (event == LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED) {
            log.info("Device connected: " + address);
            DeviceHandle handle = new DeviceHandle();
            retcode(LibUsb.open(device, handle), "Unable to acquire handle");
            retcode(LibUsb.claimInterface(handle, IFACE), "Unable to claim interface");
            UsbDevice ud = new UsbDevice(address, device, handle);
            devices.put(address, ud);
            listeners.forEach(l -> l.onConnectionEvent(ud,true));
        } else {
            log.info("Device disconnected: " + address);
            UsbDevice ud = devices.get(address);
            if (ud != null) {
                listeners.forEach(l -> l.onConnectionEvent(ud, false));
                retcode(LibUsb.releaseInterface(ud.handle, IFACE), "Unable to release interface");
                LibUsb.close(ud.handle);
            }
        }
        return 0;
    }


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


    public static void retcode(int result, String msg) {
        if (result < 0) {
            throw new LibUsbException(msg + ": " + ERRORS.get(result), result);
        }
    }

}
