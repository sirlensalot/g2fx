package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;

public class GlobalKnobAssignments {
    private final FieldValues fvs;
    private final List<FieldValues> gkfvs;
    private final List<LibProperty<KnobAssignment>> assignments = new ArrayList<>();

    // file-perf
    public GlobalKnobAssignments(FieldValues fvs) {
        this.fvs = fvs;
        this.gkfvs =
                Protocol.GlobalKnobAssignments.Knobs.subfieldsValue(fvs);
        for (FieldValues ka : gkfvs) {
            KnobAssignment k;
            if (Protocol.GlobalKnobAssignment.Assigned.intValue(ka) == 1) {
                FieldValues kp = Protocol.GlobalKnobAssignment.Params.subfieldsValue(ka).getFirst();
                k = new KnobAssignment(new KnobAssignment.Location(
                        Slot.fromIndex(Protocol.GlobalKnobParams.Slot.intValue(kp)),
                        AreaId.LOOKUP.get(Protocol.GlobalKnobParams.Location.intValue(kp)),
                        Protocol.GlobalKnobParams.Index.intValue(kp),
                        Protocol.GlobalKnobParams.Param.intValue(kp)),
                        Protocol.GlobalKnobParams.IsLed.booleanIntValue(kp));
            } else {
                k = KnobAssignment.unassigned();
            }
            assignments.add(new LibProperty<>(k));
        }
    }

    public List<LibProperty<KnobAssignment>> assignments() {
        return assignments;
    }

    public Boolean getKnobAssignment(Slot slot, AreaId area, int module, int param) {
        for (FieldValues ka : gkfvs) {
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
        return gkfvs.stream().filter(fv ->
                Protocol.GlobalKnobAssignment.Assigned.intValue(fv) == 1)
                .map(ka -> Protocol.GlobalKnobAssignment.Params.subfieldsValue(ka).getFirst()).toList();
    }

    public FieldValues getFieldValues() {
        return fvs;
    }
}
