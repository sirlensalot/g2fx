package g2lib.util;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public record SafeLookup<K, E>(Map<K, E> m, String name) {

    public static <E extends Enum<E>> SafeLookup<Integer,E> makeEnumOrdLookup(E[] values) {
        return makeLookup(values,Enum::ordinal);
    }

    public static <E extends Enum<E>> SafeLookup<String,E> makeEnumNameLookup(E[] values) {
        return makeLookup(values,Enum::name);
    }

    public static <E extends Enum<E>> SafeLookup<String,E> makeLowerCaseNameLookup(E[] values) {
        return makeLookup(values,e -> e.name().toLowerCase());
    }

    public static <K extends Comparable<K>,E extends Enum<E>> SafeLookup<K,E> makeLookup(
            E[] values, Function<E,K> accessor) {
        Map<K, E> m = new TreeMap<>();
        for (E e : values) {
            m.put(accessor.apply(e),e);
        }
        return new SafeLookup<>(m,values[0].getDeclaringClass().getSimpleName());
    }

    public E get(K i) {
        E e = m.get(i);
        if (e == null) {
            throw new IllegalArgumentException(name + ": lookup failed: " + i);
        }
        return e;
    }
}
