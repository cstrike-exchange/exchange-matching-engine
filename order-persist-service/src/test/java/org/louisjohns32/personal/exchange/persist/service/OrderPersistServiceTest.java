package org.louisjohns32.personal.exchange.persist.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.common.events.OrderCreationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderEvent;
import org.louisjohns32.personal.exchange.common.events.TradeExecutionEvent;
import org.louisjohns32.personal.exchange.persist.dao.OrderRepository;
import org.louisjohns32.personal.exchange.persist.dao.TradeRepository;
import org.louisjohns32.personal.exchange.persist.entity.OrderEntity;
import org.louisjohns32.personal.exchange.persist.entity.TradeEntity;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPersistServiceTest {

    @InjectMocks
    private OrderPersistService service;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Captor
    private ArgumentCaptor<OrderEntity> orderCaptor;

    @Captor
    private ArgumentCaptor<TradeEntity> tradeCaptor;


    @Test
    void listen_shouldPersistOrder_whenOrderCreationEventReceived() throws Exception {
        OrderCreationEvent event = OrderCreationEvent.builder()
                .orderId(1L)
                .symbol("AAPL")
                .price(150.0)
                .quantity(10.0)
                .build();


        service.consume(List.of(event));

        verify(orderRepository).save(orderCaptor.capture());

        OrderEntity savedEntity = orderCaptor.getValue();
        assertEquals(event.getOrderId(), savedEntity.getId());
        assertEquals(150, savedEntity.getPrice());
        assertEquals("AAPL", savedEntity.getSymbol());
    }

    @Test
    void listen_shouldPersistTrade_whenTradeExecutionEventReceived() throws Exception {
        TradeExecutionEvent event = TradeExecutionEvent.builder()
                .buyOrderId(1)
                .sellOrderId(2)
                .price(150)
                .quantity(5)
                .build();

        OrderEntity buyOrder = new OrderEntity(1L, event.getSymbol(), Side.BUY, event.getQuantity(),  event.getPrice());

        OrderEntity sellOrder = new OrderEntity(2L, event.getSymbol(), Side.SELL, event.getQuantity(),  event.getPrice());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(buyOrder));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(sellOrder));

        service.consume(List.of(event));

        verify(tradeRepository).save(tradeCaptor.capture());

        TradeEntity savedEntity = tradeCaptor.getValue();
        assertEquals(event.getSellOrderId(), savedEntity.getSellOrderId());
        assertEquals(event.getBuyOrderId(), savedEntity.getBuyOrderId());
        assertEquals(150, savedEntity.getPrice());
    }


    @Test
    void consume_shouldPersistMultipleOrders_whenBatchContainsMultipleCreationEvents() {
        OrderCreationEvent event1 = OrderCreationEvent.builder()
                .orderId(1L)
                .symbol("AAPL")
                .price(150.0)
                .quantity(10.0)
                .build();

        OrderCreationEvent event2 = OrderCreationEvent.builder()
                .orderId(2L)
                .symbol("GOOGL")
                .price(2800.0)
                .quantity(5.0)
                .build();

        service.consume(List.of(event1, event2));

        verify(orderRepository, times(2)).save(orderCaptor.capture());

        List<OrderEntity> savedEntities = orderCaptor.getAllValues();
        assertEquals(2, savedEntities.size());
        assertEquals(1L, savedEntities.get(0).getId());
        assertEquals(2L, savedEntities.get(1).getId());
    }

    @Test
    void consume_shouldHandleMixedEventTypes_whenBatchContainsDifferentEvents() {
        OrderCreationEvent creationEvent = OrderCreationEvent.builder()
                .orderId(1L)
                .symbol("AAPL")
                .price(150.0)
                .quantity(10.0)
                .build();

        TradeExecutionEvent tradeEvent = TradeExecutionEvent.builder()
                .buyOrderId(2L)
                .sellOrderId(3L)
                .symbol("AAPL")
                .price(150.0)
                .quantity(5.0)
                .build();

        OrderEntity buyOrder = new OrderEntity(2L, "AAPL", Side.BUY, 5.0, 150.0);
        OrderEntity sellOrder = new OrderEntity(3L, "AAPL", Side.SELL, 5.0, 150.0);

        when(orderRepository.findById(2L)).thenReturn(Optional.of(buyOrder));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(sellOrder));

        service.consume(List.of(creationEvent, tradeEvent));

        verify(orderRepository, times(3)).save(any(OrderEntity.class));
        verify(tradeRepository).save(any(TradeEntity.class));
    }

    @Test
    void consume_shouldPropagateException_whenOrderPersistenceFails() {
        OrderCreationEvent event = OrderCreationEvent.builder()
                .orderId(1L)
                .symbol("AAPL")
                .price(150.0)
                .quantity(10.0)
                .build();
        when(orderRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> service.consume(List.of(event)));
    }

    @Test
    void consume_shouldPropagateException_whenTradeExecutionFailsMidBatch() {
        OrderCreationEvent event1 = OrderCreationEvent.builder()
                .orderId(1L)
                .symbol("AAPL")
                .price(150.0)
                .quantity(10.0)
                .build();

        TradeExecutionEvent event2 = TradeExecutionEvent.builder()
                .buyOrderId(999L)
                .sellOrderId(2L)
                .symbol("AAPL")
                .price(150.0)
                .quantity(5.0)
                .build();

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.consume(List.of(event1, event2)));
    }

    @Test
    void consume_shouldHandleEmptyBatch() {
        service.consume(List.of());

        verify(orderRepository, never()).save(any());
        verify(tradeRepository, never()).save(any());
    }

    @Test
    void consume_shouldProcessLargeBatch() {
        List<OrderEvent> events = new ArrayList<>();
        for (long i = 1; i <= 500; i++) {
            events.add(OrderCreationEvent.builder()
                    .orderId(i)
                    .symbol("AAPL")
                    .price(150.0)
                    .quantity(10.0)
                    .build());
        }

        service.consume(events);

        verify(orderRepository, times(500)).save(any(OrderEntity.class));
    }


}