package g2lib.model;

import java.util.List;

public record Visual(VisualType type, List<String> names) {
    public enum VisualType {
        Led, // single name
        LedGroup, //multiple names
        Meter // single name
    }
}
