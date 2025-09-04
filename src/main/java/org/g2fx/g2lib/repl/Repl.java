package org.g2fx.g2lib.repl;

import org.g2fx.g2lib.state.Devices;
import org.g2fx.g2lib.util.Util;
import org.jline.console.CmdDesc;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.widget.TailTipWidgets;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Repl implements Runnable {

    private static final Logger log = Util.getLogger(Repl.class);


    private final Devices devices;
    private final Thread thread;
    private final boolean enabled;
    private final File scriptFile;
    private volatile boolean running = true;
    private final Eval eval;


    public Repl(Devices devices, boolean replEnabled, File scriptFile) throws Exception {
        this.devices = devices;
        this.enabled = replEnabled;
        this.scriptFile = scriptFile;
        boolean interactive = replEnabled && scriptFile == null;
        this.eval = new Eval(devices,interactive,null);

        thread = new Thread(this);
        Map<String, CmdDesc> descs = new HashMap<>();
        eval.getCmds().forEach(c -> descs.put(c.cmd(),c.desc()));
        new TailTipWidgets(eval.getReader(), descs).enable();
        if (interactive) {
            eval.getReader().getTerminal().writer().println("""
                    Welcome to the g2lib interactive repl!
                         ___ _                \s
                     ___|_  | |_ ___ ___ _____\s
                    | . |  _|  _| -_|  _|     |
                    |_  |___|_| |___|_| |_|_|_|
                    |___|                     \s
                    """);
        }

    }


    public void start(boolean initialized) throws Exception {
        if (!replEnabled()) {
            log.info("Repl disabled");
            return;
        }

        if (!initialized) {
            eval.getWriter().println("Device unavailable");
        }

        if (scriptFile == null) {
            thread.start();
        } else {
            eval.runScript(scriptFile);
        }
    }


    public boolean replEnabled() {
        return enabled;
    }

    public void stop() {
        running = false;
    }

    public void join() throws InterruptedException {
        if (replEnabled()) thread.join();
    }

    private String getPrompt() {
        eval.updatePath();
        String s = eval.descPath();
        return s == null ? "offline> " : s + "> ";
    }

    public void run() {
        while (running) {
            try {
                if (eval.getReader().readLine(getPrompt()).isEmpty()) continue;
            } catch (EndOfFileException | UserInterruptException e) {
                return;
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error reading line",e);
                continue;
            }
            List<String> ws = new ArrayList<>(eval.getReader().getParsedLine().words());
            if (!eval.handleInput(ws)) {
                return;
            }
        }
    }
}
