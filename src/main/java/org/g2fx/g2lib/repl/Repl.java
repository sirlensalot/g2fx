package org.g2fx.g2lib.repl;

import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Devices;
import org.g2fx.g2lib.state.Slot;
import org.g2fx.g2lib.state.UsbDevice;
import org.g2fx.g2lib.util.SafeLookup;
import org.g2fx.g2lib.util.Util;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Repl implements Runnable {

    private static final Logger log = Util.getLogger(Repl.class);

    public static final Object QUIT_SENTINEL = new Object();

    private final CommandRegistry commandRegistry;
    private final Devices devices;
    private final LineReader reader;
    private final Thread thread;
    private final boolean enabled;
    private final File scriptFile;
    private volatile boolean running = true;

    public record Command(String cmd, CommandMethods methods, CmdDesc desc) { }

    public record NamedIndex(int index, String name) { }
    public record SlotPatch(Slot slot, String name) { }
    public record Path(String device, String perf, SlotPatch slot, Integer variation,
                       AreaId area, NamedIndex module, NamedIndex param) { }

    private Path path = null;

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

    public Repl(Devices devices, boolean replEnabled, File scriptFile) throws Exception {
        this.devices = devices;
        this.enabled = replEnabled;
        this.scriptFile = scriptFile;

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
                                argDesc("file","File ending in .pch2 or .prf2"))),
                mkCmd("voice",(c,i) -> area(c,i,AreaId.Voice),NullCompleter.INSTANCE,
                        cmdDesc("Switch to voice area")),
                mkCmd("fx",(c,i) -> area(c,i,AreaId.Fx),NullCompleter.INSTANCE,
                        cmdDesc("Switch to FX area")),
                mkCmd("wait",Repl.this::doWait,NullCompleter.INSTANCE,
                        cmdDesc("Pause process",argDesc("time","time in millis"))),
                mkCmd("slot",Repl.this::slot,new StringsCompleter(
                        Arrays.stream(Slot.values()).map(s -> s.toString().toLowerCase()).toList()),
                        cmdDesc("Set current slot",argDesc("slot","a-d"))),
                mkCmd("load",Repl.this::load,NullCompleter.INSTANCE,
                        cmdDesc("Load entry into device",
                                argDesc("typeOrSlot","'perf' or slot (A,B,C,D)"),
                                argDesc("bank","bank number"),
                                argDesc("entry","bank entry number"))),
                mkCmd("comm",Repl.this::comm,NullCompleter.INSTANCE,
                        cmdDesc("Start/stop device communication stream",
                                argDesc("startStop","start or stop")))

        );
        commandRegistry = new JlineCommandRegistry(){{
            Map<String, CommandMethods> methods = new HashMap<>();
            cmds.forEach(c -> methods.put(c.cmd(),c.methods()));
            registerCommands(methods);
        }};
        boolean interactive = replEnabled && scriptFile == null;
        reader = LineReaderBuilder.builder()
                .terminal(interactive ?
                        TerminalBuilder.terminal() :
                        TerminalBuilder.builder().dumb(true).build())
                .parser(new DefaultParser())
                .completer(CommandRegistry.compileCompleters(commandRegistry))
                .variable(LineReader.HISTORY_FILE, ".g2lib-history")
                .build();
        thread = new Thread(this);
        Map<String, CmdDesc> descs = new HashMap<>();
        cmds.forEach(c -> descs.put(c.cmd(),c.desc()));
        new TailTipWidgets(reader, descs).enable();
        if (interactive) {
            reader.getTerminal().writer().println("""
                    Welcome to the g2lib interactive repl!
                         ___ _                \s
                     ___|_  | |_ ___ ___ _____\s
                    | . |  _|  _| -_|  _|     |
                    |_  |___|_| |___|_| |_|_|_|
                    |___|                     \s
                    """);
        }

    }

    private Object slot(CmdDesc desc, CommandInput input) {
        Slot s = Slot.fromAlpha(getArgs(desc,input).getFirst());
        if (path == null || path.perf() == null) { throw new InvalidCommandException(desc,"No current performance"); }
        SlotPatch sp = devices.invoke(true,() -> devices.getSlotPatch(s));
        path = new Path(path.device(), path.perf(),
                sp, path.variation(), path.area(), path.module(), path.param());
        return 1;
    }

    private Object doWait(CmdDesc c, CommandInput i) {
        try {
            Thread.sleep(parseInt(c,"wait",getArgs(c,i).getFirst()));
        } catch (InterruptedException ignore) { }
        return 1;
    }

    enum Comm {
        Start,
        Stop;
        public static final SafeLookup<String,Comm> LOOKUP = SafeLookup.makeLowerCaseNameLookup(values());
    }

    private Object comm(CmdDesc c, CommandInput i) {
        Comm comm = Comm.LOOKUP.get(getArgs(c,i).getFirst().toLowerCase());
        return devices.invoke(() -> {
            devices.getCurrent().sendStartStopComm(comm == Comm.Start);
            return 1;
        });
    }
    private Object area(CmdDesc c, CommandInput i, AreaId areaId) {
        getArgs(c,i); //validate no args
        if (path == null || path.slot() == null) { throw new InvalidCommandException(c,"No current patch"); }
        path = new Path(path.device(), path.perf(), path.slot(), path.variation(), areaId, path.module(), path.param());
        return 1;
    }

    private List<String> getArgs(CmdDesc desc, CommandInput input) {
        List<String> args = new ArrayList<>(Arrays.stream(input.args()).filter(s -> !s.isEmpty()).toList());
        if (args.size() != desc.getArgsDesc().size()) {
            throw new InvalidCommandException(desc,"expected " + desc.getArgsDesc().size() + " arguments");
        }
        return args;
    }


    private Object fileLoad(CmdDesc desc, CommandInput input) {
        final String path = getArgs(desc, input).getFirst();
        if (!new File(path).isFile()) {
            throw new InvalidCommandException(desc,"not a file");
        }
        if (!(path.endsWith("prf2") || path.endsWith("pch2"))) {
            throw new InvalidCommandException(desc,"Not a G2 file");
        }
        this.path = devices.invoke(true,() -> devices.loadFile(path));
        return 0;
    }


    private Object list(CmdDesc desc, CommandInput input) {
        List<String> words = getArgs(desc,input);
        UsbDevice.EntryType type = UsbDevice.EntryType.LC_NAME_LOOKUP.get(words.removeFirst());
        int bank = parseInt(desc,"index",words.removeFirst()) - 1;
        devices.execute((Devices.ThrowingRunnable) () -> {
            devices.getCurrent().dumpEntries(getWriter(),type,bank);
        });
        return "Success";
    }

    private Object load(CmdDesc desc, CommandInput input) {
        List<String> words = getArgs(desc,input);
        int slotCode;
        String typeOrSlot = words.removeFirst();
        if (typeOrSlot.equals("perf")) {
            slotCode = 4;
        } else {
            slotCode = Slot.fromAlpha(typeOrSlot.toUpperCase()).ordinal();
        }
        int bank = parseInt(desc,"bank",words.removeFirst()) - 1;
        int entry = parseInt(desc,"entry",words.removeFirst()) - 1;
        devices.execute((Devices.ThrowingRunnable) () -> devices.getCurrent().loadEntry(slotCode,bank,entry));
        return 1;
    }

    public int parseInt(CmdDesc desc, String msg, String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            throw new InvalidCommandException(desc,msg + ": expected integer: " + s);
        }
    }

    public PrintWriter getWriter() {
        return reader.getTerminal().writer();
    }


    public void start(boolean initialized) throws Exception {
        if (!replEnabled()) {
            log.info("Repl disabled");
            return;
        }

        if (!initialized) {
            getWriter().println("Device unavailable");
        }

        if (scriptFile == null) {
            thread.start();
        } else {
            runScript();
        }
    }

    private void runScript() throws Exception {
        try (BufferedReader fr = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = fr.readLine()) != null) {
                if (line.startsWith("#")) continue;
                ParsedLine pl = reader.getParser().parse(line, 0);
                handleInput(new ArrayList<>(pl.words()));
            }
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

    private void updatePath() {
        Path pp = devices.invoke(true,devices::getCurrentPath);
        if (this.path == null || this.path.device() == null || this.path.perf() == null) {
            log.info("overwriting path");
            this.path = pp;
        }
    }
    private String getPrompt() {
        updatePath();
        if (path == null || path.device() == null) { return "offline> "; }
        String s = path.device();
        if (path.perf() != null) {
            s += ":" + path.perf();
            if (path.slot() != null) {
                s += ":" + path.slot().name() + "[" + path.slot.slot() + "]";
                if (path.variation() != null) {
                    s += ":v" + (path.variation() + 1);
                }
                if (path.area() != null) {
                    s += ":" + path.area().name().toLowerCase();
                    //TODO module, param
                }
            }
        }
        //System.out.println(Arrays.toString(s.chars().toArray()));
        return s + "> ";
    }

    public void run() {
        while (running) {
            try {
                if (reader.readLine(getPrompt()).isEmpty()) continue;
            } catch (EndOfFileException | UserInterruptException e) {
                return;
            } catch (Exception e) {
                log.log(Level.SEVERE,"Error reading line",e);
                continue;
            }
            List<String> ws = new ArrayList<>(reader.getParsedLine().words());
            if (!handleInput(ws)) {
                return;
            }
        }
    }

    private boolean handleInput(List<String> ws) {
        if (ws.isEmpty()) return true;
        String cmd = ws.removeFirst();
        if (!commandRegistry.hasCommand(cmd)) {
            reader.getTerminal().writer().println("Invalid command: " + cmd);
            return true;
        }
        try {
            Object result = commandRegistry.invoke(
                    new CommandRegistry.CommandSession(reader.getTerminal()),
                    cmd,
                    ws.toArray());
            if (result == QUIT_SENTINEL) {
                reader.getTerminal().writer().println("Exiting");
                return false;
            }
        } catch (InvalidCommandException e) {
            reader.getTerminal().writer().format("Invalid command: %s\nUsage: %s\n",
                    e.getMessage(),
                    usage(e.getDesc(),cmd)
            );
        } catch (Exception e) {
            log.log(Level.SEVERE,"failure",e);
        }
        return true;
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
