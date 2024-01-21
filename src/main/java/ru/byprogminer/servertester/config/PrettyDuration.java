package ru.byprogminer.servertester.config;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;


public final class PrettyDuration {

    public final Duration value;

    public PrettyDuration(Duration value) {
        this.value = Objects.requireNonNull(value);
    }

    public PrettyDuration map(Function<Duration, Duration> f) {
        return new PrettyDuration(f.apply(value));
    }

    @Override
    public String toString() {
        final long sec = value.getSeconds();

        return (sec / 60 / 60) + ":" + (sec / 60 % 60) + ":" + (sec % 60);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PrettyDuration that = (PrettyDuration) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
