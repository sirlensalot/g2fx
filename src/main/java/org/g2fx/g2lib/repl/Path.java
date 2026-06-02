package org.g2fx.g2lib.repl;

import org.g2fx.g2lib.device.Device;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Patch;
import org.g2fx.g2lib.state.Performance;
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

    static Path pathForPatch(Performance perf, Patch patch) {
        return new Path(
                "[Device]",//cur.online() ? cur.getSynthSettings().deviceName().get() : "offline",
                perf.perfName().get(),
                new SlotPatch(patch.getSlot(),perf.getPerfSettings().getSlotSettings(patch.getSlot()).patchName().get()),
                patch.getPatchSettings().variation().get(),
                patch.getPatchSettings().height().get() == 0 ? AreaId.Fx : AreaId.Voice,
                null,
                null
        );
    }

    public static Path mkPath(Device device, Performance perf) {
        Patch patch = perf != null ? perf.getSelectedPatch() : null;
        return new Path(
                device == null || !device.online() ? null : device.getSynthSettings().deviceName().get(),
                perf == null ? null : perf.perfName().get(),
                patch == null ? null : new SlotPatch(patch.getSlot(),patch.name().get()),
                patch == null ? null : patch.getPatchSettings().variation().get(),
                patch == null ? null : patch.getPatchSettings().height().get() == 0 ? AreaId.Fx : AreaId.Voice,
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
        return String.format("%s%s%s%s%s%s",
                device == null ? "[offline]" : device,
                perf==null ? "/[no perf]" : "/" + perf,
                slot == null ? "" : "/" + slot + (variation == null ? "" : "/v" + variation),
                area == null ? "" : "/" + area.shortName(),
                module == null ? "" : "/" + module,
                param == null ? "" : ": " + param);

    }
}
