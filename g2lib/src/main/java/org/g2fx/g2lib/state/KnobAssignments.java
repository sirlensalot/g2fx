package org.g2fx.g2lib.state;

import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.List;

public class KnobAssignments {
    private final List<FieldValues> assignments;

    public KnobAssignments(FieldValues fvs) {
        this.assignments =
                Protocol.KnobAssignments.Knobs.subfieldsValue(fvs);
    }

    public Boolean getKnobAssignment(AreaId area, int module, int param) {
        for (FieldValues ka : assignments) {
            if (Protocol.KnobAssignment.Assigned.intValue(ka) == 1) {
                List<FieldValues> kps = Protocol.KnobAssignment.Params.subfieldsValue(ka);
                for (FieldValues kp : kps) {
                    if (area.ordinal() == Protocol.KnobParams.Location.intValue(kp) &&
                    module == Protocol.KnobParams.Index.intValue(kp) &&
                    param == Protocol.KnobParams.Param.intValue(kp)) {
                        return Protocol.KnobParams.IsLed.booleanIntValue(kp);
                    }
                }
            }
        }
        return null;
    }

    public List<FieldValues> getActiveAssignments() {
        return assignments.stream().filter(fv ->
                        Protocol.KnobAssignment.Assigned.intValue(fv) == 1)
                .map(ka -> Protocol.KnobAssignment.Params.subfieldsValue(ka).getFirst()).toList();
    }
}
