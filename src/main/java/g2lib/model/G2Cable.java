package g2lib.model;

public record G2Cable
        (G2Module fromMod,
         Connector fromPort,
         G2Module toMod,
         Connector toPort,
         int direction,
         int color
         ) {

}

