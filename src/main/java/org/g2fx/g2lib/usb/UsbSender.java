package org.g2fx.g2lib.usb;

import org.g2fx.g2lib.util.Util;

import java.nio.ByteBuffer;

public interface UsbSender {

    int sendBulk(String msg, boolean dispatch, byte[] data) throws Exception;

    int sendSystemRequest(String msg, int... cdata) throws Exception;

    public class OfflineSender implements UsbSender {

        @Override
        public int sendBulk(String msg, boolean dispatch, byte[] data) throws Exception {
            System.out.format("sendBulk: %s %s %s\n",msg,dispatch, Util.dumpBufferString(ByteBuffer.wrap(data)));
            return 1; // > 0 means OK
        }

        @Override
        public int sendSystemRequest(String msg, int... cdata) throws Exception {
            return 1; // > 0 means OK
        }
    }
}
