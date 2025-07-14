package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.Arrays;
import java.util.List;

public class PerformanceSettings {

    private final FieldValues fvs;

    private final LibProperty<Integer> masterClock;
    private final LibProperty<Boolean> masterClockRun;
    private final LibProperty<Boolean> keyboardRangeEnabled;
    private final LibProperty<Integer> selectedSlot;

    private final List<SlotSettings> slotSettings;

    public PerformanceSettings(FieldValues fvs) {
        this.fvs = fvs;
        this.masterClock =
                LibProperty.intFieldProperty(fvs, Protocol.PerformanceSettings.MasterClock);
        this.selectedSlot =
                LibProperty.intFieldProperty(fvs, Protocol.PerformanceSettings.SelectedSlot);
        this.masterClockRun =
                LibProperty.booleanFieldProperty(fvs, Protocol.PerformanceSettings.MasterClockRun);
        this.keyboardRangeEnabled =
                LibProperty.booleanFieldProperty(fvs, Protocol.PerformanceSettings.KeyboardRangeEnabled);
        slotSettings = Arrays.stream(Slot.values()).map(s -> new SlotSettings(
                Protocol.PerformanceSettings.Slots.subfieldsValue(fvs).get(s.ordinal())
        )).toList();
    }

    public LibProperty<Integer> selectedSlot() { return selectedSlot; }
    public LibProperty<Integer> masterClock() { return masterClock; }
    public LibProperty<Boolean> masterClockRun() { return masterClockRun; }
    public LibProperty<Boolean> keyboardRangeEnabled() { return keyboardRangeEnabled; }


    public SlotSettings getSlotSettings(Slot slot) {
        return slotSettings.get(slot.ordinal());
    }
    

}
