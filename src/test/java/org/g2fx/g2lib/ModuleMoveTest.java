package org.g2fx.g2lib;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.g2fx.g2gui.panel.MoveableModule;
import org.g2fx.g2lib.state.Coords;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModuleMoveTest {


    record MockMoveable(Property<Coords> coords, int getHeight, boolean isSelected) implements MoveableModule {
        MockMoveable(int column, int row, int height, boolean selected) {
            this(new SimpleObjectProperty<>(new Coords(column, row)), height, selected);
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MoveableModule c) {
                return coords.getValue().equals(c.coords().getValue()) && getHeight == c.getHeight() && isSelected == c.isSelected();
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("h=%d,sel=%s,col=%d,row=%d",getHeight,isSelected?"Y":"N",coords.getValue().column(),coords.getValue().row());
        }
    }


    record MoveTest(MockMoveable module,int targetRow) {}
    static MoveTest moveTest(int height, boolean selected, int column, int row, int targetRow) {
        return new MoveTest(new MockMoveable(column,row,height,selected),targetRow);
    }

    List<MockMoveable> start(List<MoveTest> tests) {
        return tests.stream().map(MoveTest::module).toList();
    }

    List<MockMoveable> intended(List<MoveTest> tests) {
        return tests.stream().map(t -> new MockMoveable(t.module.coords.getValue().column(),t.targetRow,t.module.getHeight,t.module.isSelected)).toList();
    }

    @Test void move1 () {
        //trivial
        List<MoveTest> tests = List.of(moveTest(1,true,0,0, 0));
        MoveableModule.resolveCollisions(start(tests));
        assertEquals(intended(tests),start(tests));
    }

    @Test void move2_1 () {
        //2 iso overlap, selected vs non
        List<MoveTest> tests = List.of(
                 moveTest(1,true, 0,0, 0)
                ,moveTest(1,false,0,0, 1)
        );
        MoveableModule.resolveCollisions(start(tests));
        assertEquals(intended(tests),start(tests));
    }
    @Test void move2_2 () {
        //2 diff overlap, selected vs non
        List<MoveTest> tests = List.of(
                 moveTest(2,true, 0,0, 0)
                ,moveTest(1,false,0,0, 2)
        );
        MoveableModule.resolveCollisions(start(tests));
        assertEquals(intended(tests),start(tests));
    }
    @Test void move3 () {
        //3, insert between two
        List<MoveTest> tests = List.of(
                 moveTest(3,true, 0,4, 6)
                ,moveTest(3,false,0,3, 3)
                ,moveTest(3,false,0,6, 9)
        );
        MoveableModule.resolveCollisions(start(tests));
        assertEquals(intended(tests),start(tests));
    }
    @Test void moveMulti1 () {
        /* Multicolumn, drawing drag here (test only covers end-of-drag).
         * B drag to 1,4, E to 2,5
         *
         *   0     1     2     3
         * 0 A
         * 1 A
         * 2 A     D
         * 3 B>4   D
         * 4 B>4   E>5   G     I
         * 5 B>4   E>5   G
         * 6 C     E>5   G
         * 7 C     F     H
         * 8 C     F     H
         * 9
         * 10
         *
         * B to 1,4, F unchanged; E to 2,7, H to 2,10
         */
        List<MoveTest> tests = List.of(
                 moveTest(3,false,0,0, 0) // A
                ,moveTest(3,true ,1,4, 4) // B
                ,moveTest(3,false,0,6, 6) // C
                ,moveTest(2,false,1,2, 2) // D
                ,moveTest(3,true ,2,5, 7) // E
                ,moveTest(2,false,1,7, 7) // F
                ,moveTest(3,false,2,4, 4) // G
                ,moveTest(2,false,2,7, 10) // H
        );
        MoveableModule.resolveCollisions(start(tests));
        assertEquals(intended(tests),start(tests));

    }


}
