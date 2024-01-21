package ru.byprogminer.servertester;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


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

    public static void awaitTermination(ExecutorService executorService) throws InterruptedException {
        final boolean res = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        if (!res) {
            throw new RuntimeException("wut");
        }
    }
}
