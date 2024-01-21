package ru.byprogminer.servertester.config.loader;

import java.util.Objects;
import java.util.function.Function;


public final class ParseResult<T> {

    public final T value;
    public final String rest;

    public ParseResult(T value, String rest) {
        this.value = value;
        this.rest = Objects.requireNonNull(rest);
    }

    public <R> ParseResult<R> map(Function<? super T, ? extends R> f) {
        return new ParseResult<>(f.apply(value), rest);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ParseResult<?> that = (ParseResult<?>) o;
        return Objects.equals(value, that.value) && Objects.equals(rest, that.rest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, rest);
    }

    @Override
    public String toString() {
        return "ParseResult(" + value + ", \"" + rest + "\")";
    }
}
