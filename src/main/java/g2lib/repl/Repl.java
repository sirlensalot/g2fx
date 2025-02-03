package g2lib.repl;

import g2lib.Main;
import g2lib.Util;
import g2lib.state.Device;
import g2lib.state.Devices;
import org.jline.builtins.Completers;
import org.jline.console.*;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.widget.TailTipWidgets;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Repl implements Runnable {

    private static final Logger log = Util.getLogger(Repl.class);

    public static final Object QUIT_SENTINEL = new Object();

    private final CommandRegistry commandRegistry;

    private final ExecutorService executorService;
    private final Devices devices;
    private final LineReader reader;
    private final Thread thread;
    private volatile boolean running = true;

    public record Command(String cmd, CommandMethods methods, CmdDesc desc) { }

    public static class InvalidCommandException extends RuntimeException {
        private final CmdDesc desc;
        public InvalidCommandException(CmdDesc desc,String msg) {
            super(msg);
            this.desc = desc;
        }
        public CmdDesc getDesc() { return desc; }
    }

    public static Command mkCmd(String cmd, BiFunction<CmdDesc, CommandInput, Object> method,
                                final Completer completer, CmdDesc desc) {
        return new Command(cmd, new CommandMethods(
                i -> { return method.apply(desc,i); },
                c -> List.of(completer)),desc);
    }

    public static CmdDesc cmdDesc(String desc,ArgDesc... args) {
        return new CmdDesc(List.of(new AttributedString(desc)),List.of(args),Map.of());
    }

    public static ArgDesc argDesc(String argName, String... lines) {
        return new ArgDesc(argName, Arrays.stream(lines).map(AttributedString::new).toList());
    }

    public Repl(ExecutorService executorService,
                Devices devices) throws Exception {
        this.executorService = executorService;
        this.devices = devices;

        final Completer listCompleter = new Completer() {
            private final Completer arg1 = new StringsCompleter("perf","patch");
            @Override
            public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                if (line.wordIndex() == 1) {
                    arg1.complete(reader,line,candidates);
                }
            }
        };
        final List<Command> cmds = List.of(
                mkCmd("exit",(c,i) -> QUIT_SENTINEL,NullCompleter.INSTANCE,
                        cmdDesc("Exit program")),
                mkCmd("list",Repl.this::list,listCompleter,
                        cmdDesc("List bank or patch entries",
                                argDesc("perfOrPatch","'perf' or 'patch'"),
                                argDesc("index","bank index: 1-8 for perfs, 1-32 for patches"))),
                mkCmd("file-load",Repl.this::fileLoad,new Completers.FileNameCompleter(),
                        cmdDesc("Load perf or patch file",
                                argDesc("file","File ending in .pch2 or .prf2")))
        );
        commandRegistry = new JlineCommandRegistry(){{
            Map<String, CommandMethods> methods = new HashMap<>();
            cmds.forEach(c -> methods.put(c.cmd(),c.methods()));
            registerCommands(methods);
        }};
        reader = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.terminal())
                .parser(new DefaultParser())
                .completer(CommandRegistry.compileCompleters(commandRegistry))
                .build();
        thread = new Thread(this);
        Map<String, CmdDesc> descs = new HashMap<>();
        cmds.forEach(c -> descs.put(c.cmd(),c.desc()));
        new TailTipWidgets(reader, descs).enable();


    }

    private List<String> getArgs(CmdDesc desc, CommandInput input) {
        List<String> args = new ArrayList<>(Arrays.stream(input.args()).filter(s -> !s.isEmpty()).toList());
        if (args.size() != desc.getArgsDesc().size()) {
            throw new InvalidCommandException(desc,"expected " + desc.getArgsDesc().size() + " arguments");
        }
        return args;
    }


    private Object fileLoad(CmdDesc desc, CommandInput input) {
        String path = getArgs(desc, input).getFirst();
        if (!new File(path).isFile()) {
            throw new InvalidCommandException(desc,"not a file");
        }
        if (!(path.endsWith("prf2") || path.endsWith("pch2"))) {
            throw new InvalidCommandException(desc,"Not a G2 file");
        }
        executorService.execute(() -> {
            devices.loadFile(path);
        });
        return 0;
    }

    private Object list(CmdDesc desc, CommandInput input) {
        List<String> words = getArgs(desc,input);
        String type = words.removeFirst();
        try {
            final int bank = Integer.parseUnsignedInt(words.removeFirst());
            executorService.execute(() -> {
                if (!devices.online()) {
                    input.terminal().writer().println("Not online!");
                    return;
                }
                try {
                    if ("perf".equals(type)) {
                        Map<Integer, Map<Integer, String>> perfs =
                                devices.getCurrent().readEntryList(8, false);
                        Device.dumpEntries(false, perfs, bank);
                    } else if ("patch".equals(type)) {
                        Map<Integer, Map<Integer, String>> patches =
                                devices.getCurrent().readEntryList(32, true);
                        Device.dumpEntries(true, patches, bank);
                    }
                } catch (Exception ignore) {
                }
            });
        } catch (Exception e) {
            throw new InvalidCommandException(desc,"Invalid entry index");
        }

        return "Success";
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
            try {
                if (reader.readLine("> ").isEmpty()) continue;
            } catch (EndOfFileException | UserInterruptException e) {
                return;
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error reading line",e);
                continue;
            }
            List<String> ws = new ArrayList<>(reader.getParsedLine().words());
            if (ws.isEmpty()) continue;
            String cmd = ws.removeFirst();
            if (!commandRegistry.hasCommand(cmd)) {
                reader.getTerminal().writer().println("Invalid command: " + cmd);
                continue;
            }
            try {
                Object result = commandRegistry.invoke(
                        new CommandRegistry.CommandSession(reader.getTerminal()),
                        cmd,
                        ws.toArray());
                if (result == QUIT_SENTINEL) {
                    reader.getTerminal().writer().println("Exiting");
                    return;
                }
            } catch (InvalidCommandException e) {
                reader.getTerminal().writer().format("Invalid command: %s\nUsage: %s\n",
                        e.getMessage(),
                        usage(e.getDesc(),cmd)
                        );
            } catch (Exception e) {
                log.log(Level.SEVERE,"failure",e);
            }

        }
    }

    public static String usage(CmdDesc desc,String cmd) {
        final StringBuilder s = new StringBuilder(cmd);
        desc.getArgsDesc().forEach(d -> s.append(" ").append(d.getName()));
        desc.getArgsDesc().forEach(d -> {
            List<AttributedString> ad = d.getDescription();
            if (!ad.isEmpty()) {
                s.append(String.format("\n  %-16s %s", d.getName(), ad.getFirst().toAnsi()));
                for (int i = 1; i < ad.size(); i++) {
                    s.append(String.format("\n  %-16s %s"," ",ad.get(i).toAnsi()));
                }
            }
        });
        return s.toString();
    }
}
