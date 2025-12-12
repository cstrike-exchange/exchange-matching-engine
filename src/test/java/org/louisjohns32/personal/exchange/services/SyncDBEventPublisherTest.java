package org.louisjohns32.personal.exchange.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.dao.OrderRepository;
import org.louisjohns32.personal.exchange.dao.TradeRepository;
import org.louisjohns32.personal.exchange.dto.OrderCancellationEvent;
import org.louisjohns32.personal.exchange.dto.OrderCreationEvent;
import org.louisjohns32.personal.exchange.dto.OrderEvent;
import org.louisjohns32.personal.exchange.dto.TradeExecutionEvent;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderStatus;
import org.louisjohns32.personal.exchange.entities.Trade;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SyncDBEventPublisherTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private SyncDBEventPublisher publisher;

    @Nested
    class OrderCreationEventTests {

        @Test
        void publish_orderCreationEvent_savesOrderToDatabase() {
            OrderCreationEvent event = new OrderCreationEvent(
                    1L,
                    "AAPL",
                    Side.BUY,
                    100.0,
                    150.0,
                    0
            );

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            publisher.publish(event);

            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();

            assertEquals(1L, savedOrder.getId());
            assertEquals("AAPL", savedOrder.getSymbol());
            assertEquals(Side.BUY, savedOrder.getSide());
            assertEquals(100.0, savedOrder.getQuantity());
            assertEquals(150.0, savedOrder.getPrice());
            assertEquals(0.0, savedOrder.getFilledQuantity());
            assertEquals(OrderStatus.OPEN, savedOrder.getStatus());
        }

        @Test
        void publish_orderCreationEvent_sellOrder() {
            OrderCreationEvent event = new OrderCreationEvent(
                    2L,
                    "GOOGL",
                    Side.SELL,
                    50.0,
                    2800.0,
                    0
            );

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            publisher.publish(event);

            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();

            assertEquals(Side.SELL, savedOrder.getSide());
            assertEquals("GOOGL", savedOrder.getSymbol());
        }
    }

    @Nested
    class TradeExecutionEventTests {

        @Test
        void publish_TradeExecutionEvent_savesTrade() {
            Long buyOrderId = 1L;
            Long sellOrderId = 2L;

            Order buyOrder = new Order(buyOrderId, "AAPL", Side.BUY, 100.0, 150.0);
            Order sellOrder = new Order(sellOrderId, "AAPL", Side.SELL, 100.0, 150.0);

            when(orderRepository.findById(buyOrderId)).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById(sellOrderId)).thenReturn(Optional.of(sellOrder));

            TradeExecutionEvent event = new TradeExecutionEvent(
                    "AAPL",
                    buyOrderId,
                    sellOrderId,
                    150.0,
                    100.0,
                    System.currentTimeMillis()
            );

            ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);

            publisher.publish(event);

            verify(tradeRepository).save(tradeCaptor.capture());
            Trade savedTrade = tradeCaptor.getValue();

            assertEquals("AAPL", savedTrade.getSymbol());
            assertEquals(buyOrderId, savedTrade.getBuyOrderId());
            assertEquals(sellOrderId, savedTrade.getSellOrderId());
            assertEquals(150.0, savedTrade.getPrice());
            assertEquals(100.0, savedTrade.getQuantity());
            assertNotNull(savedTrade.getExecutedAt());
        }

        @Test
        void publish_TradeExecutionEvent_updatesBothOrders() {
            Long buyOrderId = 1L;
            Long sellOrderId = 2L;

            Order buyOrder = new Order(buyOrderId, "AAPL", Side.BUY, 100.0, 150.0);
            Order sellOrder = new Order(sellOrderId, "AAPL", Side.SELL, 100.0, 150.0);

            when(orderRepository.findById(buyOrderId)).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById(sellOrderId)).thenReturn(Optional.of(sellOrder));

            TradeExecutionEvent event = new TradeExecutionEvent(
                    "AAPL",
                    buyOrderId,
                    sellOrderId,
                    150.0,
                    100.0,
                    System.currentTimeMillis()
            );

            publisher.publish(event);

            verify(orderRepository, times(2)).save(any(Order.class));
            assertEquals(100.0, buyOrder.getFilledQuantity());
            assertEquals(100.0, sellOrder.getFilledQuantity());
        }

        @Test
        void publish_TradeExecutionEvent_fullFill_updatesStatusToFilled() {
            Long buyOrderId = 1L;
            Long sellOrderId = 2L;

            Order buyOrder = new Order(buyOrderId, "AAPL", Side.BUY, 100.0, 150.0);
            Order sellOrder = new Order(sellOrderId, "AAPL", Side.SELL, 100.0, 150.0);

            when(orderRepository.findById(buyOrderId)).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById(sellOrderId)).thenReturn(Optional.of(sellOrder));

            TradeExecutionEvent event = new TradeExecutionEvent(
                    "AAPL",
                    buyOrderId,
                    sellOrderId,
                    150.0,
                    100.0,
                    System.currentTimeMillis()
            );

            publisher.publish(event);

            assertEquals(OrderStatus.FILLED, buyOrder.getStatus());
            assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
        }

        @Test
        void publish_TradeExecutionEvent_partialFill_updatesStatusToPartial() {
            Long buyOrderId = 1L;
            Long sellOrderId = 2L;

            Order buyOrder = new Order(buyOrderId, "AAPL", Side.BUY, 100.0, 150.0);
            Order sellOrder = new Order(sellOrderId, "AAPL", Side.SELL, 100.0, 150.0);

            when(orderRepository.findById(buyOrderId)).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById(sellOrderId)).thenReturn(Optional.of(sellOrder));

            TradeExecutionEvent event = new TradeExecutionEvent(
                    "AAPL",
                    buyOrderId,
                    sellOrderId,
                    150.0,
                    30.0,
                    System.currentTimeMillis()
            );

            publisher.publish(event);

            assertEquals(OrderStatus.PARTIAL, buyOrder.getStatus());
            assertEquals(OrderStatus.PARTIAL, sellOrder.getStatus());
            assertEquals(30.0, buyOrder.getFilledQuantity());
            assertEquals(30.0, sellOrder.getFilledQuantity());
        }

        @Test
        void publish_TradeExecutionEvent_multipleFills_cumulativeQuantity() {
            Long buyOrderId = 1L;
            Long sellOrderId = 2L;

            Order buyOrder = new Order(buyOrderId, "AAPL", Side.BUY, 100.0, 150.0);
            Order sellOrder = new Order(sellOrderId, "AAPL", Side.SELL, 100.0, 150.0);

            buyOrder.setFilledQuantity(20.0);
            sellOrder.setFilledQuantity(20.0);

            when(orderRepository.findById(buyOrderId)).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById(sellOrderId)).thenReturn(Optional.of(sellOrder));

            TradeExecutionEvent event = new TradeExecutionEvent(
                    "AAPL",
                    buyOrderId,
                    sellOrderId,
                    150.0,
                    30.0,
                    System.currentTimeMillis()
            );

            publisher.publish(event);

            assertEquals(50.0, buyOrder.getFilledQuantity());
            assertEquals(50.0, sellOrder.getFilledQuantity());
            assertEquals(OrderStatus.PARTIAL, buyOrder.getStatus());
            assertEquals(OrderStatus.PARTIAL, sellOrder.getStatus());
        }

        @Test
        void publish_TradeExecutionEvent_buyOrderNotFound_throwsException() {
            Long buyOrderId = 1L;
            Long sellOrderId = 2L;

            when(orderRepository.findById(buyOrderId)).thenReturn(Optional.empty());

            TradeExecutionEvent event = new TradeExecutionEvent(
                    "AAPL",
                    buyOrderId,
                    sellOrderId,
                    150.0,
                    100.0,
                    System.currentTimeMillis()
            );

            assertThrows(RuntimeException.class, () -> publisher.publish(event));
        }

        @Test
        void publish_TradeExecutionEvent_sellOrderNotFound_throwsException() {
            Long buyOrderId = 1L;
            Long sellOrderId = 2L;

            Order buyOrder = new Order(buyOrderId, "AAPL", Side.BUY, 100.0, 150.0);
            when(orderRepository.findById(buyOrderId)).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById(sellOrderId)).thenReturn(Optional.empty());

            TradeExecutionEvent event = new TradeExecutionEvent(
                    "AAPL",
                    buyOrderId,
                    sellOrderId,
                    150.0,
                    100.0,
                    System.currentTimeMillis()
            );

            assertThrows(RuntimeException.class, () -> publisher.publish(event));
        }
    }


    @Nested
    class OrderCancellationEventTests {

        @Test
        void publish_OrderCancellationEvent_updatesOrderStatusToCancelled() {
            Long orderId = 1L;
            Order order = new Order(orderId, "AAPL", Side.BUY, 100.0, 150.0);
            order.setStatus(OrderStatus.OPEN);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            OrderCancellationEvent event = new OrderCancellationEvent(orderId, "AAPL", 0);

            publisher.publish(event);

            verify(orderRepository).save(order);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }

        @Test
        void publish_OrderCancellationEvent_partiallyFilledOrder() {
            Long orderId = 1L;
            Order order = new Order(orderId, "AAPL", Side.BUY, 100.0, 150.0);
            order.setFilledQuantity(30.0);
            order.setStatus(OrderStatus.PARTIAL);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            OrderCancellationEvent event = new OrderCancellationEvent(orderId, "AAPL", 0);

            publisher.publish(event);

            verify(orderRepository).save(order);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            assertEquals(30.0, order.getFilledQuantity());
        }

        @Test
        void publish_OrderCancellationEvent_orderNotFound_throwsException() {
            Long orderId = 1L;
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            OrderCancellationEvent event = new OrderCancellationEvent(orderId, "AAPL", 0);

            assertThrows(RuntimeException.class, () -> publisher.publish(event));
        }
    }

    @Nested
    class BatchPublishTests {

        @Test
        void publishBatch_multipleEvents_publishesInOrder() {
            OrderCreationEvent createEvent1 = new OrderCreationEvent(
                    1L, "AAPL", Side.BUY, 100.0, 150.0, 0
            );
            OrderCreationEvent createEvent2 = new OrderCreationEvent(
                    2L, "AAPL", Side.SELL, 100.0, 150.0, 0
            );

            Order buyOrder = new Order(1L, "AAPL", Side.BUY, 100.0, 150.0);
            Order sellOrder = new Order(2L, "AAPL", Side.SELL, 100.0, 150.0);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById(2L)).thenReturn(Optional.of(sellOrder));

            TradeExecutionEvent tradeEvent = new TradeExecutionEvent(
                    "AAPL", 1L, 2L, 150.0, 100.0, System.currentTimeMillis()
            );

            List<OrderEvent> events = Arrays.asList(createEvent1, createEvent2, tradeEvent);

            publisher.publishBatch(events);

            verify(orderRepository, times(4)).save(any(Order.class));
            verify(tradeRepository, times(1)).save(any(Trade.class));
        }

        @Test
        void publishBatch_emptyList_noRepositoryCalls() {
            List<OrderEvent> events = Arrays.asList();

            publisher.publishBatch(events);

            verifyNoInteractions(orderRepository);
            verifyNoInteractions(tradeRepository);
        }

        @Test
        void publishBatch_mixedEventTypes() {
            OrderCreationEvent createEvent = new OrderCreationEvent(
                    1L, "AAPL", Side.BUY, 100.0, 150.0, 0
            );

            Order order = new Order(1L, "AAPL", Side.BUY, 100.0, 150.0);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            OrderCancellationEvent cancelEvent = new OrderCancellationEvent(1L, "AAPL", 0);

            List<OrderEvent> events = Arrays.asList(createEvent, cancelEvent);

            publisher.publishBatch(events);

            verify(orderRepository, times(2)).save(any(Order.class));
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }
    }
}