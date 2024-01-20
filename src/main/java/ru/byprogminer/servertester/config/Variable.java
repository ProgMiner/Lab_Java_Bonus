package ru.byprogminer.servertester.config;


public interface Variable<T> extends Iterable<T> {

    boolean isConstant();
}
