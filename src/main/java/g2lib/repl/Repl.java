package g2lib.repl;

import g2lib.Main;
import g2lib.Util;
import g2lib.state.Device;
import g2lib.state.Devices;
import org.jline.console.*;
import org.jline.console.impl.ConsoleEngineImpl;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.widget.TailTipWidgets;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Repl implements Runnable {

    private static final Logger log = Util.getLogger(Repl.class);

    private final ExecutorService executorService;
    private final CommandRegistry commandRegistry;
    private final Devices devices;
    private final LineReader reader;
    private final Thread thread;
    private volatile boolean running = true;


    public Repl(ExecutorService executorService,
                Devices devices) throws Exception {
        this.executorService = executorService;
        this.devices = devices;

        JlineCommandRegistry commandRegistry = new JlineCommandRegistry() {
            //LOL an abstract class with no abstract methods!!!
        };
        final Completer completer = new Completer() {
            private final Completer arg1 = new StringsCompleter("perf","patch");
            @Override
            public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                if (line.wordIndex() == 1) {
                    arg1.complete(reader,line,candidates);
                }
            }
        };
        commandRegistry.registerCommands(Map.of(
                "list",new CommandMethods(this::list,c -> List.of(completer)),
                "exit",new CommandMethods(this::exit,commandRegistry::defaultCompleter)
                ));
        this.commandRegistry = commandRegistry;


        reader = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.terminal())
                .parser(new DefaultParser())
                .completer(CommandRegistry.compileCompleters(commandRegistry))
                .build();
        thread = new Thread(this);

        TailTipWidgets tailTipWidgets =
                new TailTipWidgets(reader,Map.of("list",new CmdDesc(
                        List.of(new AttributedString("list perf or patch bank entries")),
                        List.of(
                                new ArgDesc("perfOrPatch",List.of(new AttributedString("perf or patch"))),
                                new ArgDesc("index",List.of(new AttributedString("bank index")))),
                        Map.of())));
        tailTipWidgets.enable();


    }

    private Object list(CommandInput input) {
        List<String> words = new ArrayList<>(List.of(input.args()));
        System.out.println(words);
        if (words.size() != 2) {
            input.terminal().writer().println("usage TODO");
            return null;
        }
        String type = words.removeFirst();
        try {
            final int bank = Integer.parseUnsignedInt(words.removeFirst());
            executorService.execute(() -> {
                try {
                    if ("perf".equals(type)) {
                        System.out.println("perf");
                        Map<Integer, Map<Integer, String>> perfs =
                                devices.getCurrent().readEntryList(8, false);
                        Device.dumpEntries(false, perfs, bank);
                    } else if ("patch".equals(type)) {
                        System.out.println("patch");
                        Map<Integer, Map<Integer, String>> patches =
                                devices.getCurrent().readEntryList(32, true);
                        Device.dumpEntries(true, patches, bank);
                    }
                } catch (Exception ignore) {
                }

            });
        } catch (Exception e) {
            System.out.println("Invalid bank");
        }

        return "Success";
    }

    private Object exit(CommandInput commandInput) {
        return this;
    }

    public PrintWriter getWriter() {
        return reader.getTerminal().writer();
    }


    public void start() {
        if (!replEnabled()) {
            log.info("Repl disabled");
            return;
        }
        thread.start();
    }

    public boolean replEnabled() {
        return System.getProperty(Main.PROP_REPL) != null;
    }

    public void stop() {
        running = false;
    }

    public void join() throws InterruptedException {
        if (replEnabled()) thread.join();
    }

    public void run() {
        while (running) {
            if (reader.readLine("> ").isEmpty()) continue;
            List<String> ws = new ArrayList<>(reader.getParsedLine().words());
            if (ws.isEmpty()) continue;
            String cmd = ws.removeFirst();
            if (!commandRegistry.hasCommand(cmd)) {
                reader.getTerminal().writer().println("Invalid command");
                continue;
            }
            try {
                Object result = commandRegistry.invoke(
                        new CommandRegistry.CommandSession(reader.getTerminal()),
                        cmd,
                        ws.toArray());
                if (result == this) { // this is sentinel for exit
                    reader.getTerminal().writer().println("Exiting");
                    return;
                }
            } catch (Exception e) {
                log.log(Level.SEVERE,"failure",e);
            }

        }
    }
}
