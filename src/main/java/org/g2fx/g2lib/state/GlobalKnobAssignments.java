package org.g2fx.g2lib.state;

import org.g2fx.g2lib.model.LibProperty;
import org.g2fx.g2lib.protocol.FieldValues;
import org.g2fx.g2lib.protocol.Protocol;

import java.util.List;
import java.util.stream.IntStream;

public class GlobalKnobAssignments {

    private final List<LibProperty<KnobAssignment>> assignments =
            IntStream.range(0,0x78).mapToObj(_ -> new LibProperty<>(KnobAssignment.unassigned())).toList();

    private static KnobAssignment readKnobAssignment(FieldValues ka) {
        FieldValues kp = Protocol.GlobalKnobAssignment.Params.subfieldsValue(ka).getFirst();
        return new KnobAssignment(new KnobAssignment.Location(
                Slot.fromIndex(Protocol.GlobalKnobParams.Slot.intValue(kp)),
                AreaId.LOOKUP.get(Protocol.GlobalKnobParams.Location.intValue(kp)),
                Protocol.GlobalKnobParams.Index.intValue(kp),
                Protocol.GlobalKnobParams.Param.intValue(kp)),
                Protocol.GlobalKnobParams.IsLed.booleanIntValue(kp));
    }

    public static FieldValues writeKnobAssignment(KnobAssignment k) {
        if (k.assigned()) {
            return Protocol.GlobalKnobAssignment.FIELDS.values(
                    Protocol.GlobalKnobAssignment.Assigned.value(1),
                    Protocol.GlobalKnobAssignment.Params.value(List.of(
                            Protocol.GlobalKnobParams.FIELDS.values(
                                    Protocol.GlobalKnobParams.Location.value(k.loc().area().ordinal()),
                                    Protocol.GlobalKnobParams.Index.value(k.loc().module()),
                                    Protocol.GlobalKnobParams.IsLed.value(k.led()),
                                    Protocol.GlobalKnobParams.Param.value(k.loc().param()),
                                    Protocol.GlobalKnobParams.Slot.value(k.loc().slot().ordinal())
                            )
                    )));
        } else {
            return Protocol.GlobalKnobAssignment.FIELDS.values(
                    Protocol.GlobalKnobAssignment.Assigned.value(0),
                    Protocol.GlobalKnobAssignment.Params.value(List.of()));
        }
    }


    public void update(FieldValues fvs) {
        int i = 0;
        for (FieldValues ka : Protocol.GlobalKnobAssignments.Knobs.subfieldsValue(fvs)) {
            assignments.get(i++).set(Protocol.GlobalKnobAssignment.Assigned.intValue(ka) == 1 ?
                    readKnobAssignment(ka) : KnobAssignment.unassigned());
        }
    }

    public List<LibProperty<KnobAssignment>> assignments() {
        return assignments;
    }

    public FieldValues getFieldValues() {
        return Protocol.GlobalKnobAssignments.FIELDS.values(
                Protocol.GlobalKnobAssignments.KnobCount.value(0x78),
                Protocol.GlobalKnobAssignments.Knobs.value(assignments.stream().map(k -> writeKnobAssignment(k.get())).toList())
        );
    }
}
