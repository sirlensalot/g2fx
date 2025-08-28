package org.g2fx.g2lib.state;

import org.g2fx.g2gui.controls.VoiceMode;
import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class PatchSettings {

    private final FieldValues fvs;

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


    public PatchSettings(FieldValues fvs) {
        this.fvs = fvs;
        voices = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Voices);
        height = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Height);
        red = LibProperty.booleanFieldProperty(fvs,Protocol.PatchDescription.Red);
        blue = LibProperty.booleanFieldProperty(fvs,Protocol.PatchDescription.Blue);
        yellow = LibProperty.booleanFieldProperty(fvs,Protocol.PatchDescription.Yellow);
        orange = LibProperty.booleanFieldProperty(fvs,Protocol.PatchDescription.Orange);
        green = LibProperty.booleanFieldProperty(fvs,Protocol.PatchDescription.Green);
        purple = LibProperty.booleanFieldProperty(fvs,Protocol.PatchDescription.Purple);
        white = LibProperty.booleanFieldProperty(fvs,Protocol.PatchDescription.White);
        monoPoly = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.MonoPoly);
        variation = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Variation);
        category = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Category);

        voiceMode = new LibProperty<>(new LibProperty.LibPropertyGetterSetter<>() {
            @Override
            public VoiceMode get() {
                return VoiceMode.fromMonoPolyAndVoices(monoPoly().get(), voices().get());
            }

            @Override
            public void set(VoiceMode newValue) {
                monoPoly.set(newValue.getMonoPoly());
                voices.set(newValue.getVoices());
            }
        });

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
}
