package ru.byprogminer.servertester.config.loader;

import ru.byprogminer.servertester.config.GenericVariable;
import ru.byprogminer.servertester.config.PrettyDuration;
import ru.byprogminer.servertester.config.Variable;

import java.time.Duration;


public class DurationVariableParser
        extends AbstractVariableParser<PrettyDuration, Duration>
        implements VariableParser<PrettyDuration> {

    @Override
    protected ParseResult<PrettyDuration> parseValue(String str) {
        return Parsers.duration(str).map(PrettyDuration::new);
    }

    @Override
    protected ParseResult<Duration> parseStep(String str) {
        return Parsers.duration(str);
    }

    @Override
    protected Variable<PrettyDuration> constant(PrettyDuration value) {
        return GenericVariable.constant(value);
    }

    @Override
    protected Variable<PrettyDuration> range(PrettyDuration begin, PrettyDuration end, Duration step) {
        return GenericVariable.range(new GenericVariable.VarProps<PrettyDuration>() {

            @Override
            public PrettyDuration increment(PrettyDuration value) {
                return value.map(v -> v.plus(step));
            }

            @Override
            public String printStep() {
                return new PrettyDuration(step).toString();
            }

            @Override
            public int compare(PrettyDuration a, PrettyDuration b) {
                return a.value.compareTo(b.value);
            }
        }, begin, end);
    }
}
