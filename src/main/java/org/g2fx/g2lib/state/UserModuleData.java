package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.usb.UsbSlotSender;
import org.g2fx.g2lib.util.Util;

import java.util.List;
import java.util.logging.Logger;

import static org.g2fx.g2lib.util.Util.mapWithIndex;

/**
 * All data in here can only be set/changed by the editor, thus these
 * represent one-way dataflow (ui->backend) after patch/perf load.
 */
public class UserModuleData {

    private final Logger log;

    private final FieldValues fvs;
    private final ModuleType type;
    private final int index;
    private final LibProperty<Integer> column;
    private final LibProperty<Integer> row;
    private final LibProperty<Integer> color;
    private final LibProperty<Integer> uprate;
    private final LibProperty<Boolean> leds;

    public record Coords(int column, int row) {}
    private final LibProperty<Coords> coords;

    /**
     * Modes are module params that cannot be assigned to knobs,
     * thus editor-only.
     */
    private final List<LibProperty<Integer>> modes;


    public UserModuleData(FieldValues fvs, UsbSlotSender sender, AreaId area) {
        this.fvs = fvs;
        this.type = ModuleType.getById(Protocol.UserModule.Id.intValue(fvs));
        this.index = Protocol.UserModule.Index.intValue(fvs);
        log = Util.getLogger(getClass(),type,index);
        column = LibProperty.intFieldProperty(fvs,Protocol.UserModule.Column);
        row = LibProperty.intFieldProperty(fvs,Protocol.UserModule.Row);
        color = LibProperty.intFieldProperty(fvs,Protocol.UserModule.Color);
        uprate = LibProperty.intFieldProperty(fvs,Protocol.UserModule.Uprate);
        leds = LibProperty.booleanFieldProperty(fvs,Protocol.UserModule.Leds);
        modes = mapWithIndex(Protocol.UserModule.Modes.subfieldsValue(fvs),(mfs,param) -> {
            LibProperty<Integer> p = LibProperty.intFieldProperty(mfs, Protocol.ModuleModes.Data);
            p.addListener((o,n)-> {
                log.info(() -> "setMode: " + index + " -> " + n);
                sender.sendSlotRequest("setMode",
                        0x2b, // S_SET_MODE
                        area.ordinal(),
                        index,
                        param,
                        n
                );
            });
            return p;
        });
        coords = new LibProperty<>(new LibProperty.LibPropertyGetterSetter<>() {
            @Override
            public Coords get() {
                return new Coords(column.get(), row.get());
            }

            @Override
            public void set(Coords newValue) {
                column.set(newValue.column());
                row.set(newValue.row());
            }
        });
    }

    public ModuleType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public LibProperty<Integer> column() { return column; }
    public LibProperty<Integer> row() { return row; }
    public LibProperty<Integer> color() { return color; }
    public LibProperty<Integer> uprate() { return uprate; }
    public LibProperty<Boolean> leds() { return leds; }

    public LibProperty<Integer> mode(int index) {
        return modes.get(index);
    }

    public List<LibProperty<Integer>> getModes() {
        return modes;
    }

    public LibProperty<Coords> coords() { return coords; }


}
