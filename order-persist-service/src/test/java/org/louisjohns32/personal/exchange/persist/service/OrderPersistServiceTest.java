package org.louisjohns32.personal.exchange.persist.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.common.events.OrderCreationEvent;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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


        service.consume(event);

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

        service.consume(event);

        verify(tradeRepository).save(tradeCaptor.capture());

        TradeEntity savedEntity = tradeCaptor.getValue();
        assertEquals(event.getSellOrderId(), savedEntity.getSellOrderId());
        assertEquals(event.getBuyOrderId(), savedEntity.getBuyOrderId());
        assertEquals(150, savedEntity.getPrice());
    }


}