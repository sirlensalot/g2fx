package org.g2fx.g2gui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.g2fx.g2lib.PerformanceTest;
import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.usb.Dispatcher;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.usb.Usb;
import org.g2fx.g2lib.usb.UsbSender;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.g2fx.g2lib.DeviceTest.parseCapture;
import static org.junit.jupiter.api.Assertions.*;

public class FxTest {

    /**
     * Sender + dispatcher driver using capture files. Expects outbounds to match exactly;
     * dispatches all inbounds until next outbound.
     */
    public static class CaptureSender implements UsbSender {

        private List<MessageRecorder.RecordedUsbMessage> script;
        private Dispatcher dispatcher;

        public CaptureSender(List<MessageRecorder.RecordedUsbMessage> script) {
            this.script = script;
        }
        public CaptureSender(String f) throws Exception {
            this(parseCapture(f, m -> true));
        }

        @Override
        public int sendBulk(String msg, boolean dispatch, ByteBuffer data) throws Exception {
            MessageRecorder.RecordedUsbMessage m = script.removeFirst();
            assertEquals(Util.dumpBufferString(m.msg().buffer()),Util.dumpBufferString(Usb.prepareSendBuffer(data)));
            dispatchInbounds();
            return 0;
        }

        public void dispatchInbounds() throws Exception {
            while (!script.isEmpty() && script.getFirst().inbound()) {
                assertTrue(dispatcher.dispatch(script.removeFirst().msg()));
            }

        }

        @Override
        public void shutdown() {

        }

        @Override
        public void setDispatcher(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }
    }

    @BeforeAll
    static void before() {
        Util.configureLogging();
        new JFXPanel();
    }

    private static void onFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("FX task timed out");
        }
    }

    @Test
    void testG2GuiApplicationInitAndStart() throws Exception {

        G2GuiApplication app = startApp();
        onFxThread(() -> {
            long t = System.nanoTime();
            app.getDevices().loadFile(PerformanceTest.PERF_002);
            long elapsed = System.nanoTime() - t;
            System.out.printf("Time: %dms\n",TimeUnit.MILLISECONDS.convert(elapsed,TimeUnit.NANOSECONDS));
        });

    }

    private static G2GuiApplication startApp() throws InterruptedException {
        AtomicReference<G2GuiApplication> appref = new AtomicReference<>();
        onFxThread(() -> {
            G2GuiApplication app = new G2GuiApplication(false); // no usb
            try {
                app.init();
                Stage stage = new Stage();
                app.start(stage);
            } catch (Exception e) {
                fail("failure",e);
            }
            appref.set(app);
        });
        G2GuiApplication app = appref.get();
        return app;
    }

    /**
     * 005: on-device load of performance, which triggers performance load calls on editor
     * and perf lifecycle exercise.
     */
    @Test
    void testLoadMem005() throws Exception {
        CaptureSender sender = new CaptureSender("data/capture/capture-005-loadmem-from-synth.pcapng");
        G2GuiApplication app = startApp();

        //check bridges not initialized
        assertEquals(0,app.getPerfBridges().activeCount());
        Performance p = new Performance(sender);
        app.getDevices().setCurrentPerf(p);
        Device d = new Device(sender);
        d.setPerf(p);
        sender.dispatchInbounds();

        assertEquals("minimal02lfo",p.perfName().get());

        //test bridges initialized
        assertEquals(1564,app.getPerfBridges().activeCount());

    }

}
