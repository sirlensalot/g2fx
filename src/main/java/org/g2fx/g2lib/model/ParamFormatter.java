package org.g2fx.g2lib.model;

import org.g2fx.g2lib.util.Util;

import java.util.function.Function;

public record ParamFormatter(Function<Integer, String> intFmt, Function<Boolean, String> boolFmt) {
    public static ParamFormatter intF(Function<Integer, String> f) {
        return new ParamFormatter(f, null);
    }

    public static ParamFormatter boolF(Function<Boolean, String> f) {
        return new ParamFormatter(null, f);
    }

    public static ParamFormatter ID =
            new ParamFormatter(n -> Integer.toString(n), n -> Boolean.toString(n));

    public static ParamFormatter intMapper(int max) {
        return intF(n -> String.format("%d", Util.mapRange(n, 0, 127, 0, max)));
    }
}
