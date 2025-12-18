package org.louisjohns32.personal.exchange.marketdata.service;

import org.louisjohns32.personal.exchange.marketdata.core.OrderBookState;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderBookRegistry {
    private final Map<String, OrderBookState> registry = new ConcurrentHashMap<>();

    public OrderBookState getOrCreate(String symbol) {
        return registry.computeIfAbsent(symbol, s -> new OrderBookState(symbol));
    }

    public Iterator<String> getSymbolsIterator() {
        return registry.keySet().iterator();
    }

}
