package g2lib.model;

public record Cable
        (ParamModule fromMod,
         Port fromPort,
         ParamModule toMod,
         Port toPort,
         LinkType linkType,
         ConnColor color
         ) {

    public enum LinkType {
        Normal
    }

}

