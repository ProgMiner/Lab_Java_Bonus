package ru.byprogminer.servertester;

import java.util.Objects;


public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException("not instantiable");
    }

    public static <E extends Throwable> E nextException(E current, E next) {
        Objects.requireNonNull(next);

        if (current == null) {
            return next;
        } else {
            current.addSuppressed(next);
            return current;
        }
    }
}
