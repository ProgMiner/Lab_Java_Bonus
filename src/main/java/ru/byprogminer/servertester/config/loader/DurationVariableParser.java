package ru.byprogminer.servertester.config.loader;

import ru.byprogminer.servertester.config.GenericVariable;
import ru.byprogminer.servertester.config.Variable;

import java.time.Duration;


public class DurationVariableParser
        extends AbstractVariableParser<Duration, Duration>
        implements VariableParser<Duration> {

    @Override
    protected ParseResult<Duration> parseValue(String str) {
        return Parsers.duration(str);
    }

    @Override
    protected ParseResult<Duration> parseStep(String str) {
        return parseValue(str);
    }

    @Override
    protected Variable<Duration> constant(Duration value) {
        return GenericVariable.constant(value);
    }

    @Override
    protected Variable<Duration> range(Duration begin, Duration end, Duration step) {
        return GenericVariable.range(new GenericVariable.VarProps<Duration>() {

            @Override
            public Duration increment(Duration value) {
                return value.plus(step);
            }

            @Override
            public int compare(Duration a, Duration b) {
                return a.compareTo(b);
            }
        }, begin, end);
    }
}
