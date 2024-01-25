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

    public static int minPowerOfTwo(int n) {
        final int mostSignificantBit = Integer.highestOneBit(n);

        if (n == mostSignificantBit) {
            return n;
        }

        return mostSignificantBit << 1;
    }

    public static void cringeSort(int[] array) {
        for (int i = 0; i < array.length - 1; ++i) {
            final int x = array[i];

            int y = x;
            int k = i;

            for (int j = i + 1; j < array.length; ++j) {
                if (y > array[j]) {
                    y = array[j];
                    k = j;
                }
            }

            array[i] = y;
            array[k] = x;
        }
    }
}
