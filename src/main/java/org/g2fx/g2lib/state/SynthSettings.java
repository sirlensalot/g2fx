package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class SynthSettings implements LibProperty.FieldValuesChangeListener {

    private final LibProperty.FieldValuesLibProperties props;

    private final LibProperty<String> deviceName;
    private final LibProperty<Boolean> perfMode;

    public SynthSettings() {
        props = new LibProperty.FieldValuesLibProperties(this);
        deviceName = props.stringFieldProperty(Protocol.SynthSettings.DeviceName);
        perfMode = props.booleanFieldProperty(Protocol.SynthSettings.PerfMode);
        update(Protocol.SynthSettings.FIELDS.values(
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

    public void update(FieldValues fvs) {
        props.update(fvs);
    }


    public LibProperty<String> deviceName() { return deviceName; }

    public LibProperty<Boolean> perfMode() {
        return perfMode;
    }

    @Override
    public void changed(FieldValues fvs) throws Exception {
        //TODO
    }
}
