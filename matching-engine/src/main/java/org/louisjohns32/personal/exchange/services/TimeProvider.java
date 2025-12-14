package org.louisjohns32.personal.exchange.services;

@FunctionalInterface
public interface TimeProvider {
    long currentTimeMillis();

    static TimeProvider getSystemTimeProvider() {
        return System::currentTimeMillis;
    }
}
