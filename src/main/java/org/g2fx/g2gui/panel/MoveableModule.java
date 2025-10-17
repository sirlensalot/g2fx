package org.g2fx.g2gui.panel;

import javafx.beans.property.Property;
import org.g2fx.g2lib.state.Coords;

import java.util.*;
import java.util.stream.Collectors;

public interface MoveableModule {

    boolean isSelected();

    Property<Coords> coords();

    int getHeight();

    default int getRow() { return coords().getValue().row(); }
    default int getColumn() { return coords().getValue().column(); }
    default int getBottomEdge() { return getRow() + getHeight(); }
    default void setRow(int row) { coords().setValue(coords().getValue().setRow(row)); }
    default void incRow(int inc) { coords().setValue(coords().getValue().incRow(inc));}


    /**
     * Resolve module bounds collisions, where selected modules represent "moved" modules/user intent,
     * and unselected those to be moved. Moves are handled independently by column.
     */

    static void resolveCollisions(Collection<? extends MoveableModule> allModules) {
        // sort, group by column
        Map<Integer, List<MoveableModule>> byColumn = allModules.stream()
                .sorted(Comparator.comparing(MoveableModule::getColumn))
                .collect(Collectors.groupingBy(MoveableModule::getColumn,
                        TreeMap::new,Collectors.toList()));
        byColumn.values().forEach(MoveableModule::resolveCollisionsColumn);
    }

    /**
     * Moves are handled with each selection top-to-bottom.
     * For each selected, you have:
     *  - selected module SEL
     *  - remaining selecteds below SELS
     *  - First colliding nonselected TOPCOLL
     *  - List with colliding and every nonselected below COLLS
     *  1. If TOPCOLL above SEL:
     *    - remove TOPCALL from COLLS
     *    - move SEL down OFFSET rows to be below TOPCOLL
     *    - move all SELS down by OFFSET.
     *  2. Initialize loop var TOP with SEL.
     *  3. For each COLL in COLLS:
     *    a. If COLL does not collide with TOP, break.
     *    b. Move COLL below TOP.
     *    c. Set TOP to COLL.
     *  4. Loop to next selected.
     */
    static void resolveCollisionsColumn(List<? extends MoveableModule> modules) {
        //sort by row, group by selected
        Map<Boolean, List<MoveableModule>> bySelected = modules.stream()
                .sorted(Comparator.comparing(MoveableModule::getRow))
                .collect(Collectors.groupingBy(MoveableModule::isSelected));
        if (bySelected.size() != 2) { return; }
        List<MoveableModule> selecteds = new ArrayList<>(bySelected.get(true));
        List<MoveableModule> unselecteds = new ArrayList<>(bySelected.get(false));
        while (!selecteds.isEmpty()) {
            // get/remove top selected
            MoveableModule sel = selecteds.removeFirst();
            // remove unselecteds whose bottom edge is above or equal to sel row
            unselecteds.removeIf(m -> m.getBottomEdge() <= sel.getRow());
            if (unselecteds.isEmpty()) { return; }
            MoveableModule topUnsel = unselecteds.getFirst();
            if (topUnsel.getRow() < sel.getRow()) {
                // keep top, move all sels immediately below
                unselecteds.removeFirst();
                int selInc = topUnsel.getBottomEdge() - sel.getRow();
                sel.incRow(selInc);
                selecteds.forEach(s -> s.incRow(selInc));
            }
            //iteratively move down unselecteds as needed
            MoveableModule top = sel;
            for (MoveableModule m : unselecteds) {
                if (top.getBottomEdge() <= m.getRow()) { break; }
                m.setRow(top.getBottomEdge());
                top = m;
            }
        }
    }

}
