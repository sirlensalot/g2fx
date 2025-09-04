package org.g2fx.g2lib.repl;

import com.google.common.collect.Streams;
import org.g2fx.g2gui.controls.IndexParam;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.state.*;
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

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Eval {

    private final PrintWriter writer;
    private Logger log = Util.getLogger(getClass());

    public static final Object QUIT_SENTINEL = new Object();

    public record Command(String cmd, CommandMethods methods, CmdDesc desc) { }
    
    private final LineReader reader;
    private final Devices devices;

    private final CommandRegistry commandRegistry;
    private final List<Command> cmds;

    private Path path = null;
    
    public Eval(Devices devices, boolean interactive, PrintWriter writer) throws Exception {

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
        cmds = List.of(
                mkCmd("exit",(c,i) -> QUIT_SENTINEL, NullCompleter.INSTANCE,
                        cmdDesc("Exit program")),
                mkCmd("list-entries",Eval.this::listEntries,listCompleter,
                        cmdDesc("List bank or patch entries",
                                argDesc("perfOrPatch","'perf' or 'patch'"),
                                argDesc("index","bank index: 1-8 for perfs, 1-32 for patches"))),
                mkCmd("file-load",Eval.this::fileLoad,new Completers.FileNameCompleter(),
                        cmdDesc("Load perf or patch file",
                                argDesc("file","File ending in .pch2 or .prf2"))),
                mkCmd("voice",(c,i) -> area(c,i, AreaId.Voice),NullCompleter.INSTANCE,
                        cmdDesc("Switch to voice area")),
                mkCmd("fx",(c,i) -> area(c,i,AreaId.Fx),NullCompleter.INSTANCE,
                        cmdDesc("Switch to FX area")),
                mkCmd("wait",Eval.this::doWait,NullCompleter.INSTANCE,
                        cmdDesc("Pause process",argDesc("time","time in millis"))),
                mkCmd("slot",Eval.this::slot,new StringsCompleter(
                                Arrays.stream(Slot.values()).map(s -> s.toString().toLowerCase()).toList()),
                        cmdDesc("Set current slot",argDesc("slot","a-d"))),
                mkCmd("load",Eval.this::load,NullCompleter.INSTANCE,
                        cmdDesc("Load entry into device",
                                argDesc("typeOrSlot","'perf' or slot (A,B,C,D)"),
                                argDesc("bank","bank number"),
                                argDesc("entry","bank entry number"))),
                mkCmd("echo",Eval.this::echo,NullCompleter.INSTANCE,cmdDesc("echo args")),
                mkCmd("path",Eval.this::path,NullCompleter.INSTANCE,cmdDesc("echo path")),
                mkCmd("list",Eval.this::list,NullCompleter.INSTANCE,cmdDesc("context-sensitive list")),
                mkCmd("var",Eval.this::var,NullCompleter.INSTANCE,cmdDesc("switch variation",
                                argDesc("idx","variation index"))),
                mkCmd("mod",Eval.this::mod,NullCompleter.INSTANCE,cmdDesc("switch to module",
                        argDesc("idx","module index"))),
                mkCmd("pset",Eval.this::pset,NullCompleter.INSTANCE,cmdDesc("set param value",
                        argDesc("idxOrName","param index or name"),
                        argDesc("val","param value"))),
                mkCmd("comm",Eval.this::comm,NullCompleter.INSTANCE,
                        cmdDesc("Start/stop device communication stream",
                                argDesc("startStop","start or stop")))

        );
        commandRegistry = new JlineCommandRegistry(){{
            Map<String, CommandMethods> methods = new HashMap<>();
            cmds.forEach(c -> methods.put(c.cmd(),c.methods()));
            registerCommands(methods);
        }};

        reader = LineReaderBuilder.builder()
                .terminal(interactive ?
                        TerminalBuilder.terminal() :
                        TerminalBuilder.builder().dumb(true).build())
                .parser(new DefaultParser())
                .completer(CommandRegistry.compileCompleters(commandRegistry))
                .variable(LineReader.HISTORY_FILE, ".g2lib-history")
                .build();

        this.writer = writer == null ? reader.getTerminal().writer() : writer;
        
    }

    private Object pset(CmdDesc desc, CommandInput ci) {
        if (path == null || path.module() == null) { throw bad(desc, "No current path/module"); }
        List<String> as = getArgs(desc, ci);
        List<NamedParam> ps = path.module().meta().getParams();
        IndexParam np = null;
        for (int i = 0 ; i < ps.size(); i++) {
            NamedParam p = ps.get(i);
            if (p.name().equals(as.get(0))) { np = new IndexParam(p,i,""); }
        }
        if (np != null) {
            as.removeFirst();
        } else {
            int ix = parseNextInt(desc, "param index", as);
            if (ix >= ps.size()) { throw bad(desc, "Invalid mod index: %s", ix); }
            np = new IndexParam(ps.get(ix),ix,"");
        }
        int val = parseNextInt(desc,"param val",as);
        ModParam mp = np.param().param();
        if (val < mp.min || val > mp.max) {
            throw bad(desc,"Invalid param value %s, should be [%s,%s)",val,mp.min,mp.max);
        }
        final var npp = np;
        devices.runWithCurrent(d -> getCurrentArea(d).getModule(path.module().index())
                .getParamValueProperty(path.variation(),npp.index()).set(val));
        return 1;
    }

    private static InvalidCommandException bad(CmdDesc desc, String msg, Object... args) {
        return new InvalidCommandException(desc, String.format(msg,args));
    }


    private Object var(CmdDesc desc, CommandInput ci) {
        if (path == null || path.slot() == null) { throw bad(desc, "No current path/slot"); }
        int v = parseNextInt(desc,"var idx",getArgs(desc,ci));
        path = path.setVar(v);
        return 1;
    }

    public Path getPath() {
        return path;
    }

    private Object mod(CmdDesc c, CommandInput i) {
        if (path == null || path.area() == null) { return done("No path"); }
        int idx = parseNextInt(c,"mod index",getArgs(c,i));
        path = devices.invokeWithCurrent(d -> {
            PatchModule m = getCurrentArea(d).getModule(idx);
            return path.setModule(new Path.NamedIndex<>(m.getIndex(),m.name().get(),m.getUserModuleData().getType()));
        });
        return 1;
    }

    private Object list(CmdDesc desc, CommandInput ci) {
        if (path == null) { return done("No path"); }
        if (path.area() == null) { return done("No area"); }
        if (path.module() == null) {
            return done(devices.invokeWithCurrent(d ->
                    String.join("\n", getCurrentArea(d).getModules().stream().map(m ->
                            String.format("%s:%s (%s)", m.getIndex(), m.name().get(),
                                    m.getUserModuleData().getType().shortName)).toList())
            ));
        }
        return done(devices.invokeWithCurrent(d -> {
            PatchModule m = getCurrentArea(d).getModule(path.module().index());
            List<Integer> vvs = m.getVarValues(path.variation());
            return String.join("\n", Streams.mapWithIndex(vvs.stream(),(v,i) ->
                    String.format("%s:%s",m.getUserModuleData().getType().getParams().get((int)i).name(),v))
                    .toList()); }));
    }

    private PatchArea getCurrentArea(Device d) {
        return d.getPerf().getSlot(path.slot().slot())
                .getArea(path.area());
    }

    private Object done(String output) { getWriter().println(output); return 1; }


    private Object path(CmdDesc desc, CommandInput input) {
        getWriter().println(path == null ? "None" : path);
        return 1;
    }

    private Object echo(CmdDesc desc, CommandInput input) {
        for (String arg : input.args()) {
            writer.print(arg);
            writer.print(" ");
        }
        writer.println();
        return 1;
    }

    public List<Command> getCmds() {
        return cmds;
    }

    public LineReader getReader() {
        return reader;
    }

    public PrintWriter getWriter() {
        return writer;
    }


    public static CmdDesc cmdDesc(String desc, ArgDesc... args) {
        return new CmdDesc(List.of(new AttributedString(desc)),List.of(args),Map.of());
    }

    public static ArgDesc argDesc(String argName, String... lines) {
        return new ArgDesc(argName, Arrays.stream(lines).map(AttributedString::new).toList());
    }


    private Object slot(CmdDesc desc, CommandInput input) {
        Slot s = Slot.fromAlpha(getArgs(desc,input).getFirst());
        if (path == null || path.perf() == null ) { throw bad(desc, "No current performance"); }
        path = devices.invoke(true,() ->
                devices.withCurrent(c -> Path.pathForPatch(c,c.getPerf().getSlot(s))));
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
        if (path == null || path.slot() == null) { throw bad(c, "No current patch"); }
        path = new Path(path.device(), path.perf(), path.slot(), path.variation(), areaId, path.module(), path.param());
        return 1;
    }

    private List<String> getArgs(CmdDesc desc, CommandInput input) {
        List<String> args = new ArrayList<>(Arrays.stream(input.args()).filter(s -> !s.isEmpty()).toList());
        if (args.size() != desc.getArgsDesc().size()) {
            throw bad(desc, "expected " + desc.getArgsDesc().size() + " arguments");
        }
        return args;
    }


    private Object fileLoad(CmdDesc desc, CommandInput input) {
        final String filePath = getArgs(desc, input).getFirst();
        if (!new File(filePath).isFile()) {
            throw bad(desc, "not a file");
        }
        if (!(filePath.endsWith("prf2") || filePath.endsWith("pch2"))) {
            throw bad(desc, "Not a G2 file");
        }
        path = devices.invoke(true,() -> {
            devices.loadFile(filePath);
            return devices.withCurrent(c -> Path.pathForPatch(c,c.getPerf().getSelectedPatch()));
        });
        return 0;
    }


    private Object listEntries(CmdDesc desc, CommandInput input) {
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

    public int parseNextInt(CmdDesc desc, String msg, List<String> words) {
        return parseInt(desc,msg,words.removeFirst());
    }
    public int parseInt(CmdDesc desc, String msg, String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            throw bad(desc, msg + ": expected integer: " + s);
        }
    }



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


    public void runScript(File scriptFile) throws Exception {
        try (BufferedReader fr = new BufferedReader(new FileReader(scriptFile))) {
            runScript(fr);
        }
    }

    public void runScript(BufferedReader fr) throws IOException {
        updatePath();
        String line;
        while ((line = fr.readLine()) != null) {
            if (line.startsWith("#")) continue;
            ParsedLine pl = reader.getParser().parse(line, 0);
            handleInput(new ArrayList<>(pl.words()));
        }
    }


    public boolean handleInput(List<String> ws) {
        if (ws.isEmpty()) return true;
        String cmd = ws.removeFirst();
        if (!commandRegistry.hasCommand(cmd)) {
            getWriter().println("Invalid command: " + cmd);
            return true;
        }
        try {
            Object result = commandRegistry.invoke(
                    new CommandRegistry.CommandSession(reader.getTerminal()),
                    cmd,
                    ws.toArray());
            if (result == QUIT_SENTINEL) {
                getWriter().println("Exiting");
                return false;
            }
        } catch (InvalidCommandException e) {
            getWriter().format("Invalid command: %s\nUsage: %s\n",
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

    public Path updatePath() {
        if (path == null || path.device() == null || path.perf() == null) {
            path = devices.invoke(true,() -> {
                Device cur = devices.getCurrent();
                if (cur == null || cur.getPerf() == null) { return null; }
                Patch patch = cur.getPerf().getSelectedPatch();
                return Path.pathForPatch(cur, patch);
            });
        }
        return path;
    }

}
