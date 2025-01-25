package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

import java.util.List;

public class ControlAssignments {

    private final List<FieldValues> assignments;

    public ControlAssignments(FieldValues fvs) {
        this.assignments =
                Protocol.ControlAssignments.Assignments.subfieldsValueRequired(fvs);
    }

    public Integer getControlAssignment(AreaId area, int module, int param) {
        for (FieldValues kp : assignments) {
            if (area.ordinal() == Protocol.ControlAssignment.Location.intValueRequired(kp) &&
                    module == Protocol.ControlAssignment.Index.intValueRequired(kp) &&
                    param == Protocol.ControlAssignment.Param.intValueRequired(kp)) {
                return Protocol.ControlAssignment.MidiCC.intValueRequired(kp);
            }
        }
        return null;
    }
}
