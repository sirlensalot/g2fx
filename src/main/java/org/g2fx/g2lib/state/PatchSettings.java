package org.g2fx.g2lib.state;

import org.g2fx.g2gui.controls.VoiceMode;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.UsbSlotSender;
import org.g2fx.g2lib.util.Util;

import java.util.logging.Logger;

public class PatchSettings implements LibProperty.FieldValuesChangeListener {

    private final LibProperty.FieldValuesLibProperties props = new LibProperty.FieldValuesLibProperties(this);

    private final LibProperty<Integer> voices;
    private final LibProperty<Integer> height;
    private final LibProperty<Boolean> red;
    private final LibProperty<Boolean> blue;
    private final LibProperty<Boolean> yellow;
    private final LibProperty<Boolean> orange;
    private final LibProperty<Boolean> green;
    private final LibProperty<Boolean> purple;
    private final LibProperty<Boolean> white;
    private final LibProperty<Integer> monoPoly;
    private final LibProperty<Integer> variation;
    private final LibProperty<Integer> category;

    private final LibProperty<VoiceMode> voiceMode;
    private final Logger log;
    private final UsbSlotSender sender;

    public PatchSettings(Slot slot, UsbSlotSender sender) {
        this.log = Util.getLogger(getClass(),slot);
        this.sender = sender;
        voices = props.intFieldProperty(Protocol.PatchDescription.Voices, false);
        height = props.intFieldProperty(Protocol.PatchDescription.Height);
        red = props.booleanFieldProperty(Protocol.PatchDescription.Red);
        blue = props.booleanFieldProperty(Protocol.PatchDescription.Blue);
        yellow = props.booleanFieldProperty(Protocol.PatchDescription.Yellow);
        orange = props.booleanFieldProperty(Protocol.PatchDescription.Orange);
        green = props.booleanFieldProperty(Protocol.PatchDescription.Green);
        purple = props.booleanFieldProperty(Protocol.PatchDescription.Purple);
        white = props.booleanFieldProperty(Protocol.PatchDescription.White);
        monoPoly = props.intFieldProperty(Protocol.PatchDescription.MonoPoly, false);
        variation = props.intFieldProperty(Protocol.PatchDescription.Variation, false);
        category = props.intFieldProperty(Protocol.PatchDescription.Category);

        voiceMode = props.register(new LibProperty<>(new LibProperty.LibPropertyGetterSetter<>() {
            @Override
            public VoiceMode get() {
                return VoiceMode.fromMonoPolyAndVoices(monoPoly().get(), voices().get());
            }

            @Override
            public void set(VoiceMode newValue) {
                monoPoly.set(newValue.getMonoPoly());
                voices.set(newValue.getVoices());
            }
        }));

    }

    public void update(FieldValues fvs) {
        props.update(fvs);
    }

    @Override
    public void changed(FieldValues fvs) throws Exception {
        log.info(() -> "sending patch settings");
        Protocol.PatchDescription.Reserved.subfieldsValue(fvs).forEach(sfvs -> sfvs.update(Protocol.Data8.Datum.value(0)));
        fvs.update(Protocol.PatchDescription.Reserved2.value(0));
        sender.sendSectionMessage(new Patch.Section(Sections.SPatchDescription_21,fvs));
    }

    /*
    function TG2MessSlot.CreateSelectVariationMessage( aVariationIndex: byte): TG2SendMessage;
begin
  add_log_line('Select variation ' + IntToStr(aVariationIndex) + ', slot ' + IntToStr(SlotIndex) + ', patch_version ' + IntToStr(FPatchVersion), LOGCMD_HDR);

  Result := TG2SendMessage.Create;
  Result.WriteMessage( $01);
  Result.WriteMessage( CMD_REQ + CMD_SLOT + SlotIndex );
  Result.WriteMessage( FPatchVersion);
  Result.WriteMessage( S_SEL_VARIATION); 0x6a
  Result.WriteMessage( aVariationIndex);
end;


     */

    public LibProperty<Integer> voices() { return voices; }
    public LibProperty<Integer> height() { return height; }
    public LibProperty<Boolean> red() { return red; }
    public LibProperty<Boolean> blue() { return blue; }
    public LibProperty<Boolean> yellow() { return yellow; }
    public LibProperty<Boolean> orange() { return orange; }
    public LibProperty<Boolean> green() { return green; }
    public LibProperty<Boolean> purple() { return purple; }
    public LibProperty<Boolean> white() { return white; }
    public LibProperty<Integer> monoPoly() { return monoPoly; }
    public LibProperty<Integer> variation() { return variation; }
    public LibProperty<Integer> category() { return category; }

    public LibProperty<VoiceMode> voiceMode() {
        return voiceMode;
    }
}
