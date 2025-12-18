package org.louisjohns32.personal.exchange.marketdata.consumer;

import lombok.extern.slf4j.Slf4j;
import org.louisjohns32.personal.exchange.common.events.OrderCreationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderEvent;
import org.louisjohns32.personal.exchange.marketdata.service.OrderBookStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderEventConsumer {

    @Autowired
    private OrderBookStateService orderBookService;

    @KafkaListener(topics = "${exchange.kafka.topics.order-events}", groupId = "${exchange.kafka.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(OrderEvent orderEvent) {
        log.info("Order Event {}",orderEvent.getSymbol());
        try {
            if (!(orderEvent instanceof OrderCreationEvent))
                orderBookService.applyEvent(orderEvent);
        }catch (Exception e){
            log.error("Failed to apply event: {}", orderEvent, e);
            //throw e;
        }
    }

}
