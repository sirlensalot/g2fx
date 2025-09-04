package org.g2fx.g2lib.repl;

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

import static org.g2fx.g2lib.util.Util.forEachIndexed;
import static org.g2fx.g2lib.util.Util.notNull;

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
                mkCmd("exit",(c,i) -> QUIT_SENTINEL,
                        cmdDesc("Exit program")),
                mkCmd("list-entries",Eval.this::listEntries,listCompleter,
                        cmdDesc("List bank or patch entries",
                                argDesc("perfOrPatch","'perf' or 'patch'"),
                                argDesc("index","bank index: 1-8 for perfs, 1-32 for patches"))),
                mkCmd("file-load",Eval.this::fileLoad,new Completers.FileNameCompleter(),
                        cmdDesc("Load perf or patch file",
                                argDesc("file","File ending in .pch2 or .prf2"))),
                mkCmd("va",(c,i) -> area(c,i, AreaId.Voice),
                        cmdDesc("Switch to voice area")),
                mkCmd("fx",(c,i) -> area(c,i,AreaId.Fx),
                        cmdDesc("Switch to FX area")),
                mkCmd("wait",Eval.this::doWait,
                        cmdDesc("Pause process",argDesc("time","time in millis"))),
                mkCmd("slot",Eval.this::slot,new StringsCompleter(
                                Arrays.stream(Slot.values()).map(s -> s.toString().toLowerCase()).toList()),
                        cmdDesc("Set current slot",argDesc("slot","a-d"))),
                mkCmd("load",Eval.this::load,
                        cmdDesc("Load entry into device",
                                argDesc("typeOrSlot","'perf' or slot (A,B,C,D)"),
                                argDesc("bank","bank number"),
                                argDesc("entry","bank entry number"))),
                mkCmd("echo",Eval.this::echo,cmdDesc("echo args")),
                mkCmd("path",Eval.this::path,cmdDesc("echo path")),
                mkCmd("list",Eval.this::list,cmdDesc("context-sensitive list")),
                mkCmd("var",Eval.this::var,cmdDesc("switch variation",
                                argDesc("idx","variation index"))),
                mkCmd("mod",Eval.this::mod,cmdDesc("switch to module",
                        argDesc("nameOrIdx","module name or index"))),
                mkCmd("pset",Eval.this::pset,cmdDesc("set param value",
                        argDesc("idxOrName","param index or name"),
                        argDesc("val","param value"))),
                mkCmd("pload",Eval.this::pload,cmdDesc("set patch load meter",
                        argDesc("area","patch area (fx|va)"),
                        argDesc("meter","meter index (mem|cyc)"),
                        argDesc("value","load value")
                        )),
                mkCmd("help",Eval.this::help,cmdDesc("Command help")),
                mkCmd("led",Eval.this::led,cmdDesc("toggle green led",
                        argDesc("name","led name"))),
                mkCmd("comm",Eval.this::comm,
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

    private Object led(CmdDesc desc, CommandInput ci) {
        String name = getArgs(desc,ci).removeFirst();
        devices.runWithCurrent(d -> {
            PatchModule pm = getCurrentModule(d);
            getCurrentSlot(d).getLeds().forEach(v -> {
                if (v.getModule() == pm && v.getVisual().names().getFirst().equals(name)) {
                    int nv = v.value().get() == 0 ? 1 : 0;
                    v.value().set(nv);
                }
            });
        });
        return null;
    }

    private Object pload(CmdDesc desc, CommandInput ci) {
        List<String> as = getArgs(desc, ci);
        var area = "va".equals(as.removeFirst()) ? AreaId.Voice : AreaId.Fx;
        var isMem = "mem".equals(as.removeFirst());
        double val = parseInt(desc,"value",as.removeFirst());
        devices.runWithCurrent(d -> Arrays.stream(AreaId.USER_AREAS).forEach(a -> {
            PatchLoadData data = getCurrentSlot(d).getArea(area).getPatchLoadData();
            if (isMem) data.mem().set(val); else data.cycles().set(val);
        }));
        return 1;
    }

    private Patch getCurrentSlot(Device d) {
        return d.getPerf().getSlot(path.slot().slot());
    }

    private Object help(CmdDesc desc, CommandInput ci) {
        for (Command cmd : cmds) {
            getWriter().println(usage(cmd.desc,cmd.cmd));
        }
        return 1;
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
        devices.runWithCurrent(d -> getCurrentModule(d)
                .getParamValueProperty(path.variation(),npp.index()).set(val));
        return 1;
    }

    private PatchModule getCurrentModule(Device d) {
        return getCurrentArea(d).getModule(path.module().index());
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
        if (path == null || path.area() == null) { throw bad(c,"Path null/no area"); }
        String a = getArgs(c,i).removeFirst();
        path = devices.invokeWithCurrent(d -> {
            PatchModule m = null;
            for (PatchModule pm : getCurrentArea(d).getModules()) {
                if (a.equals(pm.name().get())) {
                    m = pm;
                }
            }
            if (m == null) { m = getCurrentArea(d).getModule(parseInt(c,"mod idx",a)); }
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
        devices.runWithCurrent(d -> {
            PatchModule m = getCurrentModule(d);

            getWriter().println("Params:");
            List<Integer> vvs = m.getVarValues(path.variation());
            forEachIndexed(vvs,(v,i) ->
                    getWriter().format("  %s:%s\n", m.getUserModuleData().getType().getParams().get((int) i).name(), v));

            getWriter().println("LEDs:");
            Patch patch = getCurrentSlot(d);
            forEachIndexed(patch.getLeds(), (pv,ix) -> {
                if (notNull(pv).getModule() == m) {
                    getWriter().format("  %s: %s\n", ix, pv);
                }
            });
            getWriter().println("Meters/Groups:");
            forEachIndexed(patch.getMetersAndGroups(), (pv,ix) -> {
                if (notNull(pv).getModule() == m) {
                    getWriter().format("  %s: %s\n", ix, pv);
                }
            });
        });
        return 1;
    }

    private PatchArea getCurrentArea(Device d) {
        return getCurrentSlot(d)
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
        path = path.setArea(areaId);
        return 1;
    }

    private List<String> getArgs(CmdDesc desc, CommandInput input) {
        return getArgs(desc,input,desc.getArgsDesc().size());
    }
    private List<String> getArgs(CmdDesc desc, CommandInput input, int reqd) {
        List<String> args = new ArrayList<>(Arrays.stream(input.args()).filter(s -> !s.isEmpty()).toList());
        if (args.size() != reqd) {
            throw bad(desc, "expected " + reqd + " arguments");
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

    public static Command mkCmd(String cmd, BiFunction<CmdDesc, CommandInput, Object> method, CmdDesc desc) {
        return mkCmd(cmd,method,NullCompleter.INSTANCE,desc);
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
        s.append(": " + desc.getMainDesc().getFirst());
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
