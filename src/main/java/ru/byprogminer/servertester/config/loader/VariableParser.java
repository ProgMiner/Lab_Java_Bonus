package ru.byprogminer.servertester.config.loader;

import ru.byprogminer.servertester.config.Variable;


public interface VariableParser<T> {

    Variable<T> parse(String str);
}
