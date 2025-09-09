package org.g2fx.g2lib.state;

public record KnobAssignment(Location loc,Boolean led) {
    public record Location(Slot slot,AreaId area,int module,int param) {}
    public boolean assigned() { return loc != null; }
    public static KnobAssignment unassigned() {
        return new KnobAssignment(null,false);
    }
}
