package ru.byprogminer.servertester.config.loader;

import java.util.Objects;
import java.util.function.Function;


public class ParseResult<T> {

    public final T value;
    public final String rest;

    public ParseResult(T value, String rest) {
        this.value = value;
        this.rest = Objects.requireNonNull(rest);
    }

    public <R> ParseResult<R> map(Function<? super T, ? extends R> f) {
        return new ParseResult<>(f.apply(value), rest);
    }
}
