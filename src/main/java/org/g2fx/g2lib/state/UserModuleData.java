package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.List;

/**
 * All data in here can only be set/changed by the editor, thus these
 * represent one-way dataflow (ui->backend) after patch/perf load.
 */
public class UserModuleData {
    private final FieldValues fvs;
    private final ModuleType type;
    private final int index;
    private final LibProperty<Integer> horiz;
    private final LibProperty<Integer> vert;
    private final LibProperty<Integer> color;
    private final LibProperty<Integer> uprate;
    private final LibProperty<Boolean> leds;

    /**
     * Modes are module params that cannot be assigned to knobs,
     * thus editor-only.
     */
    private final List<LibProperty<Integer>> modes;


    public UserModuleData(FieldValues fvs) {
        this.fvs = fvs;
        this.type = ModuleType.getById(Protocol.UserModule.Id.intValue(fvs));
        this.index = Protocol.UserModule.Index.intValue(fvs);
        horiz = LibProperty.intFieldProperty(fvs,Protocol.UserModule.Horiz);
        vert = LibProperty.intFieldProperty(fvs,Protocol.UserModule.Vert);
        color = LibProperty.intFieldProperty(fvs,Protocol.UserModule.Color);
        uprate = LibProperty.intFieldProperty(fvs,Protocol.UserModule.Uprate);
        leds = LibProperty.booleanFieldProperty(fvs,Protocol.UserModule.Leds);
        modes = Protocol.UserModule.Modes.subfieldsValue(fvs).stream().map(mfs ->
                LibProperty.intFieldProperty(mfs, Protocol.ModuleModes.Data)).toList();

    }

    public ModuleType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public LibProperty<Integer> horiz() { return horiz; }
    public LibProperty<Integer> vert() { return vert; }
    public LibProperty<Integer> color() { return color; }
    public LibProperty<Integer> uprate() { return uprate; }
    public LibProperty<Boolean> leds() { return leds; }

    public LibProperty<Integer> mode(int index) {
        return modes.get(index);
    }

    public List<LibProperty<Integer>> getModes() {
        return modes;
    }


}
