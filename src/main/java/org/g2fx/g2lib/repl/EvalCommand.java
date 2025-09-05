package org.g2fx.g2lib.repl;

import org.g2fx.g2gui.controls.IndexParam;
import org.g2fx.g2lib.model.ModParam;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.state.*;
import org.g2fx.g2lib.util.SafeLookup;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.utils.AttributedString;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.g2fx.g2lib.repl.EvalResult.evalContinue;
import static org.g2fx.g2lib.util.Util.forEachIndexed;
import static org.g2fx.g2lib.util.Util.notNull;

public enum EvalCommand {
    ui(cmd("Toggle UI mode (slot selection affects UI, etc)")
            .run(c -> c.eval.toggleUI())),

    exit(cmd("Exit program")
            .eval(c -> new EvalResult(EvalResult.EvalResultType.Quit,0))),

    listEntries(cmd("List bank or patch entries",
            argDesc("perfOrPatch","'perf' or 'patch'"),
            argDesc("index","bank index: 1-8 for perfs, 1-32 for patches"))
            .run(c -> {
                UsbDevice.EntryType type = UsbDevice.EntryType.LC_NAME_LOOKUP.get(c.nextArg());
                int bank = c.nextInt() - 1;
                c.devices.runWithCurrent(d -> d.dumpEntries(c.writer,type,bank));
            })),

    fileLoad(cmd("Load perf or patch file",
            argDesc("file","File ending in .pch2 or .prf2"))
            .run(c -> {
                String filePath = c.nextArg();
                if (!new File(filePath).isFile()) {
                    throw c.bad("not a file");
                }
                if (!(filePath.endsWith("prf2") || filePath.endsWith("pch2"))) {
                    throw c.bad("Not a G2 file");
                }
                c.eval.setPath(c.devices.invoke(true,() -> {
                    c.devices.loadFile(filePath);
                    return c.devices.withCurrent(d -> Path.pathForPatch(d,d.getPerf().getSelectedPatch()));
                }));
            })),

    va(cmd("Switch to voice area").run(c -> c.area(AreaId.Voice))),
    fx(cmd("Switch to FX area").run(c -> c.area(AreaId.Fx))),

    wait(cmd("Pause process",argDesc("time","time in millis")).eval(c -> {
        int wait = c.nextInt();
        if (c.ui != null) return new EvalResult(EvalResult.EvalResultType.Wait,wait);
        try {
            Thread.sleep(wait);
        } catch (InterruptedException ignore) {}
        return evalContinue();
    })),

    slot(cmd("Set current slot",argDesc("slot","a-d"))
            .completer(new StringsCompleter(
                    Arrays.stream(Slot.values()).map(s -> s.toString().toLowerCase()).toList()))
            .run(c -> {
                Slot s = Slot.fromAlpha(c.nextArg());
                if (c.path == null || c.path.perf() == null ) { throw c.bad("No current performance"); }
                Path p = c.devices.invokeWithCurrent(d -> Path.pathForPatch(d, d.getPerf().getSlot(s)));
                c.eval.setPath(p);
                if (c.uiMode) {
                    c.ui.setSlot(p.slot().slot());
                }
            })),

    load(cmd("Load entry into device",
            argDesc("typeOrSlot","'perf' or slot (A,B,C,D)"),
            argDesc("bank","bank number"),
            argDesc("entry","bank entry number"))
            .run(c -> {
                String typeOrSlot = c.nextArg();
                int slotCode;
                if (typeOrSlot.equals("perf")) {
                    slotCode = 4;
                } else {
                    slotCode = Slot.fromAlpha(typeOrSlot.toUpperCase()).ordinal();
                }
                int bank = c.nextInt() - 1;
                int entry = c.nextInt() - 1;
                c.devices.runWithCurrent(d -> d.loadEntry(slotCode, bank, entry));
            })),

    echo(cmd("echo args").run(c -> {
        for (EvalCtx.ArgAndDesc arg : c.args()) {
            c.writer.print(arg);
            c.writer.print(" ");
        }
        c.writer.println();
    })),

    path(cmd("echo path").run(c -> c.writer.println(c.path == null ? "None" : c.path))),

