package org.g2fx.g2gui.bridge;

public interface Iso<A, B> {


    B to(A a);

    A from(B b);

    Iso<Integer, Boolean> BOOL_PARAM_ISO = new Iso<>() {
        @Override
        public Boolean to(Integer a) {
            return a == 1;
        }
        @Override
        public Integer from(Boolean b) {
            return b ? 1 : 0;
        }
    };

    Iso<Integer, Number> INTEGER_NUMBER_ISO = new Iso<>() {
        @Override
        public Number to(Integer integer) {
            return integer;
        }

        @Override
        public Integer from(Number number) {
            return number.intValue();
        }
    };

    Iso<Integer, Double> INTEGER_DOUBLE_ISO = new Iso<>() {
        @Override
        public Double to(Integer integer) {
            return integer.doubleValue();
        }

        @Override
        public Integer from(Double number) {
            return number.intValue();
        }
    };


    static <I> Iso<I,I> id() {
        return new Iso<>() {
            @Override public I to(I i) { return i; }
            @Override public I from(I i) { return i; }
        };
    }

}
