package ru.byprogminer.servertester.config.loader;

import ru.byprogminer.servertester.config.IntVariable;
import ru.byprogminer.servertester.config.Variable;


public class IntVariableParser extends AbstractVariableParser<Integer, Integer> implements VariableParser<Integer> {

    @Override
    protected ParseResult<Integer> parseValue(String str) {
        return Parsers.integer(str).map(v -> (int) (long) v);
    }

    @Override
    protected ParseResult<Integer> parseStep(String str) {
        return parseValue(str);
    }

    @Override
    protected Variable<Integer> constant(Integer value) {
        return IntVariable.constant(value);
    }

    @Override
    protected Variable<Integer> range(Integer begin, Integer end, Integer step) {
        return IntVariable.range(begin, end, step);
    }
}