    list(cmd("context-sensitive list").run(c -> {
        if (c.path == null) { c.writer.println("No path"); return; }
        if (c.path.area() == null) { c.writer.println("No area"); return; }
        if (c.path.module() == null) {
            c.writer.println((String)c.devices.invokeWithCurrent(d ->
                    String.join("\n", c.getCurrentArea(d).getModules().stream().map(m ->
                            String.format("%s:%s (%s)", m.getIndex(), m.name().get(),
                                    m.getUserModuleData().getType().shortName)).toList())));
            return;
        }
        c.devices.runWithCurrent(d -> {
            PatchModule m = c.getCurrentModule(d);

            c.writer.println("Params:");
            List<Integer> vvs = m.getVarValues(c.path.variation());
            forEachIndexed(vvs,(v,i) ->
                    c.writer.format("  %s:%s\n", m.getUserModuleData().getType().getParams().get(i).name(), v));

            c.writer.println("LEDs:");
            Patch patch = c.getCurrentSlot(d);
            forEachIndexed(patch.getLeds(), (pv,ix) -> {
                if (notNull(pv).getModule() == m) {
                    c.writer.format("  %s: %s\n", ix, pv);
                }
            });
            c.writer.println("Meters/Groups:");
            forEachIndexed(patch.getMetersAndGroups(), (pv,ix) -> {
                if (notNull(pv).getModule() == m) {
                    c.writer.format("  %s: %s\n", ix, pv);
                }
            });
        });

    })),
    var(cmd("switch variation",argDesc("idx","variation index")).run(c -> {
        if (c.path == null || c.path.slot() == null) { throw c.bad("No current path/slot"); }
        int v = c.nextInt();
        if (v < 1 || v > PatchModule.MAX_VARIATIONS) { throw c.bad("Invalid variation"); }
        c.eval.setPath(c.path.setVar(v-1));
        if (c.uiMode) {
            c.ui.setVar(v-1);
        }
    })),
    mod(cmd("switch to module", argDesc("nameOrIdx","module name or index"))
            .run(c -> {
                if (c.path == null || c.path.area() == null) { throw c.bad("Path null/no area"); }
                EvalCtx.ArgAndDesc a = c.popArg();
                Path p = c.devices.invokeWithCurrent(d -> {
                    PatchModule m = null;
                    for (PatchModule pm : c.getCurrentArea(d).getModules()) {
                        if (a.arg().equals(pm.name().get())) {
                            m = pm;
                        }
                    }
                    if (m == null) {
                        m = c.getCurrentArea(d).getModule(c.parseInt(a));
                    }
                    return c.path.setModule(new Path.NamedIndex<>(m.getIndex(), m.name().get(), m.getUserModuleData().getType()));
                });
                c.eval.setPath(p);
                if (c.uiMode) { c.ui.selectModule(p.slot().slot(), p.area(), p.module().index()); }
            })),
    pset(cmd("set param value",
            argDesc("idxOrName","param index or name"),
            argDesc("val","param value")).run(c -> {
        if (c.path == null || c.path.module() == null) { throw c.bad("No current path/module"); }
        List<NamedParam> ps = c.path.module().meta().getParams();
        IndexParam np = null;
        String iOn = c.nextArg();
        for (int i = 0 ; i < ps.size(); i++) {
            NamedParam p = ps.get(i);
            if (p.name().equals(iOn)) { np = new IndexParam(p,i,""); }
        }
        if (np == null) {
            int ix = c.nextInt();
            if (ix >= ps.size()) { throw c.bad("Invalid mod index: %s", ix); }
            np = new IndexParam(ps.get(ix),ix,"");
        }
        int val = c.nextInt();
        ModParam mp = np.param().param();
        if (val < mp.min || val > mp.max) {
            throw c.bad("Invalid param value %s, should be [%s,%s)",val,mp.min,mp.max);
        }
        final var npp = np;
        c.devices.runWithCurrent(d -> c.getCurrentModule(d)
                .getParamValueProperty(c.path.variation(),npp.index()).set(val));

    })),
    pload(cmd("set patch load meter",
            argDesc("area","patch area (fx|va)"),
            argDesc("meter","meter index (mem|cyc)"),
            argDesc("value","load value")).run(c -> {
        var area = "va".equals(c.nextArg()) ? AreaId.Voice : AreaId.Fx;
        var isMem = "mem".equals(c.nextArg());
        double val = c.nextInt();
        c.devices.runWithCurrent(d -> Arrays.stream(AreaId.USER_AREAS).forEach(a -> {
            PatchLoadData data = c.getCurrentSlot(d).getArea(area).getPatchLoadData();
            if (isMem) data.mem().set(val); else data.cycles().set(val);
        }));
    })),
    help(cmd("Command help").run(EvalCtx::help)),
    led(cmd("toggle green led",
            argDesc("name","led name"))
            .run(c -> {
                String name = c.nextArg();
                c.devices.runWithCurrent(d -> {
                    PatchModule pm = c.getCurrentModule(d);
                    c.getCurrentSlot(d).getLeds().forEach(v -> {
                        if (v.getModule() == pm && v.getVisual().names().getFirst().equals(name)) {
                            int nv = v.value().get() == 0 ? 1 : 0;
                            v.value().set(nv);
                        }
                    });
                });
            })),
    comm(cmd("Start/stop device communication stream",
            argDesc("startStop","start or stop"))
            .run(c -> {
                Eval.Comm comm = Eval.Comm.LOOKUP.get(c.nextArg().toLowerCase());
                c.devices.runWithCurrent(d -> d.sendStartStopComm(comm == Eval.Comm.Start));
            }));

    public static final SafeLookup<String,EvalCommand> BY_NAME =
            SafeLookup.makeEnumNameLookup(values());

    public static class Builder {
        public CmdDesc desc;
        public Function<EvalCtx, EvalResult> cmd;
        public Completer completer = NullCompleter.INSTANCE;

        public Builder desc(CmdDesc desc) { this.desc = desc; return this; }
        public Builder run(Consumer<EvalCtx> cmd) { this.cmd = adaptConsumer(cmd); return this; }
        public Builder eval(Function<EvalCtx, EvalResult> cmd) { this.cmd = cmd; return this; }
        public Builder completer(Completer completer) { this.completer = completer; return this; }
    }

    public static Builder cmd(String desc, ArgDesc... args) {
        return cmd(cmdDesc(desc,args));
    }
    public static Builder cmd(CmdDesc desc) {
        return new Builder().desc(desc);
    }
    public final Builder builder;
    EvalCommand(Builder builder) {
        this.builder = builder;
    }

    private static Function<EvalCtx, EvalResult> adaptConsumer(Consumer<EvalCtx> cmd) {
        return c -> {
            cmd.accept(c);
            return evalContinue();
        };
    }

    public static CmdDesc cmdDesc(String desc, ArgDesc... args) {
        return new CmdDesc(List.of(new AttributedString(desc)),List.of(args), Map.of());
    }

    public static ArgDesc argDesc(String argName, String... lines) {
        return new ArgDesc(argName, Arrays.stream(lines).map(AttributedString::new).toList());
    }


}
