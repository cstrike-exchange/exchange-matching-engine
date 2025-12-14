package org.louisjohns32.personal.exchange.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.dto.OrderCreationEvent;
import org.louisjohns32.personal.exchange.dto.OrderEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Mock
    private SendResult<String, OrderEvent> sendResult;

    @Captor
    private ArgumentCaptor<OrderEvent> eventCaptor;

    private KafkaEventPublisher eventPublisher;

    private static final String TOPIC = "order.events";

    @BeforeEach
    void setUp() {
        eventPublisher = new KafkaEventPublisher(kafkaTemplate, TOPIC);

        CompletableFuture<SendResult<String, OrderEvent>> future =
                CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);
    }

    @Test
    void shouldCallKafkaTemplateWithCorrectParameters() {
        OrderCreationEvent event = new OrderCreationEvent(
                12345L, "AAPL", Side.BUY, 100.0, 150.0, System.currentTimeMillis()
        );

        eventPublisher.publish(event);

        verify(kafkaTemplate).send(eq(TOPIC), eq("AAPL"), eventCaptor.capture());

        OrderEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isEqualTo(event);
    }

    @Test
    void shouldPublishBatchOfEvents() {
        List<OrderEvent> events = List.of(
                new OrderCreationEvent(1L, "AAPL", Side.BUY, 100.0, 150.0, System.currentTimeMillis()),
                new OrderCreationEvent(2L, "GOOGL", Side.SELL, 50.0, 2800.0, System.currentTimeMillis())
        );

        eventPublisher.publishBatch(events);
        verify(kafkaTemplate, times(2)).send(eq(TOPIC), any(), any());
    }

    @Test
    void shouldUseSymbolAsPartitionKey() {
        OrderCreationEvent appleEvent = new OrderCreationEvent(
                1L, "AAPL", Side.BUY, 100.0, 150.0, System.currentTimeMillis()
        );
        OrderCreationEvent googleEvent = new OrderCreationEvent(
                2L, "GOOGL", Side.BUY, 50.0, 2800.0, System.currentTimeMillis()
        );

        eventPublisher.publish(appleEvent);
        eventPublisher.publish(googleEvent);

        verify(kafkaTemplate).send(eq(TOPIC), eq("AAPL"), any());
        verify(kafkaTemplate).send(eq(TOPIC), eq("GOOGL"), any());
    }

    @Test
    void shouldHandlePublishFailure_throwException() {
        CompletableFuture<SendResult<String, OrderEvent>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);

        OrderCreationEvent event = new OrderCreationEvent(
                12345L, "AAPL", Side.BUY, 100.0, 150.0, System.currentTimeMillis()
        );

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventPublisher.publish(event)
        );

        assertThat(exception.getMessage()).contains("Failed to publish to Kafka");
        verify(kafkaTemplate).send(eq("order.events"), eq("AAPL"), any());
    }
}