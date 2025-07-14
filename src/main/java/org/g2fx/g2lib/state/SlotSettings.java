package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class SlotSettings {

    private final FieldValues fvs;

    private final LibProperty<Boolean> enabled;
    private final LibProperty<Boolean> keyboard;
    private final LibProperty<String> patchName;
    private final LibProperty<Boolean> hold;
    private final LibProperty<Integer> bankIndex;
    private final LibProperty<Integer> patchIndex;
    private final LibProperty<Integer> keyboardRangeFrom;
    private final LibProperty<Integer> keyboardRangeTo;

    public SlotSettings(FieldValues fvs) {
        this.fvs = fvs;
        enabled = LibProperty.booleanFieldProperty(fvs, Protocol.PerfSlot.Enabled);
        keyboard = LibProperty.booleanFieldProperty(fvs, Protocol.PerfSlot.Keyboard);
        patchName = LibProperty.stringFieldProperty(fvs, Protocol.PerfSlot.PatchName);
        hold = LibProperty.booleanFieldProperty(fvs, Protocol.PerfSlot.Hold);
        bankIndex = LibProperty.intFieldProperty(fvs, Protocol.PerfSlot.BankIndex);
        patchIndex = LibProperty.intFieldProperty(fvs, Protocol.PerfSlot.PatchIndex);
        keyboardRangeFrom = LibProperty.intFieldProperty(fvs, Protocol.PerfSlot.KeyboardRangeFrom);
        keyboardRangeTo = LibProperty.intFieldProperty(fvs, Protocol.PerfSlot.KeyboardRangeTo);
    }

    public LibProperty<Boolean> enabled() { return enabled; }
    public LibProperty<Boolean> keyboard() { return keyboard; }
    public LibProperty<String> patchName() { return patchName; }
    public LibProperty<Boolean> hold() {
        return hold;
    }

    public LibProperty<Integer> bankIndex() {
        return bankIndex;
    }

    public LibProperty<Integer> patchIndex() {
        return patchIndex;
    }

    public LibProperty<Integer> keyboardRangeFrom() {
        return keyboardRangeFrom;
    }

    public LibProperty<Integer> keyboardRangeTo() {
        return keyboardRangeTo;
    }
}
