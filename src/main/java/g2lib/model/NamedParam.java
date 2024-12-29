package g2lib.model;

import java.util.List;

public record NamedParam(ModParam param, String name, List<String> labels) {

    public NamedParam label(String... labels) {
        return new NamedParam(this.param, this.name, List.of(labels));
    }

}
