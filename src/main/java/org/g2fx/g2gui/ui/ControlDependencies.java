package org.g2fx.g2gui.ui;

import java.util.List;

public interface ControlDependencies {

    record Dependency(UIElements.DepType type, int index) {}

    public List<Dependency> Dependencies();

}
