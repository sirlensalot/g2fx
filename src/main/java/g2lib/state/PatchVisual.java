package g2lib.state;

import g2lib.model.Visual;

public class PatchVisual {

    private final AreaId area;
    private final PatchModule module;
    private final Visual visual;
    private int value;

    public PatchVisual(AreaId area, PatchModule module, Visual visual) {
        this.area = area;
        this.module = module;
        this.visual = visual;
    }

    public boolean update(int value) {
        if (this.value != value) {
            this.value = value;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        int sz = visual.names().size();
        String ns = switch (sz) {
            case 1 -> visual.names().getFirst();
            case 2 , 3 , 4 -> visual.names().toString();
            default -> visual.groupType() + "[" + visual.names().size() + "]";
        };
        return area + "." + module.getName() + "[" + module.getIndex() + "]." + ns + "=" + value;
    }
}
