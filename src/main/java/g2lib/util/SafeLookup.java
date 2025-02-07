package g2lib.util;

import java.util.Map;
import java.util.TreeMap;

public record SafeLookup<K, E>(Map<K, E> m, String name) {

    public static <E extends Enum<E>> SafeLookup<Integer,E> makeEnumOrdLookup(E[] values) {
        Map<Integer, E> m = new TreeMap<>();
        for (E e : values) {
            m.put(e.ordinal(),e);
        }
        return new SafeLookup<Integer,E>(m,values[0].getDeclaringClass().getSimpleName());
    }

    public static <E extends Enum<E>> SafeLookup<String,E> makeEnumNameLookup(E[] values) {
        Map<String, E> m = new TreeMap<>();
        for (E e : values) {
            m.put(e.name(),e);
        }
        return new SafeLookup<String,E>(m,values[0].getDeclaringClass().getSimpleName());
    }

    public E get(K i) {
        E e = m.get(i);
        if (e == null) {
            throw new IllegalArgumentException(name + ": lookup failed: " + i);
        }
        return e;
    }
}
