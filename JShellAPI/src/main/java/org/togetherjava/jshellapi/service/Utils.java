package org.togetherjava.jshellapi.service;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Utils {
    public static <E extends Enum<E>, X extends Exception> E nameOrElseThrow(Class<E> c,
            String name, Function<String, X> exceptionFunction) throws X {
        return name(c, name).orElseThrow(() -> exceptionFunction.apply(name));
    }

    public static <E extends Enum<E>> Optional<E> name(Class<E> c, String name) {
        return predicate(c, e -> e.name().equals(name)).findAny();
    }

    public static <E extends Enum<E>, K> Optional<E> key(Class<E> c, Function<E, K> keyMapper,
            K name) {
        return predicate(c, e -> keyMapper.apply(e).equals(name)).findAny();
    }

    public static <E extends Enum<E>> Stream<E> predicate(Class<E> c, Predicate<E> predicate) {
        E[] enumConstants = c.getEnumConstants();
        if (enumConstants == null) {
            throw new RuntimeException(); // Impossible
        }
        return Arrays.stream(enumConstants).filter(predicate);
    }

    public static String sanitizeStartupScript(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n");
    }

    public static String deSanitizeStartupScript(String text) {
        return text.replace("\\n", "\n").replace("\\\\", "\\");
    }
}
