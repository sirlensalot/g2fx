package org.g2fx.g2lib.state;

import org.g2fx.g2lib.usb.UsbSlotSender;

import static org.g2fx.g2lib.protocol.Codes.O_UNKNOWN6;

public class PatchLoadResponder {
    enum LoadResponseState {
        LoadMsgReceived,
        ResponseSent
    }
    private LoadResponseState state = LoadResponseState.ResponseSent;
    private final UsbSlotSender sender;

    public PatchLoadResponder(UsbSlotSender sender) {
        this.sender = sender;
    }

    public void loadMsgReceived() {
        state = LoadResponseState.LoadMsgReceived;
    }
    public void responseSent() {
        state = LoadResponseState.ResponseSent;
    }
    public boolean responseNeeded() {
        return state == LoadResponseState.LoadMsgReceived;
    }

    public void sendResponse() throws Exception {
        sender.sendSlotRequest("unknown 6", O_UNKNOWN6);
        responseSent();
    }

    public void sendResponseIfNeeded() throws Exception {
        if (responseNeeded()) { sendResponse(); }
    }
}
