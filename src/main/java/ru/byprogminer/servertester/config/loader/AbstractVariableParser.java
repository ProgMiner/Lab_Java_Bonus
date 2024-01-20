package ru.byprogminer.servertester.config.loader;

import ru.byprogminer.servertester.config.Variable;


public abstract class AbstractVariableParser<T, S> implements VariableParser<T> {

    @Override
    public final Variable<T> parse(String str) {
        str = str.trim();

        try {
            return parseRange(str);
        } catch (Exception e) {
            try {
                return parseConstant(str);
            } catch (Exception e1) {
                e.addSuppressed(e1);
                throw e;
            }
        }
    }

    protected abstract ParseResult<T> parseValue(String str);
    protected abstract ParseResult<S> parseStep(String str);

    protected abstract Variable<T> constant(T value);
    protected abstract Variable<T> range(T begin, T end, S step);

    private Variable<T> parseConstant(String str) {
        final ParseResult<T> result = parseValue(str);

        Parsers.finish(result.rest);

        return constant(result.value);
    }

    private Variable<T> parseRange(String str) {
        final ParseResult<T> begin = parseValue(str);
        str = begin.rest.trim();

        str = Parsers.string("..", str).trim();

        final ParseResult<T> end = parseValue(str);
        str = end.rest.trim();

        str = Parsers.string(",", str).trim();

        final ParseResult<S> step = parseStep(str);
        str = step.rest.trim();

        Parsers.finish(str);

        return range(begin.value, end.value, step.value);
    }
}
