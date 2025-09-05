package org.g2fx.g2lib.repl;

import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Device;
import org.g2fx.g2lib.state.Patch;
import org.g2fx.g2lib.state.Slot;

public record Path(String device, String perf, SlotPatch slot, Integer variation,
                   AreaId area, NamedIndex<ModuleType> module, NamedIndex<NamedParam> param) {

    public record NamedIndex<T>(int index, String name, T meta) {
        @Override
        public String toString() {
            return index + ":" + name;
        }
    }

    public record SlotPatch(Slot slot, String name) {
        @Override
        public String toString() {
            return slot + ":" + name;
        }
    }

    static Path pathForPatch(Device cur, Patch patch) {
        return new Path(
                cur.online() ? cur.getSynthSettings().deviceName().get() : "offline",
                cur.getPerf().perfName().get(),
                new SlotPatch(patch.getSlot(),cur.getPerf().getPerfSettings().getSlotSettings(patch.getSlot()).patchName().get()),
                patch.getPatchSettings().variation().get(),
                AreaId.Voice,
                null,
                null
        );
    }
    public Path setModule(NamedIndex<ModuleType> m) { return new Path(device,perf,slot,variation,area,m,null);}
    public Path setParam(NamedIndex<NamedParam> m) { return new Path(device,perf,slot,variation,area,module,m);}
    public Path setVar(int v) { return new Path(device,perf,slot,v,area,module,param);}
    public Path setArea(AreaId a) { return new Path(device,perf,slot,variation,a,null,null); }

    @Override
    public String toString() {
        return device == null ? "[no device]" : perf == null ? "[no perf]" : String.format(
            "%s/%s%s",device,perf,slot == null ? "" : String.format(
                    "/%s%s",slot,area == null ? "" : String.format(
                            "/%s%s",area, variation == null ? "" : String.format(
                            "/v%s%s",variation+1,module == null ? "" : String.format(
                                    "/%s%s",module,param == null ? "" : param.toString())))));

    }
}
