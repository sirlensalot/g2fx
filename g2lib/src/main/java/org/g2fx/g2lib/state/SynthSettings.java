package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class SynthSettings {

    private final FieldValues fvs;

    public SynthSettings(FieldValues fvs) {
        this.fvs = fvs;
    }

    public String getDeviceName() {
        return Protocol.SynthSettings.DeviceName.stringValue(fvs);
    }

    public void setDeviceName(int value) {
        fvs.update(Protocol.SynthSettings.DeviceName.value(value));
    }

    public boolean getPerfMode() {
        return Protocol.SynthSettings.PerfMode.booleanIntValue(fvs);
    }

    public void setPerfMode(boolean value) {
        fvs.update(Protocol.SynthSettings.PerfMode.value(value));
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
