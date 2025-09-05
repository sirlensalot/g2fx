package org.g2fx.g2lib.repl;

import com.google.common.collect.Streams;
import org.g2fx.g2gui.Commands;
import org.g2fx.g2lib.state.*;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.utils.AttributedString;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EvalCtx {

    public final PrintWriter writer;
    public final Devices devices;
    public final Commands ui;
    public final CmdDesc desc;
    public final Path path;
    public record ArgAndDesc (String arg, ArgDesc desc) {}
    private final List<ArgAndDesc> args;
    public final Eval eval;
    public final boolean uiMode;

    public EvalCtx(PrintWriter writer, Devices devices, Commands ui, Path path, CmdDesc desc, CommandInput input, boolean uiMode, Eval eval) {
        this.writer = writer;
        this.devices = devices;
        this.ui = ui;
        this.path = path;
        this.desc = desc;
        this.eval = eval;
        this.uiMode = uiMode;
        this.args = initArgs(desc,input); // can fail so must be last
    }

    public String nextArg() {
        return popArg().arg();
    }

    public ArgAndDesc popArg() {
        if (args.isEmpty()) { throw bad("Expected arg"); }
        return args.removeFirst();
    }

    public List<ArgAndDesc> args() {
        return args;
    }

    public int nextInt() {
        return parseInt(popArg());
    }
    public int parseInt(ArgAndDesc ad) {
        try {
            return Integer.parseInt(ad.arg());
        } catch (Exception ignore) {
            throw bad(ad.desc().getName() + ": expected integer: " + ad.arg);
        }
    }

    private List<ArgAndDesc> initArgs(CmdDesc desc, CommandInput input) {
        List<String> words = new ArrayList<>(Arrays.stream(input.args()).filter(s -> !s.isEmpty()).toList());
        List<ArgDesc> ads = desc.getArgsDesc();
        if (words.size() != ads.size()) {
            throw bad("expected " + ads.size() + " arguments");
        }
        List<ArgAndDesc> as = new ArrayList<>();
        Streams.forEachPair(words.stream(), ads.stream(), (v, d) -> as.add(new ArgAndDesc(v, d)));
        return as;
    }

    public Eval.InvalidCommandException bad(String msg, Object... args) {
        return new Eval.InvalidCommandException(desc, String.format(msg,args));
    }


    public void area(AreaId areaId) {
        if (path == null || path.slot() == null) { throw bad("No current patch"); }
        eval.setPath(path.setArea(areaId));
        if (uiMode) ui.setArea(areaId);
    }

    public PatchModule getCurrentModule(Device d) {
        return getCurrentArea(d).getModule(path.module().index());
    }
    public PatchArea getCurrentArea(Device d) {
        return getCurrentSlot(d)
                .getArea(path.area());
    }
    public Patch getCurrentSlot(Device d) {
        return d.getPerf().getSlot(path.slot().slot());
    }


    public void help() {
        for (EvalCommand cmd : EvalCommand.values()) {
            writer.println(usage(cmd.builder.desc, cmd.name()));
        }
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

}
