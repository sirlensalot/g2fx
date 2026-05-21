package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.List;

public class ControlAssignments {

    private final List<FieldValues> assignments;
    private final FieldValues fvs;

    public ControlAssignments(FieldValues fvs) {
        this.fvs = fvs;
        this.assignments =
                Protocol.ControlAssignments.Assignments.subfieldsValue(fvs);
    }

    public ControlAssignments() {
        this(Protocol.ControlAssignments.FIELDS.values(
                Protocol.ControlAssignments.NumControls.value(2),
                Protocol.ControlAssignments.Assignments.value(List.of(
                        Protocol.ControlAssignment.FIELDS.values(
                                Protocol.ControlAssignment.MidiCC.value(7), // midi volume
                                Protocol.ControlAssignment.Location.value(2), // Settings
                                Protocol.ControlAssignment.Index.value(2), // Gain
                                Protocol.ControlAssignment.Param.value(0)), // Gain volume
                        Protocol.ControlAssignment.FIELDS.values(
                                Protocol.ControlAssignment.MidiCC.value(0x11), // 17 ...
                                Protocol.ControlAssignment.Location.value(2), // Settings
                                Protocol.ControlAssignment.Index.value(7), // Misc
                                Protocol.ControlAssignment.Param.value(0)) // Oct shift
                        ))
                ));
    }

    public Integer getControlAssignment(AreaId area, int module, int param) {
        for (FieldValues kp : assignments) {
            if (area.ordinal() == Protocol.ControlAssignment.Location.intValue(kp) &&
                    module == Protocol.ControlAssignment.Index.intValue(kp) &&
                    param == Protocol.ControlAssignment.Param.intValue(kp)) {
                return Protocol.ControlAssignment.MidiCC.intValue(kp);
            }
        }
        return null;
    }

    public FieldValues values() {
        return fvs;
    }
}
