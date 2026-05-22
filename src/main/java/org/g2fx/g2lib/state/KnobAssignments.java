package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class KnobAssignments {
    private final List<FieldValues> kfvs;
    private final List<LibProperty<KnobAssignment>> assignments = new ArrayList<>();
    private final FieldValues fvs;

    public KnobAssignments(FieldValues fvs, Slot slot) {
        this.fvs = fvs;
        this.kfvs =
                Protocol.KnobAssignments.Knobs.subfieldsValue(fvs);
        for (FieldValues ka : kfvs) {
            KnobAssignment k;
            if (Protocol.KnobAssignment.Assigned.intValue(ka) == 1) {
                FieldValues kp = Protocol.KnobAssignment.Params.subfieldsValue(ka).getFirst();
                k = new KnobAssignment(new KnobAssignment.Location(slot,
                        AreaId.LOOKUP.get(Protocol.KnobParams.Location.intValue(kp)),
                        Protocol.KnobParams.Index.intValue(kp),
                        Protocol.KnobParams.Param.intValue(kp)),
                        Protocol.KnobParams.IsLed.booleanIntValue(kp));
            } else {
                k = KnobAssignment.unassigned();
            }
            assignments.add(new LibProperty<>(k));
        }
    }

    public KnobAssignments(Slot slot) {
        this(Protocol.KnobAssignments.FIELDS.values(
                Protocol.KnobAssignments.KnobCount.value(0x78),
                Protocol.KnobAssignments.Knobs.value(
                        IntStream.range(0,0x78).mapToObj(i ->
                                Protocol.KnobAssignment.FIELDS.values(
                                        Protocol.KnobAssignment.Assigned.value(0),
                                        Protocol.KnobAssignment.Params.value(List.of())
                                )).toList())
        ),slot);
    }

    public List<LibProperty<KnobAssignment>> assignments() {
        return assignments;
    }

    //test only
    public List<FieldValues> getActiveAssignments() {
        return kfvs.stream().filter(fv ->
                        Protocol.KnobAssignment.Assigned.intValue(fv) == 1)
                .map(ka -> Protocol.KnobAssignment.Params.subfieldsValue(ka).getFirst()).toList();
    }

    public FieldValues values() {
        return fvs;
    }
}
