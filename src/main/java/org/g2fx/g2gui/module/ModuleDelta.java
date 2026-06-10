package org.g2fx.g2gui.module;

import org.g2fx.g2lib.model.ModuleType;
import org.g2fx.g2lib.model.NamedParam;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;
import org.g2fx.g2lib.state.AreaId;
import org.g2fx.g2lib.state.Coords;
import org.g2fx.g2lib.state.ParamValues;
import org.g2fx.g2lib.state.PatchModule;
import org.g2fx.g2lib.usb.UsbSlotSender;

import java.util.List;

/**
 * Capture a UI module add/paste or delete/cut.
 * @param records added or deleted module records
 * @param add true for add, false for delete.
 */
public record ModuleDelta(List<UserModuleRecord> records, List<FieldValues> cables, boolean add) {

    public record UserModuleRecord (
            String name,
            FieldValues moduleData,
            AreaId area,
            List<FieldValues> paramValues,
            FieldValues moduleLabels) {

        public UserModuleRecord(PatchModule pm) {
            this(pm.name().get(),
                    pm.getUserModuleData().getValues(),
                    pm.getArea(),
                    pm.getValues() == null ? null : pm.getValues().getValues(),
                    pm.getModuleLabelsValues()
                    );
        }

        public PatchModule mkPatchModule(UsbSlotSender sender) {
            PatchModule pm = new PatchModule(moduleData, sender, area);
            pm.setParamValues(paramValues);
            if (moduleLabels != null) {
                pm.setUserLabels(Protocol.ModuleLabel.Labels.subfieldsValue(moduleLabels));
            }
            return pm;
        }

        public UserModuleRecord duplicate(int index, Coords coords) {
            FieldValues md = moduleData.copy()
                    .update(Protocol.UserModule.Index.value(index))
                    .update(Protocol.UserModule.Column.value(coords.column()))
                    .update(Protocol.UserModule.Row.value(coords.row()));
            return new UserModuleRecord(name,md,area,paramValues,moduleLabels);
        }

        public Coords getCoords() {
            return new Coords(Protocol.UserModule.Column.intValue(moduleData),
                    Protocol.UserModule.Row.intValue(moduleData));
        }
        public int getIndex() {
            return Protocol.UserModule.Index.intValue(moduleData);
        }

    }

    public ModuleDelta() {
        this(List.of(),List.of(),false);
    }

    public boolean isEmpty() {
        return records.isEmpty() && cables().isEmpty();
    }

    public static ModuleDelta addNewModule(AreaId area, ModuleType type, int index, String name, int color, Coords coords) {
        return new ModuleDelta(List.of(new UserModuleRecord(
                name,
                Protocol.UserModule.FIELDS.init().addAll(
                        Protocol.UserModule.Id.value(type.ix),
                        Protocol.UserModule.Index.value(index),
                        Protocol.UserModule.Column.value(coords.column()),
                        Protocol.UserModule.Row.value(coords.row()),
                        Protocol.UserModule.Color.value(color),
                        Protocol.UserModule.Uprate.value(0),
                        Protocol.UserModule.Leds.value(type.isLed),
                        Protocol.UserModule.Reserved.value(0),
                        Protocol.UserModule.ModeCount.value(type.modes.size()),
                        Protocol.UserModule.Modes.value(type.modes.stream().map(np ->
                                Protocol.ModuleModes.FIELDS.init().add(Protocol.ModuleModes.Data.value(np.param().def)
                                )).toList())),
                area,
                ParamValues.mkDefaultVarParams(type.getParams().stream().map(NamedParam::def).toList()),
                null
        )),List.of(),true);
    }

    public ModuleDelta invert() {
        return new ModuleDelta(records,cables,!add);
    }



}
