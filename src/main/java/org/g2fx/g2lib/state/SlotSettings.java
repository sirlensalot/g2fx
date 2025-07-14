package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class SlotSettings {

    private final FieldValues fvs;

    private final LibProperty<Boolean> enabled;
    private final LibProperty<Boolean> keyboard;
    private final LibProperty<String> patchName;

    public SlotSettings(FieldValues fvs) {
        this.fvs = fvs;
        enabled = LibProperty.booleanFieldProperty(fvs, Protocol.PerfSlot.Enabled);
        keyboard = LibProperty.booleanFieldProperty(fvs, Protocol.PerfSlot.Keyboard);
        patchName = LibProperty.stringFieldProperty(fvs, Protocol.PerfSlot.PatchName);
    }

    public LibProperty<Boolean> enabled() { return enabled; }
    public LibProperty<Boolean> keyboard() { return keyboard; }
    public LibProperty<String> patchName() { return patchName; }

    public String getPatchName() {
        return Protocol.PerfSlot.PatchName.stringValue(fvs);
    }

    public void setPatchName(String value) {
        fvs.update(Protocol.PerfSlot.PatchName.value(value));
    }

    public int getEnabled() {
        return Protocol.PerfSlot.Enabled.intValue(fvs);
    }

    public void setEnabled(int value) {
        fvs.update(Protocol.PerfSlot.Enabled.value(value));
    }

    public int getKeyboard() {
        return Protocol.PerfSlot.Keyboard.intValue(fvs);
    }

    public void setKeyboard(int value) {
        fvs.update(Protocol.PerfSlot.Keyboard.value(value));
    }

    public int getHold() {
        return Protocol.PerfSlot.Hold.intValue(fvs);
    }

    public void setHold(int value) {
        fvs.update(Protocol.PerfSlot.Hold.value(value));
    }

    public int getBankIndex() {
        return Protocol.PerfSlot.BankIndex.intValue(fvs);
    }

    public void setBankIndex(int value) {
        fvs.update(Protocol.PerfSlot.BankIndex.value(value));
    }

    public int getPatchIndex() {
        return Protocol.PerfSlot.PatchIndex.intValue(fvs);
    }

    public void setPatchIndex(int value) {
        fvs.update(Protocol.PerfSlot.PatchIndex.value(value));
    }

    public int getKeyboardRangeFrom() {
        return Protocol.PerfSlot.KeyboardRangeFrom.intValue(fvs);
    }

    public void setKeyboardRangeFrom(int value) {
        fvs.update(Protocol.PerfSlot.KeyboardRangeFrom.value(value));
    }

    public int getKeyboardRangeTo() {
        return Protocol.PerfSlot.KeyboardRangeTo.intValue(fvs);
    }

    public void setKeyboardRangeTo(int value) {
        fvs.update(Protocol.PerfSlot.KeyboardRangeTo.value(value));
    }

}
