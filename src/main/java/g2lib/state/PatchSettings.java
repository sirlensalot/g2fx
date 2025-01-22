package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

public class PatchSettings {

    private final FieldValues fvs;

    public PatchSettings(FieldValues fvs) {
        this.fvs = fvs;
    }

    public int getVoices() {
        return Protocol.PatchDescription.Voices.intValueRequired(fvs);
    }

    public void setVoices(int value) {
        fvs.update(Protocol.PatchDescription.Voices.value(value));
    }

    public int getHeight() {
        return Protocol.PatchDescription.Height.intValueRequired(fvs);
    }

    public void setHeight(int value) {
        fvs.update(Protocol.PatchDescription.Height.value(value));
    }

    public boolean getRed() {
        return Protocol.PatchDescription.Red.booleanIntValue(fvs);
    }

    public void setRed(boolean value) {
        fvs.update(Protocol.PatchDescription.Red.value(value));
    }

    public boolean getBlue() {
        return Protocol.PatchDescription.Blue.booleanIntValue(fvs);
    }

    public void setBlue(boolean value) {
        fvs.update(Protocol.PatchDescription.Blue.value(value));
    }

    public boolean getYellow() {
        return Protocol.PatchDescription.Yellow.booleanIntValue(fvs);
    }

    public void setYellow(boolean value) {
        fvs.update(Protocol.PatchDescription.Yellow.value(value));
    }

    public boolean getOrange() {
        return Protocol.PatchDescription.Orange.booleanIntValue(fvs);
    }

    public void setOrange(boolean value) {
        fvs.update(Protocol.PatchDescription.Orange.value(value));
    }

    public boolean getGreen() {
        return Protocol.PatchDescription.Green.booleanIntValue(fvs);
    }

    public void setGreen(boolean value) {
        fvs.update(Protocol.PatchDescription.Green.value(value));
    }

    public boolean getPurple() {
        return Protocol.PatchDescription.Purple.booleanIntValue(fvs);
    }

    public void setPurple(boolean value) {
        fvs.update(Protocol.PatchDescription.Purple.value(value));
    }

    public boolean getWhite() {
        return Protocol.PatchDescription.White.booleanIntValue(fvs);
    }

    public void setWhite(boolean value) {
        fvs.update(Protocol.PatchDescription.White.value(value));
    }

    public int getMonoPoly() {
        return Protocol.PatchDescription.MonoPoly.intValueRequired(fvs);
    }

    public void setMonoPoly(int value) {
        fvs.update(Protocol.PatchDescription.MonoPoly.value(value));
    }

    public int getVariation() {
        return Protocol.PatchDescription.Variation.intValueRequired(fvs);
    }

    public void setVariation(int value) {
        fvs.update(Protocol.PatchDescription.Variation.value(value));
    }

    public int getCategory() {
        return Protocol.PatchDescription.Category.intValueRequired(fvs);
    }

    public void setCategory(int value) {
        fvs.update(Protocol.PatchDescription.Category.value(value));
    }

}
