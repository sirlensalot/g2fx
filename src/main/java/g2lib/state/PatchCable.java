package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

public class PatchCable {
    private final FieldValues fvs;

    public PatchCable(FieldValues fvs) {
        this.fvs = fvs;
    }
    public int getColor() {
        return Protocol.Cable.Color.intValueRequired(fvs);
    }

    public void setColor(int value) {
        fvs.update(Protocol.Cable.Color.value(value));
    }

    public int getSrcModule() {
        return Protocol.Cable.SrcModule.intValueRequired(fvs);
    }

    public void setSrcModule(int value) {
        fvs.update(Protocol.Cable.SrcModule.value(value));
    }

    public int getSrcConn() {
        return Protocol.Cable.SrcConn.intValueRequired(fvs);
    }

    public void setSrcConn(int value) {
        fvs.update(Protocol.Cable.SrcConn.value(value));
    }

    public boolean getDirection() {
        return Protocol.Cable.Direction.booleanIntValue(fvs);
    }

    public void setDirection(boolean value) {
        fvs.update(Protocol.Cable.Direction.value(value));
    }

    public int getDestModule() {
        return Protocol.Cable.DestModule.intValueRequired(fvs);
    }

    public void setDestModule(int value) {
        fvs.update(Protocol.Cable.DestModule.value(value));
    }

    public int getDestConn() {
        return Protocol.Cable.DestConn.intValueRequired(fvs);
    }

    public void setDestConn(int value) {
        fvs.update(Protocol.Cable.DestConn.value(value));
    }

}
