package org.g2fx.g2gui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.g2fx.g2lib.PerformanceTest;
import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.state.Performance;
import org.g2fx.g2lib.state.Slot;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class FxTest {

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

    private static <T> T callFxThread(Callable<T> c) throws Exception {
        AtomicReference<T> ref = new AtomicReference<>();
        AtomicReference<Exception> ex = new AtomicReference<>();
        onFxThread(() -> {
            try {
                ref.set(c.call());
            } catch (Exception e) {
                ex.set(e);
            }
        });
        if (ex.get() != null) { fail(ex.get()); }
        return ref.get();
    }

    @Test
    void testG2GuiApplicationInitAndStart() throws Exception {

        G2GuiApplication app = startApp();
        onFxThread(() -> {
            long t = System.nanoTime();
            app.getDevices().loadFile(PerformanceTest.PERF_002, null);
            long elapsed = System.nanoTime() - t;
            System.out.printf("Load Perf Time: %dms\n",TimeUnit.MILLISECONDS.convert(elapsed,TimeUnit.NANOSECONDS));
        });

    }

    private static G2GuiApplication startApp() throws Exception {
        return callFxThread(() -> {
            G2GuiApplication app = new G2GuiApplication(false); // no usb
            try {
                app.init();
                Stage stage = new Stage();
                app.start(stage);
            } catch (Exception e) {
                fail("failure",e);
            }
            return app;
        });
    }

    /**
     * 005: on-device load of performance, which triggers performance load calls on editor
     * and perf lifecycle exercise.
     */
    @Test
    void testLoadMemPerf005() throws Exception {
        CaptureSender sender = new CaptureSender("data/capture/capture-005-loadmem-from-synth.pcapng");
        G2GuiApplication app = startApp();

        //check bridges not initialized
        Callable<Integer> computeBridgesCount = () ->
                app.getPerfBridges().activeCount() +
                        Arrays.stream(Slot.values()).reduce(0, (su, sl) ->
                                app.getSlots().getSlot(sl).getBridges().activeCount() + su, Integer::sum);
        assertEquals(0,callFxThread(computeBridgesCount));
        Device d = new Device(sender, app.getDevices().getPerfLoadListener(), app.getDevices().getPatchLoadListener());
        sender.dispatchInbounds();
        assertEquals("minimal02lfo",app.getDevices().getCurrentPerf().perfName().get());

        //test bridges initialized
        assertEquals(1924,callFxThread(computeBridgesCount));

        app.getDevices().getPerfLoadListener().onLifecycleDispose(app.getDevices().getCurrentPerf());

        assertEquals(0,callFxThread(computeBridgesCount));

    }

    @Test
    void testLoadMemPatch007() throws Exception {
        CaptureSender sender = new CaptureSender("data/capture/capture-007-loadmem-patch-g2fx-uprate-4mod.pcapng");
        G2GuiApplication app = startApp();

        //check bridges not initialized
        Callable<Integer> computeBridgesCount = () ->
                app.getPerfBridges().activeCount() +
                        Arrays.stream(Slot.values()).reduce(0, (su, sl) ->
                                app.getSlots().getSlot(sl).getBridges().activeCount() + su, Integer::sum);
        //assertEquals(0,callFxThread(computeBridgesCount));
        Device d = new Device(sender, app.getDevices().getPerfLoadListener(), app.getDevices().getPatchLoadListener());
        Performance perf = new Performance(sender);
        perf.setVersion(1);
        d.setPerf(perf);
        perf.slots().forEach(p -> p.setVersion(2));
        d.getEntries().loadEntry(0, 7, 0);
        sender.dispatchInbounds();
        assertEquals("g2fx-uprate-4mod",perf.getSlot(Slot.A).name().get());

        //test bridges initialized
        assertEquals(90,callFxThread(computeBridgesCount));

        app.getDevices().getPatchLoadListener().onLifecycleDispose(perf.getSlot(Slot.A));

        assertEquals(0,callFxThread(computeBridgesCount));

    }

}
