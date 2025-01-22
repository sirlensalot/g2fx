package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

public class SlotSettings {

    private final FieldValues fvs;

    public SlotSettings(FieldValues fvs) {
        this.fvs = fvs;
    }

    public String getPatchName() {
        return Protocol.PerfSlot.PatchName.stringValueRequired(fvs);
    }

    public void setPatchName(String value) {
        fvs.update(Protocol.PerfSlot.PatchName.value(value));
    }

    public int getEnabled() {
        return Protocol.PerfSlot.Enabled.intValueRequired(fvs);
    }

    public void setEnabled(int value) {
        fvs.update(Protocol.PerfSlot.Enabled.value(value));
    }

    public int getKeyboard() {
        return Protocol.PerfSlot.Keyboard.intValueRequired(fvs);
    }

    public void setKeyboard(int value) {
        fvs.update(Protocol.PerfSlot.Keyboard.value(value));
    }

    public int getHold() {
        return Protocol.PerfSlot.Hold.intValueRequired(fvs);
    }

    public void setHold(int value) {
        fvs.update(Protocol.PerfSlot.Hold.value(value));
    }

    public int getBankIndex() {
        return Protocol.PerfSlot.BankIndex.intValueRequired(fvs);
    }

    public void setBankIndex(int value) {
        fvs.update(Protocol.PerfSlot.BankIndex.value(value));
    }

    public int getPatchIndex() {
        return Protocol.PerfSlot.PatchIndex.intValueRequired(fvs);
    }

    public void setPatchIndex(int value) {
        fvs.update(Protocol.PerfSlot.PatchIndex.value(value));
    }

    public int getKeyboardRangeFrom() {
        return Protocol.PerfSlot.KeyboardRangeFrom.intValueRequired(fvs);
    }

    public void setKeyboardRangeFrom(int value) {
        fvs.update(Protocol.PerfSlot.KeyboardRangeFrom.value(value));
    }

    public int getKeyboardRangeTo() {
        return Protocol.PerfSlot.KeyboardRangeTo.intValueRequired(fvs);
    }

    public void setKeyboardRangeTo(int value) {
        fvs.update(Protocol.PerfSlot.KeyboardRangeTo.value(value));
    }

}
