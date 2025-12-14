package org.louisjohns32.personal.exchange.services;

@FunctionalInterface
public interface IdGenerator<T> {

    public T nextId();

}
