package g2lib.usb;

public interface Dispatcher {
    boolean dispatch(UsbMessage msg);
}
