package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

public class PatchSettings {

    private final FieldValues fvs;

    private final LibProperty<Integer> voices;
    private final LibProperty<Integer> height;
    private final LibProperty<Integer> red;
    private final LibProperty<Integer> blue;
    private final LibProperty<Integer> yellow;
    private final LibProperty<Integer> orange;
    private final LibProperty<Integer> green;
    private final LibProperty<Integer> purple;
    private final LibProperty<Integer> white;
    private final LibProperty<Integer> monoPoly;
    private final LibProperty<Integer> variation;
    private final LibProperty<Integer> category;


    public PatchSettings(FieldValues fvs) {
        this.fvs = fvs;
        voices = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Voices);
        height = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Height);
        red = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Red);
        blue = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Blue);
        yellow = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Yellow);
        orange = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Orange);
        green = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Green);
        purple = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Purple);
        white = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.White);
        monoPoly = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.MonoPoly);
        variation = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Variation);
        category = LibProperty.intFieldProperty(fvs,Protocol.PatchDescription.Category);

    }

    public LibProperty<Integer> voices() { return voices; }
    public LibProperty<Integer> height() { return height; }
    public LibProperty<Integer> red() { return red; }
    public LibProperty<Integer> blue() { return blue; }
    public LibProperty<Integer> yellow() { return yellow; }
    public LibProperty<Integer> orange() { return orange; }
    public LibProperty<Integer> green() { return green; }
    public LibProperty<Integer> purple() { return purple; }
    public LibProperty<Integer> white() { return white; }
    public LibProperty<Integer> monoPoly() { return monoPoly; }
    public LibProperty<Integer> variation() { return variation; }
    public LibProperty<Integer> category() { return category; }

}
