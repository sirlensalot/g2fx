package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class PerformanceSettings {

    private final FieldValues fvs;

    private final LibProperty<Integer> masterClock;
    private final LibProperty<Integer> selectedSlot;

    public PerformanceSettings(FieldValues fvs) {
        this.fvs = fvs;
        this.masterClock = LibProperty.intFieldProperty(fvs, Protocol.PerformanceSettings.MasterClock);
        this.selectedSlot = LibProperty.intFieldProperty(fvs, Protocol.PerformanceSettings.SelectedSlot);
    }

    public LibProperty<Integer> selectedSlot() {
        return selectedSlot;
    }

    public int getKeyboardRangeEnabled() {
        return Protocol.PerformanceSettings.KeyboardRangeEnabled.intValue(fvs);
    }

    public void setKeyboardRangeEnabled(int value) {
        fvs.update(Protocol.PerformanceSettings.KeyboardRangeEnabled.value(value));
    }

    public LibProperty<Integer> masterClock() { return masterClock; }

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
