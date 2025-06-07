package g2lib.state;

import g2lib.util.SafeLookup;

public enum AreaId {
    Fx,
    Voice,
    Settings;
    public static final SafeLookup<Integer,AreaId> LOOKUP = SafeLookup.makeEnumOrdLookup(values());
}
