package org.louisjohns32.personal.exchange.marketdata.service;

import lombok.extern.slf4j.Slf4j;
import org.louisjohns32.personal.exchange.common.events.OrderEvent;
import org.louisjohns32.personal.exchange.marketdata.core.OrderBookState;
import org.louisjohns32.personal.exchange.marketdata.model.LevelSnapshot;
import org.louisjohns32.personal.exchange.marketdata.model.OrderBookDelta;
import org.louisjohns32.personal.exchange.marketdata.model.OrderBookSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
public class OrderBookStateService {

    @Autowired
    private OrderBookRegistry orderBookRegistry;


    public OrderBookSnapshot getOrderBookSnapshot(String symbol) {
        OrderBookState orderBookState = orderBookRegistry.getOrCreate(symbol);
        return orderBookState.getSnapshot();
    }

    public OrderBookSnapshot getOrderBookSnapshot(String symbol, int depth) {
        OrderBookSnapshot full = getOrderBookSnapshot(symbol);
        if (depth <= 0) {
            return full;
        }
        List<LevelSnapshot> bids =
                full.getBidLevels()
                        .stream()
                        .limit(depth)
                        .toList();
        List<LevelSnapshot> asks =
                full.getAskLevels()
                        .stream()
                        .limit(depth)
                        .toList();
        return OrderBookSnapshot.builder()
                .sequenceNumber(full.getSequenceNumber())
                .timestamp(full.getTimestamp())
                .bidLevels(bids)
                .askLevels(asks)
                .build();
    }


    // consume OrderEvent, pass onto OrderBookState
    public void applyEvent(OrderEvent orderEvent) {
        String symbol = orderEvent.getSymbol();
        OrderBookState orderBookState = orderBookRegistry.getOrCreate(symbol);

        orderBookState.applyEvent(orderEvent);
    }

    public OrderBookDelta flushDelta(String symbol) {
        OrderBookState orderBookState = orderBookRegistry.getOrCreate(symbol);
        return orderBookState.flushDelta();
    }

    public Iterator<String> getSymbols() {
        return orderBookRegistry.getSymbolsIterator();
    }

}
