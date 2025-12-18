package org.louisjohns32.personal.exchange.marketdata.service;

import org.louisjohns32.personal.exchange.marketdata.model.OrderBookDelta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderBookDeltaPublisher {

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    public void publishDelta(String symbol, OrderBookDelta delta) {
        messagingTemplate.convertAndSend(
                "/topic/orderbook/" + symbol,
                delta
        );
    }

}
