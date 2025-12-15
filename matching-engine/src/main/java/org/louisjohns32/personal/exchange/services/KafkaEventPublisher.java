package org.louisjohns32.personal.exchange.services;

import lombok.extern.slf4j.Slf4j;
import org.louisjohns32.personal.exchange.common.events.OrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
@Slf4j
public class KafkaEventPublisher implements EventPublisher{

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String ordersTopic;

    public KafkaEventPublisher(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            @Value("${exchange.kafka.topics.order-events}") String ordersTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ordersTopic = ordersTopic;
    }

    @Override
    public void publish(OrderEvent event) {
        kafkaTemplate.send(ordersTopic, event.getSymbol(), event)
                .whenComplete((result, ex) -> {
            if (ex != null) log.error("Failed to publish event", ex);
        });

    }

    @Override
    public void publishBatch(List<OrderEvent> events) {
        events.forEach(this::publish);
    }
}
