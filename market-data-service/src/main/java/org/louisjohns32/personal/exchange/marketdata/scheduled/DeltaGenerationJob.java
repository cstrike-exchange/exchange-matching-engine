package org.louisjohns32.personal.exchange.marketdata.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.louisjohns32.personal.exchange.marketdata.model.OrderBookDelta;
import org.louisjohns32.personal.exchange.marketdata.service.OrderBookDeltaPublisher;
import org.louisjohns32.personal.exchange.marketdata.service.OrderBookStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeltaGenerationJob {

    @Autowired
    private OrderBookStateService orderBookStateService;

    @Autowired
    private OrderBookDeltaPublisher deltaPublisher;

    @Scheduled(fixedDelay = 50)
    public void execute() {

        orderBookStateService.getSymbols().forEachRemaining(symbol -> {
            OrderBookDelta delta = orderBookStateService.flushDelta(symbol);

            deltaPublisher.publishDelta(symbol, delta);
        });

    }

}
