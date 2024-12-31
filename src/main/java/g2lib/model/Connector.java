package g2lib.model;

public record Connector(String name, PortType type, ConnColor color, int horiz, int vert) {

    public static Connector out(String name, ConnColor color, int horiz, int vert) {
        return new Connector(name,PortType.Out,color,horiz,vert);
    }
    public static Connector in(String name, ConnColor color, int horiz, int vert) {
        return new Connector(name,PortType.In,color,horiz,vert);
    }

    public enum PortType {
        In,
        Out
    }
}
