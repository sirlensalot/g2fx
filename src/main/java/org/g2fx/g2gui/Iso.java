package org.g2fx.g2gui;

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

    static <I> Iso<I,I> id() {
        return new Iso<>() {
            @Override public I to(I i) { return i; }
            @Override public I from(I i) { return i; }
        };
    }

}
