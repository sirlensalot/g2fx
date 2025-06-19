package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class PerformanceSettings {

    private final FieldValues fvs;

    public PerformanceSettings(FieldValues fvs) {
        this.fvs = fvs;
    }

    public int getSelectedSlot() {
        return Protocol.PerformanceSettings.SelectedSlot.intValue(fvs);
    }

    public void setSelectedSlot(int value) {
        fvs.update(Protocol.PerformanceSettings.SelectedSlot.value(value));
    }

    public int getKeyboardRangeEnabled() {
        return Protocol.PerformanceSettings.KeyboardRangeEnabled.intValue(fvs);
    }

    public void setKeyboardRangeEnabled(int value) {
        fvs.update(Protocol.PerformanceSettings.KeyboardRangeEnabled.value(value));
    }

    public int getMasterClock() {
        return Protocol.PerformanceSettings.MasterClock.intValue(fvs);
    }

    public void setMasterClock(int value) {
        fvs.update(Protocol.PerformanceSettings.MasterClock.value(value));
    }

    public int getMasterClockRun() {
        return Protocol.PerformanceSettings.MasterClockRun.intValue(fvs);
    }

    public void setMasterClockRun(int value) {
        fvs.update(Protocol.PerformanceSettings.MasterClockRun.value(value));
    }

    public SlotSettings getSlotSettings(Slot slot) {
        return new SlotSettings(
                Protocol.PerformanceSettings.Slots.subfieldsValue(fvs)
                        .get(slot.ordinal()));
    }
    

}
