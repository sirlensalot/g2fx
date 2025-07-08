package org.g2fx.g2gui;

import javafx.animation.AnimationTimer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FXQueue implements Executor {

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = false;
    private AnimationTimer timer;
    private static final Logger log = Logger.getLogger(FXQueue.class.getName());

    @Override
    public void execute(Runnable task) {
        if (!running) {
            log.warning("FXQueue is not running. Task submission ignored.");
            return;
        }
        queue.offer(task);
    }

    /**
     * Starts the AnimationTimer that polls the queue and executes tasks on the FX thread.
     * Should be called from the JavaFX Application Thread, typically in Application.start().
     */
    public void startPolling() {
        if (running) {
            log.warning("FXQueue is already running.");
            return;
        }
        running = true;
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Runnable task;
                while ((task = queue.poll()) != null) {
                    try {
                        task.run();
                    } catch (Exception e) {
                        log.log(Level.SEVERE,
                                "Exception while executing task in FXQueue",e);
                    }
                }
            }
        };
        timer.start();
    }

    /**
     * Stops the AnimationTimer and prevents further task submissions.
     * Tasks already in the queue will not be executed after shutdown.
     */
    public void shutdown() {
        running = false;
        if (timer != null) {
            timer.stop();
        }
        queue.clear();
    }
}
