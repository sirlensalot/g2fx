package g2lib.usb;

import g2lib.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class UsbReadThread implements Runnable {

    private final Usb usb;
    private final Logger log = Util.getLogger(UsbReadThread.class);
    private final Thread thread;

    public UsbReadThread(Usb usb) {
        this.usb = usb;
        thread = new Thread(this);
    }

    public final AtomicBoolean go = new AtomicBoolean(true);
    public final AtomicInteger recd = new AtomicInteger(0);
    public final LinkedBlockingQueue<UsbMessage> q = new LinkedBlockingQueue<>();

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
                    receiveMsg(r, "extended");
                }
            } else {
                receiveMsg(r, "embedded");
            }
        }
        log.fine("Exit");
    }

    private void receiveMsg(UsbMessage r, String x) {
        for (MsgFuture f : new ArrayList<>(futures)) {
            if (f.filter.test(r)) {
                f.future.complete(r);
                futures.remove(f);
                return;
            }
        }
        try {
            q.put(r);
        } catch (Exception e) {
            log.severe(x + " put failed: " + e);
        }
    }



    public Future<UsbMessage> expect(String id, MsgP filter) {
        CompletableFuture<UsbMessage> f = new CompletableFuture<>();
        futures.add(new MsgFuture(id,filter,f));
        return f;
    }

    public UsbMessage expectBlocking(String msg, MsgP filter) throws InterruptedException {
        UsbMessage m = q.take();
        if (filter.test(m)) {
            log.fine(() -> "expect: received " + msg + ": " + m.dump());
            return m;
        } else {
            log.warning("expect: " + msg + ": did not receive: " + m.dump());
            return null;
        }
    }


}
