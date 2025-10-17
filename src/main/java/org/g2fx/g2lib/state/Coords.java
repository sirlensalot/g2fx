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

    public Coords incRow(int inc) {
        return new Coords(column,row+inc);
    }

    public Coords setRow(int r) {
        return new Coords(column,r);
    }
}
