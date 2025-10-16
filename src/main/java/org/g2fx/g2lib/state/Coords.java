package org.g2fx.g2lib.state;

public record Coords(int column, int row) implements Comparable<Coords> {
    /**
     * Sorts by column then row.
     */
    @Override
    public int compareTo(Coords o) {
        int c;
        return o == null ? 1 :
                (c= Integer.compare(column,o.column)) != 0 ? c :
                        Integer.compare(row,o.row);
    }
}
