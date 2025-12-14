package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.common.events.OrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Primary
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
        System.out.println("Publishing order event: " + event + " to topic " + ordersTopic);
        try {
            SendResult<String, OrderEvent> result = kafkaTemplate
                    .send(ordersTopic, event.getSymbol(), event)
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to Kafka", e);
        }
    }

    // TODO need to do batching properly, ---
    @Override
    public void publishBatch(List<OrderEvent> events) {
        events.forEach(this::publish);
    }
}
