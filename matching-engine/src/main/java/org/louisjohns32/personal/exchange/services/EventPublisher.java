package org.louisjohns32.personal.exchange.services;


import org.louisjohns32.personal.exchange.common.events.OrderEvent;

import java.util.List;

public interface EventPublisher {

    void publish(OrderEvent event);

    void publishBatch(List<OrderEvent> events);

}
