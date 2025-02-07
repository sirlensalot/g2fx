package g2lib.state;

import g2lib.protocol.FieldValues;
import g2lib.protocol.Protocol;

import java.util.List;

public class KnobAssignments {
    private final List<FieldValues> assignments;

    public KnobAssignments(FieldValues fvs) {
        this.assignments =
                Protocol.KnobAssignments.Knobs.subfieldsValueRequired(fvs);
    }

    public Boolean getKnobAssignment(AreaId area, int module, int param) {
        for (FieldValues ka : assignments) {
            if (Protocol.KnobAssignment.Assigned.intValueRequired(ka) == 1) {
                List<FieldValues> kps = Protocol.KnobAssignment.Params.subfieldsValueRequired(ka);
                for (FieldValues kp : kps) {
                    if (area.ordinal() == Protocol.KnobParams.Location.intValueRequired(kp) &&
                    module == Protocol.KnobParams.Index.intValueRequired(kp) &&
                    param == Protocol.KnobParams.Param.intValueRequired(kp)) {
                        return Protocol.KnobParams.IsLed.booleanIntValue(kp);
                    }
                }
            }
        }
        return null;
    }

    public List<FieldValues> getActiveAssignments() {
        return assignments.stream().filter(fv ->
                        Protocol.KnobAssignment.Assigned.intValueRequired(fv) == 1)
                .map(ka -> Protocol.KnobAssignment.Params.subfieldsValueRequired(ka).getFirst()).toList();
    }
}
