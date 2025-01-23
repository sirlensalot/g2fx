package g2lib.state;

import g2lib.model.ModuleType;
import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

public class UserModuleData {
    private final FieldValues fvs;
    private final ModuleType type;

    public UserModuleData(FieldValues fvs) {
        this.fvs = fvs;
        this.type = ModuleType.getById(Protocol.UserModule.Id.intValueRequired(fvs));
    }

    public ModuleType getType() {
        return type;
    }

    public int getHoriz() {
        return Protocol.UserModule.Horiz.intValueRequired(fvs);
    }

    public void setHoriz(int value) {
        fvs.update(Protocol.UserModule.Horiz.value(value));
    }

    public int getVert() {
        return Protocol.UserModule.Vert.intValueRequired(fvs);
    }

    public void setVert(int value) {
        fvs.update(Protocol.UserModule.Vert.value(value));
    }

    public int getColor() {
        return Protocol.UserModule.Color.intValueRequired(fvs);
    }

    public void setColor(int value) {
        fvs.update(Protocol.UserModule.Color.value(value));
    }

    public int getUprate() {
        return Protocol.UserModule.Uprate.intValueRequired(fvs);
    }

    public void setUprate(int value) {
        fvs.update(Protocol.UserModule.Uprate.value(value));
    }

    public boolean getLeds() {
        return Protocol.UserModule.Leds.booleanIntValue(fvs);
    }

    public void setLeds(boolean value) {
        fvs.update(Protocol.UserModule.Leds.value(value));
    }

}
