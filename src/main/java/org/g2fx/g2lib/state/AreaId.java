package org.g2fx.g2lib.state;

import org.g2fx.g2lib.util.SafeLookup;

public enum AreaId {
    Fx,
    Voice,
    Settings;
    public static final SafeLookup<Integer,AreaId> LOOKUP = SafeLookup.makeEnumOrdLookup(values());

    public static final AreaId[] USER_AREAS = new AreaId[] { Fx, Voice };

}
