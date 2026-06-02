package org.g2fx.g2lib.state;

import org.g2fx.g2lib.util.BitBuffer;
import org.g2fx.g2lib.util.SafeLookup;

public enum AreaId {
    Fx("FX"),
    Voice("VA"),
    Settings("Settings");
    public static final SafeLookup<Integer,AreaId> LOOKUP = SafeLookup.makeEnumOrdLookup(values());

    public static final AreaId[] USER_AREAS = new AreaId[] { Fx, Voice };
    private final String shortName;

    AreaId(String shortName) {
        this.shortName = shortName;
    }
    public void writeLocation(BitBuffer bb) throws Exception {
        bb.put(2,ordinal());
    }

    public static AreaId readLocation(BitBuffer bb) {
        return LOOKUP.get(bb.get(2));
    }

    public String shortName() {
        return shortName;
    }
}
