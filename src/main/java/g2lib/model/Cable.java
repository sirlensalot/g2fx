package g2lib.model;

public record Cable
        (G2Module fromMod,
         Port fromPort,
         G2Module toMod,
         Port toPort,
         LinkType linkType,
         ConnColor color
         ) {

    public enum LinkType {
        Normal
    }

}

