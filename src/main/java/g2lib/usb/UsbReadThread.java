package g2lib.usb;

import g2lib.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class UsbReadThread implements Runnable {

    private final Usb usb;
    private final Logger log = Util.getLogger(UsbReadThread.class);
    private final Thread thread;
    private Dispatcher dispatcher;
    private final AtomicBoolean go = new AtomicBoolean(true);
    private final AtomicInteger recd = new AtomicInteger(0);

    public UsbReadThread(Usb usb) {
        this.usb = usb;
        thread = new Thread(this);
    }


    public void shutdown() {
        log.fine("Shutdown");
        go.set(false);
        try {
            log.fine("Joining read thread");
            thread.join();
        } catch (Exception ignored) {}
    }


    public interface MsgP extends Predicate<UsbMessage> {

    }

    private record MsgFuture(String id,
                             MsgP filter,
                             CompletableFuture<UsbMessage> future) {

    }
    private final List<MsgFuture> futures = new CopyOnWriteArrayList<>();

    public void start() { thread.start(); }

    @Override
    public void run() {
        while (go.get()) {
            UsbMessage r = usb.readInterrupt(500);
            if (!r.success()) {
                continue;
            }
            recd.incrementAndGet();
            if (r.extended()) {
                r = usb.readBulkRetries(r.size(), 5);
                if (r.success()) {
                    receiveMsg(r);
                }
            } else {
                receiveMsg(r);
            }
        }
        log.fine("Exit");
    }

    private void receiveMsg(UsbMessage r) {
        for (MsgFuture f : new ArrayList<>(futures)) {
            if (f.filter.test(r)) {
                f.future.complete(r);
                futures.remove(f);
                return;
            }
        }
        dispatcher.dispatch(r);
    }

    public Future<UsbMessage> expect(String id, MsgP filter) {
        CompletableFuture<UsbMessage> f = new CompletableFuture<>();
        futures.add(new MsgFuture(id, filter, f));
        return f;
    }
    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}
