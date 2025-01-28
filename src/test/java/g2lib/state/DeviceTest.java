package g2lib.state;

import g2lib.Util;
import g2lib.usb.Usb;
import g2lib.usb.UsbMessage;
import g2lib.usb.UsbReadThread;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static g2lib.usb.UsbReadThread.*;

class DeviceTest {

    @Test
    public void initialize() throws Exception {
        Usb usb = mock(Usb.class);
        UsbReadThread readThread = mock(UsbReadThread.class);
        Device d = new Device(usb,readThread);
        when(readThread.expectBlocking(eq("perf version"),any(MsgP.class))).thenReturn(
                new UsbMessage(0,false,0, Util.readFile("data/msg_PerfVersion_1bd6.msg").position(6)));
        when(readThread.expectBlocking(eq("Synth settings"),any(MsgP.class))).thenReturn(
                new UsbMessage(0,false,0, Util.readFile("data/msg_SynthSettings_f574.msg").position(4)));
        //d.initialize();

    }
}