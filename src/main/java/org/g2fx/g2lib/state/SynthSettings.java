package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class SynthSettings {

    private final FieldValues fvs;

    private final LibProperty<String> deviceName;
    private final LibProperty<Boolean> perfMode;

    public SynthSettings(FieldValues fvs) {
        this.fvs = fvs;
        this.deviceName = LibProperty.stringFieldProperty(fvs, Protocol.SynthSettings.DeviceName);
        this.perfMode = LibProperty.booleanFieldProperty(fvs, Protocol.SynthSettings.PerfMode);
    }

    public SynthSettings() {
        this(Protocol.SynthSettings.FIELDS.values(
                Protocol.SynthSettings.DeviceName.value("[offline]"),
                Protocol.SynthSettings.PerfMode.value(1),
                Protocol.SynthSettings.Reserved0.value(0),
                Protocol.SynthSettings.Reserved1.value(0),
                Protocol.SynthSettings.PerfBank.value(0),
                Protocol.SynthSettings.PerfLocation.value(0),
                Protocol.SynthSettings.MemoryProtect.value(0),
                Protocol.SynthSettings.Reserved2.value(0),
                Protocol.SynthSettings.MidiChannelA.value(0),
                Protocol.SynthSettings.MidiChannelB.value(1),
                Protocol.SynthSettings.MidiChannelC.value(2),
                Protocol.SynthSettings.MidiChannelD.value(3),
                Protocol.SynthSettings.MidiChannelGlobal.value(0),
                Protocol.SynthSettings.SysExId.value(16),
                Protocol.SynthSettings.LocalOn.value(1),
                Protocol.SynthSettings.Reserved3.value(0),
                Protocol.SynthSettings.Reserved4.value(0),
                Protocol.SynthSettings.ProgramChangeReceive.value(1),
                Protocol.SynthSettings.ProgramChangeSend.value(0),
                Protocol.SynthSettings.Reserved5.value(0),
                Protocol.SynthSettings.ControllersReceive.value(1),
                Protocol.SynthSettings.ControllersSend.value(0),
                Protocol.SynthSettings.Reserved6.value(0),
                Protocol.SynthSettings.SendClock.value(0),
                Protocol.SynthSettings.IgnoreExternalClock.value(0),
                Protocol.SynthSettings.Reserved7.value(0),
                Protocol.SynthSettings.TuneCent.value(0),
                Protocol.SynthSettings.GlobalOctaveShiftActive.value(0),
                Protocol.SynthSettings.Reserved8.value(0),
                Protocol.SynthSettings.GlobalOctaveShift.value(0),
                Protocol.SynthSettings.TuneSemi.value(0),
                Protocol.SynthSettings.Reserved9.value(0),
                Protocol.SynthSettings.PedalPolarity.value(0),
                Protocol.SynthSettings.ReservedA.value(64),
                Protocol.SynthSettings.ControlPedalGain.value(0)));
    }

    public LibProperty<String> deviceName() { return deviceName; }

    public LibProperty<Boolean> perfMode() {
        return perfMode;
    }

    public int getPerfBank() {
        return Protocol.SynthSettings.PerfBank.intValue(fvs);
    }

    public void setPerfBank(int value) {
        fvs.update(Protocol.SynthSettings.PerfBank.value(value));
    }

    public int getPerfLocation() {
        return Protocol.SynthSettings.PerfLocation.intValue(fvs);
    }

    public void setPerfLocation(int value) {
        fvs.update(Protocol.SynthSettings.PerfLocation.value(value));
    }

    public boolean getMemoryProtect() {
        return Protocol.SynthSettings.MemoryProtect.booleanIntValue(fvs);
    }

    public void setMemoryProtect(boolean value) {
        fvs.update(Protocol.SynthSettings.MemoryProtect.value(value));
    }

    public int getMidiChannelA() {
        return Protocol.SynthSettings.MidiChannelA.intValue(fvs);
    }

    public void setMidiChannelA(int value) {
        fvs.update(Protocol.SynthSettings.MidiChannelA.value(value));
    }

    public int getMidiChannelB() {
        return Protocol.SynthSettings.MidiChannelB.intValue(fvs);
    }

    public void setMidiChannelB(int value) {
        fvs.update(Protocol.SynthSettings.MidiChannelB.value(value));
    }

    public int getMidiChannelC() {
        return Protocol.SynthSettings.MidiChannelC.intValue(fvs);
    }

    public void setMidiChannelC(int value) {
        fvs.update(Protocol.SynthSettings.MidiChannelC.value(value));
    }

    public int getMidiChannelD() {
        return Protocol.SynthSettings.MidiChannelD.intValue(fvs);
    }

    public void setMidiChannelD(int value) {
        fvs.update(Protocol.SynthSettings.MidiChannelD.value(value));
    }

    public int getMidiChannelGlobal() {
        return Protocol.SynthSettings.MidiChannelGlobal.intValue(fvs);
    }

    public void setMidiChannelGlobal(int value) {
        fvs.update(Protocol.SynthSettings.MidiChannelGlobal.value(value));
    }

    public int getSysExId() {
        return Protocol.SynthSettings.SysExId.intValue(fvs);
    }

    public void setSysExId(int value) {
        fvs.update(Protocol.SynthSettings.SysExId.value(value));
    }

    public boolean getLocalOn() {
        return Protocol.SynthSettings.LocalOn.booleanIntValue(fvs);
    }

    public void setLocalOn(boolean value) {
        fvs.update(Protocol.SynthSettings.LocalOn.value(value));
    }

    public boolean getProgramChangeReceive() {
        return Protocol.SynthSettings.ProgramChangeReceive.booleanIntValue(fvs);
    }

    public void setProgramChangeReceive(boolean value) {
        fvs.update(Protocol.SynthSettings.ProgramChangeReceive.value(value));
    }

    public boolean getProgramChangeSend() {
        return Protocol.SynthSettings.ProgramChangeSend.booleanIntValue(fvs);
    }

    public void setProgramChangeSend(boolean value) {
        fvs.update(Protocol.SynthSettings.ProgramChangeSend.value(value));
    }

    public boolean getControllersReceive() {
        return Protocol.SynthSettings.ControllersReceive.booleanIntValue(fvs);
    }

    public void setControllersReceive(boolean value) {
        fvs.update(Protocol.SynthSettings.ControllersReceive.value(value));
    }

    public boolean getControllersSend() {
        return Protocol.SynthSettings.ControllersSend.booleanIntValue(fvs);
    }

    public void setControllersSend(boolean value) {
        fvs.update(Protocol.SynthSettings.ControllersSend.value(value));
    }

    public boolean getSendClock() {
        return Protocol.SynthSettings.SendClock.booleanIntValue(fvs);
    }

    public void setSendClock(boolean value) {
        fvs.update(Protocol.SynthSettings.SendClock.value(value));
    }

    public boolean getIgnoreExternalClock() {
        return Protocol.SynthSettings.IgnoreExternalClock.booleanIntValue(fvs);
    }

    public void setIgnoreExternalClock(boolean value) {
        fvs.update(Protocol.SynthSettings.IgnoreExternalClock.value(value));
    }

    public int getTuneCent() {
        return Protocol.SynthSettings.TuneCent.intValue(fvs);
    }

    public void setTuneCent(int value) {
        fvs.update(Protocol.SynthSettings.TuneCent.value(value));
    }

    public boolean getGlobalOctaveShiftActive() {
        return Protocol.SynthSettings.GlobalOctaveShiftActive.booleanIntValue(fvs);
    }

    public void setGlobalOctaveShiftActive(boolean value) {
        fvs.update(Protocol.SynthSettings.GlobalOctaveShiftActive.value(value));
    }

    public int getGlobalOctaveShift() {
        return Protocol.SynthSettings.GlobalOctaveShift.intValue(fvs);
    }

    public void setGlobalOctaveShift(int value) {
        fvs.update(Protocol.SynthSettings.GlobalOctaveShift.value(value));
    }

    public int getTuneSemi() {
        return Protocol.SynthSettings.TuneSemi.intValue(fvs);
    }

    public void setTuneSemi(int value) {
        fvs.update(Protocol.SynthSettings.TuneSemi.value(value));
    }

    public boolean getPedalPolarity() {
        return Protocol.SynthSettings.PedalPolarity.booleanIntValue(fvs);
    }

    public void setPedalPolarity(boolean value) {
        fvs.update(Protocol.SynthSettings.PedalPolarity.value(value));
    }

    public int getControlPedalGain() {
        return Protocol.SynthSettings.ControlPedalGain.intValue(fvs);
    }

    public void setControlPedalGain(int value) {
        fvs.update(Protocol.SynthSettings.ControlPedalGain.value(value));
    }



}
