package ru.byprogminer.servertester.config;

import java.util.Iterator;


public class IntVariable implements Variable<Integer> {

    private final int begin;
    private final int end;
    private final int step;

    private IntVariable(int begin, int end, int step) {
        this.begin = begin;
        this.end = end;
        this.step = step;
    }

    @Override
    public boolean isConstant() {
        return step == 0;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {

            private int value = begin;

            @Override
            public boolean hasNext() {
                return value <= end;
            }

            @Override
            public Integer next() {
                final int result = value;
                value += step;
                return result;
            }
        };
    }

    public static IntVariable constant(int value) {
        return new IntVariable(value, value, 0);
    }

    public static IntVariable range(int begin, int end, int step) {
        if (begin > end) {
            throw new IllegalArgumentException("empty range (begin > end)");
        }

        if (step <= 0) {
            throw new IllegalArgumentException("infinite range (step = 0)");
        }

        return new IntVariable(begin, end, step);
    }
}
