package org.g2fx.g2gui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.g2fx.g2lib.PerformanceTest;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    @Test
    void testG2GuiApplicationInitAndStart() throws Exception {

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
        onFxThread(() -> {
            long t = System.nanoTime();
            appref.get().getDevices().loadFile(PerformanceTest.PERF_002);
            long elapsed = System.nanoTime() - t;
            System.out.printf("Time: %dms\n",TimeUnit.MILLISECONDS.convert(elapsed,TimeUnit.NANOSECONDS));
        });

    }
}
