package g2lib.state;

import g2lib.usb.Usb;
import g2lib.usb.UsbReadThread;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class DeviceTest {

    @Test
    public void initialize() throws Exception {
        Usb usb = mock(Usb.class);
        UsbReadThread readThread = mock(UsbReadThread.class);
        new Device(usb);

        //d.initialize();

    }
}
