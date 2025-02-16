package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

import java.util.List;

public class GlobalKnobAssignments {
    private final List<FieldValues> assignments;

    public GlobalKnobAssignments(FieldValues fvs) {
        this.assignments =
                Protocol.GlobalKnobAssignments.Knobs.subfieldsValue(fvs);
    }

    public Boolean getKnobAssignment(Slot slot, AreaId area, int module, int param) {
        for (FieldValues ka : assignments) {
            if (Protocol.GlobalKnobAssignment.Assigned.intValue(ka) == 1) {
                List<FieldValues> kps = Protocol.GlobalKnobAssignment.Params.subfieldsValue(ka);
                for (FieldValues kp : kps) {
                    if (area.ordinal() == Protocol.GlobalKnobParams.Location.intValue(kp) &&
                            slot.ordinal() == Protocol.GlobalKnobParams.Slot.intValue(kp) &&
                    module == Protocol.GlobalKnobParams.Index.intValue(kp) &&
                    param == Protocol.GlobalKnobParams.Param.intValue(kp)) {
                        return Protocol.GlobalKnobParams.IsLed.booleanIntValue(kp);
                    }
                }
            }
        }
        return null;
    }

    public List<FieldValues> getActiveAssignments() {
        return assignments.stream().filter(fv ->
                Protocol.GlobalKnobAssignment.Assigned.intValue(fv) == 1)
                .map(ka -> Protocol.GlobalKnobAssignment.Params.subfieldsValue(ka).getFirst()).toList();
    }
}
