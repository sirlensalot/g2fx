package g2lib.model;

public class Visual {
    protected final VisualType type;
    protected final String name;
    protected int value;

    protected Visual(VisualType type, String name) {
        this.type = type;
        this.name = name;
    }

    public VisualType getType() {
        return type;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public enum VisualType {
        Led,
        Meter;
    }
}
