package org.g2fx.g2lib.repl;

import org.g2fx.g2gui.Commands;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.Devices;
import org.g2fx.g2lib.state.Patch;
import org.g2fx.g2lib.util.SafeLookup;
import org.g2fx.g2lib.util.Util;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.TerminalBuilder;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.g2fx.g2lib.repl.EvalResult.evalContinue;

public class Eval {

    private final PrintWriter writer;
    private Logger log = Util.getLogger(getClass());

    public record Command(String cmd, CommandMethods methods, CmdDesc desc) { }
    
    private final LineReader reader;
    private final Devices devices;

    private final CommandRegistry commandRegistry;

    private boolean uiMode;
    private Commands ui;

    private Path path = null;
    
    public Eval(Devices devices, boolean interactive, PrintWriter writer, Commands commands) throws Exception {

        this.devices = devices;
        this.ui = commands;
        uiMode = commands != null;
        commandRegistry = new JlineCommandRegistry(){{
            Map<String, CommandMethods> methods = new HashMap<>();
            Arrays.stream(EvalCommand.values()).forEach(c -> methods.put(c.name(),mkCmd(c).methods()));
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

    public void toggleUI() {
        uiMode = ui != null && !uiMode;
    }

    public LineReader getReader() {
        return reader;
    }

    public PrintWriter getWriter() {
        return writer;
    }
    enum Comm {
        Start,
        Stop;
        public static final SafeLookup<String,Comm> LOOKUP = SafeLookup.makeLowerCaseNameLookup(values());
    }



    public static class InvalidCommandException extends RuntimeException {
        private final CmdDesc desc;
        public InvalidCommandException(CmdDesc desc,String msg) {
            super(msg);
            this.desc = desc;
        }
        public CmdDesc getDesc() { return desc; }
    }

    public Command mkCmd(EvalCommand ec) {
        return mkCmd(ec.name(),(c,i) ->
                ec.builder.cmd.apply(new EvalCtx(writer,devices,ui,path,c,i,uiMode,this)),
                ec.builder.completer,
                ec.builder.desc);
    }
    private static Command mkCmd(String cmd, BiFunction<CmdDesc, CommandInput, Object> method,
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

    public EvalResult runScript(BufferedReader fr) throws IOException {
        updatePath();
        String line;
        while ((line = fr.readLine()) != null) {
            if (line.startsWith("#")) continue;
            ParsedLine pl = reader.getParser().parse(line, 0);
            EvalResult r = handleInput(new ArrayList<>(pl.words()));
            if (r.type() != EvalResult.EvalResultType.Continue) {
                return r;
            }
        }
        return evalContinue();

    }


    public EvalResult handleInput(List<String> ws) {
        if (ws.isEmpty()) return evalContinue();
        String cmd = ws.removeFirst();
        if (!commandRegistry.hasCommand(cmd)) {
            getWriter().println("Invalid command: " + cmd);
            return evalContinue();
        }
        try {
            Object result = commandRegistry.invoke(
                    new CommandRegistry.CommandSession(reader.getTerminal()),
                    cmd,
                    ws.toArray());
            if (result instanceof EvalResult er) {
                if (er.isQuit()) {
                    getWriter().println("Exiting");
                    return er;
                } else if (er.isWait()) {
                    getWriter().println("Wait " + er.waitMs());
                    return er;
                }
            }

        } catch (InvalidCommandException e) {
            usageError(e, cmd);
        } catch (Exception e) {
            if (e.getCause() instanceof InvalidCommandException ice) {
                usageError(ice,cmd);
            } else {
                log.log(Level.SEVERE, "failure", e);
            }
        }
        return evalContinue();
    }

    private void usageError(InvalidCommandException e, String cmd) {
        getWriter().format("Invalid command: %s\nUsage: %s\n",
                e.getMessage(),
                EvalCtx.usage(e.getDesc(), cmd)
        );
    }

    public void setPath(Path path) {
        this.path = path;
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

    @Override
    public String toString() {
        return String.format("%s%s",
                path == null ? "offline" : path,
                ui == null ? "" : " uiMode=" + uiMode);
    }
}
