package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.util.Util;

public class UsbPerfSender {

    private final UsbSender sender;
    private final Performance perf;

    public UsbPerfSender(UsbSender sender, Performance perf) {
        this.sender = sender;
        this.perf = perf;
    }


    public int sendPerfRequest(String msg, int... cdata) throws Exception {
        return sender.sendBulk(msg, true, Util.concat(Util.asBytes(
                0x01,
                0x20 + 0x0c,// CMD_REQ + CMD_SYS
                perf.getVersion() // TODO this can probably be maintained here
        ),Util.asBytes(cdata)));
    }


}
