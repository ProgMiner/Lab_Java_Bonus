package ru.byprogminer.servertester.config;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;


public class GenericVariable<T> implements Variable<T> {

    private final VarProps<T> props;

    private final T begin;
    private final T end;

    private GenericVariable(VarProps<T> props, T begin, T end) {
        this.props = props;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public boolean isConstant() {
        return props == null;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private T value = begin;

            @Override
            public boolean hasNext() {
                return props != null && props.compare(value, end) <= 0;
            }

            @Override
            public T next() {
                final T result = value;
                value = props.increment(value);
                return result;
            }
        };
    }

    public interface VarProps<T> extends Comparator<T> {

        T increment(T value);
    }

    public static <T> GenericVariable<T> constant(T value) {
        return new GenericVariable<>(null, value, null);
    }

    public static <T> GenericVariable<T> range(VarProps<T> props, T begin, T end) {
        Objects.requireNonNull(props, "props");

        if (props.compare(begin, end) > 0) {
            throw new IllegalArgumentException("empty range (begin > end)");
        }

        if (props.compare(props.increment(begin), begin) <= 0) {
            throw new IllegalArgumentException("infinite range (step = 0)");
        }

        return new GenericVariable<>(props, begin, end);
    }
}
