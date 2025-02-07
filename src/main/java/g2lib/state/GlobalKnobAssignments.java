package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

import java.util.List;

public class GlobalKnobAssignments {
    private final List<FieldValues> assignments;

    public GlobalKnobAssignments(FieldValues fvs) {
        this.assignments =
                Protocol.GlobalKnobAssignments.Knobs.subfieldsValueRequired(fvs);
    }

    public Boolean getKnobAssignment(Slot slot, AreaId area, int module, int param) {
        for (FieldValues ka : assignments) {
            if (Protocol.GlobalKnobAssignment.Assigned.intValueRequired(ka) == 1) {
                List<FieldValues> kps = Protocol.GlobalKnobAssignment.Params.subfieldsValueRequired(ka);
                for (FieldValues kp : kps) {
                    if (area.ordinal() == Protocol.GlobalKnobParams.Location.intValueRequired(kp) &&
                            slot.ordinal() == Protocol.GlobalKnobParams.Slot.intValueRequired(kp) &&
                    module == Protocol.GlobalKnobParams.Index.intValueRequired(kp) &&
                    param == Protocol.GlobalKnobParams.Param.intValueRequired(kp)) {
                        return Protocol.GlobalKnobParams.IsLed.booleanIntValue(kp);
                    }
                }
            }
        }
        return null;
    }

    public List<FieldValues> getActiveAssignments() {
        return assignments.stream().filter(fv ->
                Protocol.GlobalKnobAssignment.Assigned.intValueRequired(fv) == 1)
                .map(ka -> Protocol.GlobalKnobAssignment.Params.subfieldsValueRequired(ka).getFirst()).toList();
    }
}
