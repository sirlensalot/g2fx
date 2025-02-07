package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

public class PerformanceSettings {

    private final FieldValues fvs;

    public PerformanceSettings(FieldValues fvs) {
        this.fvs = fvs;
    }

    public int getSelectedSlot() {
        return Protocol.PerformanceSettings.SelectedSlot.intValueRequired(fvs);
    }

    public void setSelectedSlot(int value) {
        fvs.update(Protocol.PerformanceSettings.SelectedSlot.value(value));
    }

    public int getKeyboardRangeEnabled() {
        return Protocol.PerformanceSettings.KeyboardRangeEnabled.intValueRequired(fvs);
    }

    public void setKeyboardRangeEnabled(int value) {
        fvs.update(Protocol.PerformanceSettings.KeyboardRangeEnabled.value(value));
    }

    public int getMasterClock() {
        return Protocol.PerformanceSettings.MasterClock.intValueRequired(fvs);
    }

    public void setMasterClock(int value) {
        fvs.update(Protocol.PerformanceSettings.MasterClock.value(value));
    }

    public int getMasterClockRun() {
        return Protocol.PerformanceSettings.MasterClockRun.intValueRequired(fvs);
    }

    public void setMasterClockRun(int value) {
        fvs.update(Protocol.PerformanceSettings.MasterClockRun.value(value));
    }

    public SlotSettings getSlotSettings(Slot slot) {
        return new SlotSettings(
                Protocol.PerformanceSettings.Slots.subfieldsValueRequired(fvs)
                        .get(slot.ordinal()));
    }
    

}
