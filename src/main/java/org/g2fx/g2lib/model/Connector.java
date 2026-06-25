package org.g2fx.g2lib.model;

public record Connector(String name, ConnDir dir, ConnType type, Bandwidth bandwidth) {

    public static Connector out(String name, ConnTypeBandwidth color) {
        return new Connector(name, ConnDir.Out,color.type,color.bandwidth);
    }
    public static Connector in(String name, ConnTypeBandwidth color) {
        return new Connector(name, ConnDir.In,color.type,color.bandwidth);
    }

    public enum ConnType {
        Audio,
        Control,
        Logic
    }

    public enum ConnDir {
        In,
        Out
    }

    public enum Bandwidth {
        Dynamic,
        Static
    }

    public enum ConnTypeBandwidth {
        Audio(ConnType.Audio,Bandwidth.Static),
        Control(ConnType.Control,Bandwidth.Static),
        Logic(ConnType.Logic,Bandwidth.Static),
        ControlDyn(ConnType.Control,Bandwidth.Dynamic),
        LogicDyn(ConnType.Logic,Bandwidth.Dynamic);
        private final ConnType type;
        private final Bandwidth bandwidth;
        ConnTypeBandwidth(ConnType type, Bandwidth bandwidth) {
            this.type = type;
            this.bandwidth = bandwidth;
        }
    }

}
