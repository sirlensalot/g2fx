package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.Arrays;
import java.util.List;

import static org.g2fx.g2lib.protocol.Protocol.PerfSlot.*;
import static org.g2fx.g2lib.protocol.Protocol.PerformanceSettings.*;

public class PerformanceSettings {

    private final FieldValues fvs;

    private final LibProperty<Integer> masterClock;
    private final LibProperty<Boolean> masterClockRun;
    private final LibProperty<Boolean> keyboardRangeEnabled;
    private final LibProperty<Integer> selectedSlot;

    private final List<SlotSettings> slotSettings;

    // file-perf
    public PerformanceSettings(FieldValues fvs) {
        this.fvs = fvs;
        this.masterClock =
                LibProperty.intFieldProperty(fvs, MasterClock);
        this.selectedSlot =
                LibProperty.intFieldProperty(fvs, SelectedSlot);
        this.masterClockRun =
                LibProperty.booleanFieldProperty(fvs, MasterClockRun);
        this.keyboardRangeEnabled =
                LibProperty.booleanFieldProperty(fvs, KeyboardRangeEnabled);
        slotSettings = Arrays.stream(Slot.values()).map(s -> new SlotSettings(
                Slots.subfieldsValue(fvs).get(s.ordinal())
        )).toList();
    }

    public PerformanceSettings() {
        this(Protocol.PerformanceSettings.FIELDS.values(
                Unknown1.value(0),
                SelectedSlot.value(0),
                Unknown2.value(0),
                KeyboardRangeEnabled.value(0),
                MasterClock.value(0x78),
                Unknown3.value(0),
                MasterClockRun.value(0),
                Unknown4.value(0),
                Slots.value(Arrays.stream(Slot.values()).map(s ->
                        Protocol.PerfSlot.FIELDS.values(
                                PatchName.value("No name"),
                                Enabled.value(1),
                                Keyboard.value(s == Slot.A ? 1 : 0),
                                Hold.value(0),
                                BankIndex.value(0),
                                PatchIndex.value(0),
                                KeyboardRangeFrom.value(0),
                                KeyboardRangeTo.value(0x7f),
                                Unknown.value(0)
                        )
                ).toList())
        ));
    }

    public LibProperty<Integer> selectedSlot() { return selectedSlot; }
    public LibProperty<Integer> masterClock() { return masterClock; }
    public LibProperty<Boolean> masterClockRun() { return masterClockRun; }
    public LibProperty<Boolean> keyboardRangeEnabled() { return keyboardRangeEnabled; }


    public SlotSettings getSlotSettings(Slot slot) {
        return slotSettings.get(slot.ordinal());
    }

    public FieldValues getFieldValues() {
        return fvs;
    }
}
