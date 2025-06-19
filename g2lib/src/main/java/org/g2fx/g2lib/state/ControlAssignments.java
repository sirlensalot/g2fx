package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.List;

public class ControlAssignments {

    private final List<FieldValues> assignments;

    public ControlAssignments(FieldValues fvs) {
        this.assignments =
                Protocol.ControlAssignments.Assignments.subfieldsValue(fvs);
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
}
