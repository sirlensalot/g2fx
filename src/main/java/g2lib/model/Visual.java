package g2lib.model;

public record Visual(VisualType type,String name) {
    public enum VisualType {
        Led,
        Meter
    }
}
