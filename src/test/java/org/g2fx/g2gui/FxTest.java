package org.g2fx.g2gui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Point2D;
import javafx.stage.Stage;
import org.g2fx.g2gui.panel.AreaPane;
import org.g2fx.g2gui.panel.Slots;
import org.g2fx.g2lib.PerformanceTest;
import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.state.*;
import org.g2fx.g2lib.usb.MessageRecorder;
import org.g2fx.g2lib.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class FxTest {

    @BeforeAll
    static void before() {
        Util.configureLogging();
        new JFXPanel();
    }


    private static <T> T callFxQueue(G2GuiApplication app, Callable<T> c) throws Exception {
        AtomicReference<T> ref = new AtomicReference<>();
        AtomicReference<Exception> ex = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        app.getFxQueue().execute(() -> {
            try {
                ref.set(c.call());
                latch.countDown();
            } catch (Exception e) {
                ex.set(e);
            }
        });
        assertTrue(latch.await(1,TimeUnit.SECONDS));
        if (ex.get() != null) { fail(ex.get()); }
        return ref.get();
    }

    private static void onFxQueue(G2GuiApplication app, Runnable task) throws Exception {
        AtomicReference<Exception> ex = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        app.getFxQueue().execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                ex.set(e);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(1,TimeUnit.SECONDS));
        if (ex.get() != null) { fail(ex.get()); }
    }


    @Test
    void testG2GuiApplicationInitAndStart() throws Exception {

        G2GuiApplication app = startApp();
        onFxQueue(app,() -> {
            long t = System.nanoTime();
            app.getDevices().loadFile(PerformanceTest.PERF_002, null);
            long elapsed = System.nanoTime() - t;
            System.out.printf("Load Perf Time: %dms\n",TimeUnit.MILLISECONDS.convert(elapsed,TimeUnit.NANOSECONDS));
        });

    }

    private static G2GuiApplication startApp() throws Exception {
        // no usb
        AtomicReference<G2GuiApplication> ref = new AtomicReference<>();
        AtomicReference<Exception> ex = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                G2GuiApplication app = new G2GuiApplication(false); // no usb
                try {
                    app.init();
                    Stage stage = new Stage();
                    app.start(stage);
                } catch (Exception e) {
                    fail("failure", e);
                }
                ref.set(app);
            } catch (Exception e1) {
                ex.set(e1);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("FX task timed out");
        }
        if (ex.get() != null) { fail(ex.get()); }
        return ref.get();
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
        assertEquals(0,callFxQueue(app,computeBridgesCount));
        Device d = new Device(sender, app.getDevices().getPerfLoadListener(), app.getDevices().getPatchLoadListener());
        sender.dispatchInbounds();
        assertEquals("minimal02lfo",app.getDevices().getCurrentPerf().perfName().get());

        //test bridges initialized
        assertEquals(1932,callFxQueue(app,computeBridgesCount));

        app.getDevices().getPerfLoadListener().onLifecycleDispose(app.getDevices().getCurrentPerf());

        assertEquals(0,callFxQueue(app,computeBridgesCount));

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
        assertEquals(92,callFxQueue(app,computeBridgesCount));

        app.getDevices().getPatchLoadListener().onLifecycleDispose(perf.getSlot(Slot.A));

        assertEquals(0,callFxQueue(app,computeBridgesCount));

    }


    @Test
    void testPasteMods009() throws Exception {
        CaptureSender sender = new CaptureSender("data/capture/capture-009-pasteallmods-g2fx-uprate-4mod.pcapng");
        MessageRecorder.RecordedUsbMessage m0 = sender.getScript().get(0);
        //overwrite 5a reserved value,crc
        m0.msg().buffer().put(0xd1, (byte)0x40);
        m0.msg().buffer().putShort(0xf9, (short) 0xa275);
        sender.setStrict(false);

        G2GuiApplication app = startApp();
        app.getDevices().setCurrentSender(sender);
        app.getDevices().loadFile(PerformanceTest.PATCH_UPRATE_4MOD,Slot.A);
        //select all modules in A:VA
        PatchArea libSlotAVoice = app.getDevices().getCurrentPerf().getSlot(Slot.A).getArea(AreaId.Voice);
        AreaPane uiSlotAVoice = callFxQueue(app,() -> app.getSlots().getSlot(Slot.A).getAreaPane(AreaId.Voice));
        for (PatchModule pm : libSlotAVoice.getModules()) {
            onFxQueue(app,() -> uiSlotAVoice.selectModule(pm.getIndex()));
        }
        //copy
        Slots.ModuleIds mids = callFxQueue(app,() -> app.getSlots().getSlot(Slot.A).doCopy());
        assertEquals(new Slots.ModuleIds(Slot.A,AreaId.Voice, new TreeSet<>(List.of(1,2,3,4))),mids);
        //start paste
        onFxQueue(app,()->app.getSlots().doPaste(mids));
        //simulate mouse position
        onFxQueue(app,()->uiSlotAVoice.getModulePaste().init(new Point2D(300,290)));

        sender.setStrict(true);
        app.getDevices().getCurrentPerf().getSlot(Slot.A).setVersion(4);
        app.getDevices().getCurrentPerf().getSlot(Slot.B).setVersion(2);
        app.getDevices().getCurrentPerf().getSlot(Slot.C).setVersion(2);
        app.getDevices().getCurrentPerf().getSlot(Slot.D).setVersion(2);

        //simulate mouse click
        onFxQueue(app,()->uiSlotAVoice.getModulePaste().onMouseReleased(null));

        Coords c5 = new Coords(1, 15);
        assertEquals(c5,callFxQueue(app, () -> uiSlotAVoice.getModule(5).coords().getValue()));
        assertEquals(c5,libSlotAVoice.getModule(5).getUserModuleData().coords().get());
        Coords c6 = new Coords(1, 18);
        assertEquals(c6,callFxQueue(app, () -> uiSlotAVoice.getModule(6).coords().getValue()));
        assertEquals(c6,libSlotAVoice.getModule(6).getUserModuleData().coords().get());
        Coords c7 = new Coords(1, 20);
        assertEquals(c7,callFxQueue(app, () -> uiSlotAVoice.getModule(7).coords().getValue()));
        assertEquals(c7,libSlotAVoice.getModule(7).getUserModuleData().coords().get());
        Coords c8 = new Coords(1, 22);
        assertEquals(c8,callFxQueue(app, () -> uiSlotAVoice.getModule(8).coords().getValue()));
        assertEquals(c8,libSlotAVoice.getModule(8).getUserModuleData().coords().get());
        //TODO add other test for negative placements
        //assertEquals(List.of(),sender.getScript()); TODO -- select param for module under mouse, send unk6s
    }

}
