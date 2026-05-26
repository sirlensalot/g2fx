package org.g2fx.g2lib.state;

import org.g2fx.g2gui.controls.VoiceMode;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.protocol.Sections;
import org.g2fx.g2lib.usb.UsbSlotSender;
import org.g2fx.g2lib.util.Util;

import java.util.logging.Logger;

import static org.g2fx.g2lib.protocol.Protocol.PatchDescription.*;

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
        voices = props.intFieldProperty(Voices, false);
        height = props.intFieldProperty(Height);
        red = props.booleanFieldProperty(Red);
        blue = props.booleanFieldProperty(Blue);
        yellow = props.booleanFieldProperty(Yellow);
        orange = props.booleanFieldProperty(Orange);
        green = props.booleanFieldProperty(Green);
        purple = props.booleanFieldProperty(Purple);
        white = props.booleanFieldProperty(White);
        monoPoly = props.intFieldProperty(MonoPoly, false);
        variation = props.intFieldProperty(Variation, false);
        category = props.intFieldProperty(Category);

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
        Reserved.subfieldsValue(fvs).forEach(sfvs -> sfvs.update(Protocol.Data8.Datum.value(0)));
        fvs.update(Reserved2.value(0));
        log.info(() -> "sending patch settings: " + fvs);
        sender.sendSectionMessage(new Sections.Section(Sections.SPatchDescription_21,fvs));
    }

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

    public void initNew() {
        update(FIELDS.values(
                Reserved.value(Protocol.Data8.asSubfield(0,0,0,0,0,0,0)),
                Reserved2.value(0),
                Voices.value(1),
                Height.value(0x258),
                Unk2.value(2),
                Red.value(1),
                Blue.value(1),
                Yellow.value(1),
                Orange.value(1),
                Green.value(1),
                Purple.value(1),
                White.value(1),
                MonoPoly.value(1),
                Variation.value(0),
                Category.value(0),
                Reserved3.value(0)
        ));
    }

    public FieldValues values() {
        return FIELDS.values(
                Reserved.value(Protocol.Data8.asSubfield(0,0,0,0,0,0,0)),
                Reserved2.value(0),
                Voices.value(voices.get()),
                Height.value(height.get()),
                Unk2.value(2),
                Red.value(red.get()),
                Blue.value(blue.get()),
                Yellow.value(yellow.get()),
                Orange.value(orange.get()),
                Green.value(green.get()),
                Purple.value(purple.get()),
                White.value(white.get()),
                MonoPoly.value(monoPoly.get()),
                Variation.value(variation.get()),
                Category.value(category.get()),
                Reserved3.value(0)
        );
    }
}
