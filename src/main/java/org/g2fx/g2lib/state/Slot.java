package org.g2fx.g2lib.state;

import org.g2fx.g2lib.util.SafeLookup;

public enum Slot {
    A, B, C, D;
    private static final SafeLookup<Integer, Slot> LOOKUP = SafeLookup.makeEnumOrdLookup(Slot.values());
    private static final SafeLookup<String, Slot> ALPHA = SafeLookup.makeEnumNameLookup(Slot.values());

    public static Slot fromIndex(int i) {
        return LOOKUP.get(i);
    }

    public static Slot fromAlpha(String n) {
        return ALPHA.get(n);
    }

    public boolean testSlotId(int slot) {
        return slot == ordinal() || slot == ordinal() + 8;
    }
}
