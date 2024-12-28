package g2lib.model;

public record Port(String name, PortType type, ConnColor color, int horiz, int vert) {

    public static Port out(String name, ConnColor color, int horiz, int vert) {
        return new Port(name,PortType.Out,color,horiz,vert);
    }
    public static Port in(String name, ConnColor color, int horiz, int vert) {
        return new Port(name,PortType.In,color,horiz,vert);
    }

    public enum PortType {
        In,
        Out
    }
}
