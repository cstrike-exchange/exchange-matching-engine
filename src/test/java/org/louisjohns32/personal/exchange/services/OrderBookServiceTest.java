package org.louisjohns32.personal.exchange.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.dto.*;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.entities.OrderBookLevel;
import org.louisjohns32.personal.exchange.entities.Trade;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrderBookServiceTest {

    @Mock
    private Validator validator;

    @Mock
    private OrderBook orderBook;

    @Mock
    private OrderBookRegistry orderBookRegistry;

    @InjectMocks
    @Spy
    private OrderBookServiceImpl orderBookService;

    @Mock
    private EventPublisher eventPublisher;

    private static final String SYMBOL = "AAPL";
    private Order bidOrder;
    private Order askOrder;

    @BeforeEach
    void setUp() {
        bidOrder = new Order(1L, SYMBOL, Side.BUY, 5.0, 100.0);
        askOrder = new Order(2L, SYMBOL, Side.SELL, 5.0, 99.0);
    }

    @Nested
    class OrderBookRegistryTests {
        @Test
        void createOrderBook() {
            String symbol = "SYMBOL";
            orderBookService.createOrderBook(symbol);

            verify(orderBookRegistry, times(1)).createOrderBook(eq(symbol));
        }

        @Test
        void getOrderBook() {
            String symbol = "SYMBOL";
            when(orderBookRegistry.getOrderBook(eq(symbol))).thenReturn(orderBook);

            OrderBook resOrderBook = orderBookService.getOrderBook(symbol);

            assertEquals(orderBook, resOrderBook);
        }
    }

    @Nested
    class CreateOrderTests {

        @Test
        void validOrder_addsToOrderBookAndMatches() {
            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());

            Order newOrder = orderBookService.createOrder(orderBook, bidOrder);

            assertNotNull(newOrder);
            verify(orderBook, times(1)).addOrder(any(Order.class));
            verify(orderBookService, times(1)).match(orderBook, newOrder);
        }

        @Test
        void invalidOrder_throwsConstraintViolationException() {
            ConstraintViolation<Order> violation = mock(ConstraintViolation.class);
            when(validator.validate(any(Order.class))).thenReturn(Set.of(violation));

            assertThrows(ConstraintViolationException.class,
                    () -> orderBookService.createOrder(orderBook, bidOrder));

            verify(orderBook, never()).addOrder(any(Order.class));
            verify(orderBookService, never()).match(any(OrderBook.class), any(Order.class));
        }

        @Test
        void validOrder_addsOrderToBook() {
            Order inputOrder = new Order(0L, SYMBOL, Side.BUY, 100.0, 10.0);
            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
            when(orderBook.getLowestAskLevel()).thenReturn(null);

            Order result = orderBookService.createOrder(orderBook, inputOrder);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(orderBook).addOrder(any(Order.class));
        }
    }

    @Nested
    class DeleteOrderTests {

        @Test
        void existingOrder_removesOrder() {
            when(orderBook.getOrderById(1)).thenReturn(bidOrder);

            orderBookService.deleteOrderById(orderBook, 1);

            verify(orderBook, times(1)).removeOrder(bidOrder);
        }
    }

    @Nested
    class FillOrderTests {

        @Test
        void partialFill_updatesRemainingQuantity() {
            bidOrder = spy(new Order(1L, SYMBOL, Side.BUY, 5.0, 100.0));
            when(orderBook.getOrderById(1)).thenReturn(bidOrder);

            double remainingQty = orderBookService.fillOrder(orderBook, bidOrder, 3.0);

            assertEquals(2.0, remainingQty);
            verify(orderBook, never()).removeOrder(bidOrder);
        }

        @Test
        void fullFill_removesOrder() {
            bidOrder = spy(new Order(1L, SYMBOL, Side.BUY, 5.0, 100.0));
            when(orderBook.getOrderById(1)).thenReturn(bidOrder);

            double remainingQty = orderBookService.fillOrder(orderBook, bidOrder, 5.0);

            assertEquals(0.0, remainingQty);
            verify(orderBook, times(1)).removeOrder(bidOrder);
        }
    }

    @Nested
    class MatchOrderTests {
        @Test
        void executesTradeBid() {
            OrderBookLevel bidLevel = mock(OrderBookLevel.class);
            OrderBookLevel askLevel = mock(OrderBookLevel.class);

            when(orderBook.getHighestBidLevel()).thenReturn(bidLevel);
            when(orderBook.getLowestAskLevel()).thenReturn(askLevel);
            when(bidLevel.getPrice()).thenReturn(100.0);
            when(askLevel.getPrice()).thenReturn(99.0);
            when(bidLevel.getOrder()).thenReturn(bidOrder);
            when(askLevel.getOrder()).thenReturn(askOrder);
            when(orderBook.getOrderById(bidOrder.getId())).thenReturn(bidOrder);
            when(orderBook.getOrderById(askOrder.getId())).thenReturn(askOrder);

            orderBookService.match(orderBook, bidOrder);

            assertEquals(0.0, askOrder.getRemainingQuantity());
            assertEquals(0.0, bidOrder.getRemainingQuantity());
            verify(orderBook).removeOrder(askOrder);
            verify(orderBook).removeOrder(bidOrder);
        }

        @Test
        void executesTradeAsk() {
            OrderBookLevel bidLevel = mock(OrderBookLevel.class);
            OrderBookLevel askLevel = mock(OrderBookLevel.class);

            when(orderBook.getHighestBidLevel()).thenReturn(bidLevel);
            when(orderBook.getLowestAskLevel()).thenReturn(askLevel);
            when(bidLevel.getPrice()).thenReturn(100.0);
            when(askLevel.getPrice()).thenReturn(99.0);
            when(bidLevel.getOrder()).thenReturn(bidOrder);
            when(askLevel.getOrder()).thenReturn(askOrder);
            when(orderBook.getOrderById(bidOrder.getId())).thenReturn(bidOrder);
            when(orderBook.getOrderById(askOrder.getId())).thenReturn(askOrder);

            orderBookService.match(orderBook, askOrder);

            assertEquals(0.0, askOrder.getRemainingQuantity());
            assertEquals(0.0, bidOrder.getRemainingQuantity());
            verify(orderBook).removeOrder(askOrder);
            verify(orderBook).removeOrder(bidOrder);
        }

        @Test
        void doesNotExecuteTrade() {
            OrderBookLevel bidLevel = mock(OrderBookLevel.class);
            OrderBookLevel askLevel = mock(OrderBookLevel.class);

            bidOrder = new Order(1L, SYMBOL, Side.BUY, 5.0, 98.0);
            askOrder = new Order(2L, SYMBOL, Side.SELL, 5.0, 99.0);

            when(orderBook.getHighestBidLevel()).thenReturn(bidLevel);
            when(orderBook.getLowestAskLevel()).thenReturn(askLevel);
            when(bidLevel.getPrice()).thenReturn(98.0);
            when(askLevel.getPrice()).thenReturn(99.0);
            when(bidLevel.getOrder()).thenReturn(bidOrder);
            when(askLevel.getOrder()).thenReturn(askOrder);

            orderBookService.match(orderBook, bidOrder);

            assertEquals(5.0, askOrder.getRemainingQuantity());
            assertEquals(5.0, bidOrder.getRemainingQuantity());
            verify(orderBook, never()).removeOrder(any());
        }

        @Test
        void correctlyHandlesRemainingQuantity() {
            OrderBookLevel bidLevel = mock(OrderBookLevel.class);
            OrderBookLevel askLevel = mock(OrderBookLevel.class);

            askOrder = new Order(2L, SYMBOL, Side.SELL, 3.0, 99.0);

            when(orderBook.getHighestBidLevel()).thenReturn(bidLevel);
            when(orderBook.getLowestAskLevel()).thenReturn(askLevel);
            when(orderBook.getOrderById(askOrder.getId())).thenReturn(askOrder);
            when(bidLevel.getPrice()).thenReturn(100.0);
            when(askLevel.getPrice()).thenReturn(99.0);
            when(bidLevel.getOrder()).thenReturn(bidOrder);
            when(askLevel.getOrder()).thenReturn(askOrder);

            doAnswer(invocation -> {
                when(orderBook.getLowestAskLevel()).thenReturn(null);
                return null;
            }).when(orderBook).removeOrder(askOrder);

            orderBookService.match(orderBook, bidOrder);

            assertEquals(2.0, bidOrder.getRemainingQuantity());
            assertEquals(0.0, askOrder.getRemainingQuantity());

            verify(orderBook, times(1)).removeOrder(askOrder);

            assertNull(orderBook.getLowestAskLevel(),
                    "Expected lowest ask level to be null after removal.");
        }

        @Test
        void matchesWithMultipleSmallerOrders() {
            OrderBookLevel askLevel1 = mock(OrderBookLevel.class);
            OrderBookLevel askLevel2 = mock(OrderBookLevel.class);
            OrderBookLevel askLevel3 = mock(OrderBookLevel.class);

            bidOrder = new Order(1L, SYMBOL, Side.BUY, 20.0, 100.0);
            Order smallAsk1 = new Order(2L, SYMBOL, Side.SELL, 3.0, 99.0);
            Order smallAsk2 = new Order(3L, SYMBOL, Side.SELL, 6.0, 99.0);
            Order smallAsk3 = new Order(4L, SYMBOL, Side.SELL, 1.0, 98.0);
            Order ask4 = new Order(5L, SYMBOL, Side.SELL, 1.0, 101.0);

            when(orderBook.getLowestAskLevel()).thenReturn(askLevel2);

            when(orderBook.getOrderById(smallAsk1.getId())).thenReturn(smallAsk1);
            when(orderBook.getOrderById(smallAsk2.getId())).thenReturn(smallAsk2);
            when(orderBook.getOrderById(smallAsk3.getId())).thenReturn(smallAsk3);
            when(orderBook.getOrderById(ask4.getId())).thenReturn(ask4);

            when(askLevel1.getPrice()).thenReturn(99.0);
            when(askLevel1.getOrder()).thenReturn(smallAsk1);

            when(askLevel2.getPrice()).thenReturn(98.0);
            when(askLevel2.getOrder()).thenReturn(smallAsk3);

            when(askLevel3.getPrice()).thenReturn(101.0);
            when(askLevel3.getOrder()).thenReturn(ask4);

            doAnswer(invocation -> {
                when(orderBook.getLowestAskLevel()).thenReturn(askLevel1);
                return null;
            }).when(orderBook).removeOrder(smallAsk3);

            doAnswer(invocation -> {
                when(askLevel1.getOrder()).thenReturn(smallAsk2);
                return null;
            }).when(orderBook).removeOrder(smallAsk1);

            doAnswer(invocation -> {
                when(orderBook.getLowestAskLevel()).thenReturn(askLevel3);
                return null;
            }).when(orderBook).removeOrder(smallAsk2);

            orderBookService.match(orderBook, bidOrder);

            assertEquals(10.0, bidOrder.getRemainingQuantity(),
                    "Bid order should have 10/20 filled.");
            assertEquals(0.0, smallAsk1.getRemainingQuantity(),
                    "First small ask should be fully filled.");
            assertEquals(0.0, smallAsk2.getRemainingQuantity(),
                    "Second small ask should be fully filled.");
            assertEquals(0.0, smallAsk3.getRemainingQuantity(),
                    "Third small ask should be fully filled.");
            assertEquals(1.0, ask4.getRemainingQuantity(),
                    "Fourth ask shouldn't be filled at all.");
            verify(orderBook, times(1)).removeOrder(smallAsk1);
            verify(orderBook, times(1)).removeOrder(smallAsk2);
            verify(orderBook, times(1)).removeOrder(smallAsk3);
            verify(orderBook, never()).removeOrder(ask4);
        }
    }

    @Nested
    class AggregatedOrderBookTests {

        private OrderBook mockOrderBook;
        private final String symbol = "SYMBOL";

        @BeforeEach
        void setup() {
            mockOrderBook = mock(OrderBook.class);

            when(orderBookRegistry.getOrderBook(eq(symbol))).thenReturn(mockOrderBook);
        }

        @Test
        void returnsEmptyAggregatedOrderBook() {
            when(mockOrderBook.getBidLevels()).thenReturn(Collections.emptyNavigableMap());
            when(mockOrderBook.getAskLevels()).thenReturn(Collections.emptyNavigableMap());

            OrderBookDTO dto = orderBookService.getAggregatedOrderBook(symbol);

            assertNotNull(dto);
            assertTrue(dto.getBidLevels().isEmpty(), "Expected empty bids");
            assertTrue(dto.getAskLevels().isEmpty(), "Expected empty asks");
        }

        @Test
        void aggregatesOrdersAtSingleBidAndAskLevel() {
            OrderBookLevel bidLevel = new OrderBookLevel(100.0, Side.BUY);
            bidLevel.addOrder(new Order(1L, symbol, Side.BUY, 5.0, 100.0));

            OrderBookLevel askLevel = new OrderBookLevel(101.0, Side.SELL);
            askLevel.addOrder(new Order(2L, symbol, Side.SELL, 3.0, 101.0));

            when(mockOrderBook.getBidLevels()).thenReturn(
                    new ConcurrentSkipListMap<>(Map.of(100.0, bidLevel)));
            when(mockOrderBook.getAskLevels()).thenReturn(
                    new ConcurrentSkipListMap<>(Map.of(101.0, askLevel)));

            when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

            OrderBookDTO dto = orderBookService.getAggregatedOrderBook(symbol);

            assertEquals(1, dto.getBidLevels().size());
            assertEquals(1, dto.getAskLevels().size());

            assertEquals(100.0, dto.getBidLevels().getFirst().getPrice());
            assertEquals(5.0, dto.getBidLevels().getFirst().getVolume());

            assertEquals(101.0, dto.getAskLevels().getFirst().getPrice());
            assertEquals(3.0, dto.getAskLevels().getFirst().getVolume());
        }

        @Test
        void aggregatesMultipleOrdersAtSameLevel() {
            OrderBookLevel bidLevel = new OrderBookLevel(100.0, Side.BUY);
            bidLevel.addOrder(new Order(1L, symbol, Side.BUY, 5.0, 100.0));
            bidLevel.addOrder(new Order(2L, symbol, Side.BUY, 10.0, 100.0));

            when(mockOrderBook.getBidLevels()).thenReturn(
                    new ConcurrentSkipListMap<>(Map.of(100.0, bidLevel)));
            when(mockOrderBook.getAskLevels()).thenReturn(Collections.emptyNavigableMap());

            when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

            OrderBookDTO dto = orderBookService.getAggregatedOrderBook(symbol);

            assertEquals(1, dto.getBidLevels().size());
            assertEquals(15.0, dto.getBidLevels().get(0).getVolume());
        }

        @Test
        void aggregatesOrdersAcrossMultipleLevels() {
            OrderBookLevel bidLevel1 = new OrderBookLevel(101.0, Side.BUY);
            bidLevel1.addOrder(new Order(1L, symbol, Side.BUY, 5.0, 101.0));

            OrderBookLevel bidLevel2 = new OrderBookLevel(100.0, Side.BUY);
            bidLevel2.addOrder(new Order(2L, symbol, Side.BUY, 10.0, 100.0));

            OrderBookLevel askLevel1 = new OrderBookLevel(102.0, Side.SELL);
            askLevel1.addOrder(new Order(3L, symbol, Side.SELL, 4.0, 102.0));

            OrderBookLevel askLevel2 = new OrderBookLevel(103.0, Side.SELL);
            askLevel2.addOrder(new Order(4L, symbol, Side.SELL, 6.0, 103.0));

            when(mockOrderBook.getBidLevels()).thenReturn(
                    new ConcurrentSkipListMap<>(Map.of(
                            101.0, bidLevel1,
                            100.0, bidLevel2
                    )));
            when(mockOrderBook.getAskLevels()).thenReturn(
                    new ConcurrentSkipListMap<>(Map.of(
                            102.0, askLevel1,
                            103.0, askLevel2
                    )));

            when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

            OrderBookDTO dto = orderBookService.getAggregatedOrderBook(symbol);

            assertEquals(2, dto.getBidLevels().size());
            assertEquals(2, dto.getAskLevels().size());

            assertEquals(5.0, dto.getBidLevels().get(0).getVolume());
            assertEquals(10.0, dto.getBidLevels().get(1).getVolume());
            assertEquals(4.0, dto.getAskLevels().get(0).getVolume());
            assertEquals(6.0, dto.getAskLevels().get(1).getVolume());
        }
    }

    @Nested
    class EventPublishingTests {

        @Test
        void createOrder_publishesOrderCreationEventInBatch() {
            Order newOrder = new Order(null, SYMBOL, Side.BUY, 100.0, 150.0);
            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
            when(orderBookRegistry.getOrderBook(SYMBOL)).thenReturn(orderBook);
            when(orderBook.getSymbol()).thenReturn(SYMBOL);

            orderBookService.createOrder(SYMBOL, newOrder);

            verify(eventPublisher).publishBatch(any(List.class));
        }

        @Test
        void createOrder_publishesOrderCreationEventWithCorrectData() {
            Order newOrder = new Order(null, SYMBOL, Side.BUY, 100.0, 150.0);
            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
            when(orderBookRegistry.getOrderBook(SYMBOL)).thenReturn(orderBook);
            when(orderBook.getSymbol()).thenReturn(SYMBOL);

            ArgumentCaptor<List<OrderEvent>> batchCaptor = ArgumentCaptor.forClass(List.class);

            Order result = orderBookService.createOrder(SYMBOL, newOrder);

            verify(eventPublisher).publishBatch(batchCaptor.capture());
            List<OrderEvent> events = batchCaptor.getValue();

            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof OrderCreationEvent);

            OrderCreationEvent event = (OrderCreationEvent) events.get(0);
            assertEquals(result.getId(), event.getOrderId());
            assertEquals(SYMBOL, event.getSymbol());
            assertEquals(Side.BUY, event.getSide());
            assertEquals(100.0, event.getQuantity());
            assertEquals(150.0, event.getPrice());
        }

        @Test
        void createMatchingOrder_publishesTradeExecutionEventInBatch() {
            OrderBook realOrderBook = new OrderBook(SYMBOL);

            Order existingSellOrder = new Order(2L, SYMBOL, Side.SELL, 100.0, 150.0);
            realOrderBook.addOrder(existingSellOrder);

            Order newBuyOrder = new Order(null, SYMBOL, Side.BUY, 100.0, 150.0);

            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
            when(orderBookRegistry.getOrderBook(SYMBOL)).thenReturn(realOrderBook);

            ArgumentCaptor<List<OrderEvent>> batchCaptor = ArgumentCaptor.forClass(List.class);

            orderBookService.createOrder(SYMBOL, newBuyOrder);

            verify(eventPublisher).publishBatch(batchCaptor.capture());
            List<OrderEvent> events = batchCaptor.getValue();

            assertTrue(events.stream().anyMatch(e -> e instanceof TradeExecutionEvent),
                    "Expected at least one TradeExecutionEvent");
        }

        @Test
        void createMatchingOrder_publishesTradeExecutionEventWithCorrectData() {
            OrderBook realOrderBook = new OrderBook(SYMBOL);

            Order existingSellOrder = new Order(2L, SYMBOL, Side.SELL, 100.0, 150.0);
            realOrderBook.addOrder(existingSellOrder);

            Order newBuyOrder = new Order(null, SYMBOL, Side.BUY, 100.0, 150.0);

            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
            when(orderBookRegistry.getOrderBook(SYMBOL)).thenReturn(realOrderBook);

            ArgumentCaptor<List<OrderEvent>> batchCaptor = ArgumentCaptor.forClass(List.class);

            Order result = orderBookService.createOrder(SYMBOL, newBuyOrder);

            verify(eventPublisher).publishBatch(batchCaptor.capture());
            List<OrderEvent> events = batchCaptor.getValue();

            TradeExecutionEvent event = (TradeExecutionEvent) events.stream()
                    .filter(e -> e instanceof TradeExecutionEvent)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No TradeExecutionEvent found"));

            assertEquals(SYMBOL, event.getSymbol());
            assertEquals(result.getId(), event.getBuyOrderId());
            assertEquals(2L, event.getSellOrderId());
            assertEquals(150.0, event.getPrice());
            assertEquals(100.0, event.getQuantity());
        }

        @Test
        void createPartialMatchOrder_publishesTradeExecutionEventWithPartialQuantity() {
            OrderBook realOrderBook = new OrderBook(SYMBOL);

            Order existingSellOrder = new Order(2L, SYMBOL, Side.SELL, 50.0, 150.0);
            realOrderBook.addOrder(existingSellOrder);

            Order newBuyOrder = new Order(null, SYMBOL, Side.BUY, 100.0, 150.0);

            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
            when(orderBookRegistry.getOrderBook(SYMBOL)).thenReturn(realOrderBook);

            ArgumentCaptor<List<OrderEvent>> batchCaptor = ArgumentCaptor.forClass(List.class);

            orderBookService.createOrder(SYMBOL, newBuyOrder);

            verify(eventPublisher).publishBatch(batchCaptor.capture());
            List<OrderEvent> events = batchCaptor.getValue();

            TradeExecutionEvent event = (TradeExecutionEvent) events.stream()
                    .filter(e -> e instanceof TradeExecutionEvent)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No TradeExecutionEvent found"));

            assertEquals(50.0, event.getQuantity());
        }

        @Test
        void createOrderWithMultipleMatches_publishesMultipleTradeExecutionEventsInBatch() {
            OrderBook realOrderBook = new OrderBook(SYMBOL);

            Order sellOrder1 = new Order(2L, SYMBOL, Side.SELL, 50.0, 150.0);
            Order sellOrder2 = new Order(3L, SYMBOL, Side.SELL, 100.0, 150.0);
            realOrderBook.addOrder(sellOrder1);
            realOrderBook.addOrder(sellOrder2);

            Order newBuyOrder = new Order(null, SYMBOL, Side.BUY, 150.0, 150.0);

            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
            when(orderBookRegistry.getOrderBook(SYMBOL)).thenReturn(realOrderBook);

            ArgumentCaptor<List<OrderEvent>> batchCaptor = ArgumentCaptor.forClass(List.class);

            orderBookService.createOrder(SYMBOL, newBuyOrder);

            verify(eventPublisher).publishBatch(batchCaptor.capture());
            List<OrderEvent> events = batchCaptor.getValue();

            long tradeEventCount = events.stream()
                    .filter(e -> e instanceof TradeExecutionEvent)
                    .count();

            assertEquals(2, tradeEventCount);
        }

        @Test
        void createOrderWithNoMatch_doesNotPublishTradeExecutionEvent() {
            OrderBook realOrderBook = new OrderBook(SYMBOL);

            Order existingSellOrder = new Order(2L, SYMBOL, Side.SELL, 100.0, 200.0);
            realOrderBook.addOrder(existingSellOrder);

            Order newBuyOrder = new Order(null, SYMBOL, Side.BUY, 100.0, 100.0);

            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
            when(orderBookRegistry.getOrderBook(SYMBOL)).thenReturn(realOrderBook);

            ArgumentCaptor<List<OrderEvent>> batchCaptor = ArgumentCaptor.forClass(List.class);

            orderBookService.createOrder(SYMBOL, newBuyOrder);

            verify(eventPublisher).publishBatch(batchCaptor.capture());
            List<OrderEvent> events = batchCaptor.getValue();

            long tradeEventCount = events.stream()
                    .filter(e -> e instanceof TradeExecutionEvent)
                    .count();

            assertEquals(0, tradeEventCount);
        }

        @Test
        void deleteOrder_publishesOrderCancellationEvent() {
            Order order = new Order(1L, SYMBOL, Side.BUY, 100.0, 150.0);
            when(orderBook.getOrderById(1L)).thenReturn(order);
            when(orderBook.getSymbol()).thenReturn(SYMBOL);

            orderBookService.deleteOrderById(orderBook, 1L);

            verify(eventPublisher).publish(any(OrderCancellationEvent.class));
        }

        @Test
        void deleteOrder_publishesOrderCancellationEventWithCorrectData() {
            Order order = new Order(1L, SYMBOL, Side.BUY, 100.0, 150.0);
            when(orderBook.getOrderById(1L)).thenReturn(order);
            when(orderBook.getSymbol()).thenReturn(SYMBOL);

            ArgumentCaptor<OrderCancellationEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancellationEvent.class);

            orderBookService.deleteOrderById(orderBook, 1L);

            verify(eventPublisher).publish(eventCaptor.capture());
            OrderCancellationEvent event = eventCaptor.getValue();

            assertEquals(1L, event.getOrderId());
            assertEquals(SYMBOL, event.getSymbol());
            assertNotNull(event.getTimestamp());
        }

        @Test
        void createOrderWithMatch_publishesBothCreationAndTradeEventsInBatch() {
            OrderBook realOrderBook = new OrderBook(SYMBOL);

            Order existingSellOrder = new Order(2L, SYMBOL, Side.SELL, 100.0, 150.0);
            realOrderBook.addOrder(existingSellOrder);

            Order newBuyOrder = new Order(null, SYMBOL, Side.BUY, 100.0, 150.0);

            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
            when(orderBookRegistry.getOrderBook(SYMBOL)).thenReturn(realOrderBook);

            ArgumentCaptor<List<OrderEvent>> batchCaptor = ArgumentCaptor.forClass(List.class);

            orderBookService.createOrder(SYMBOL, newBuyOrder);

            verify(eventPublisher).publishBatch(batchCaptor.capture());
            List<OrderEvent> events = batchCaptor.getValue();

            long creationEventCount = events.stream()
                    .filter(e -> e instanceof OrderCreationEvent)
                    .count();
            long tradeEventCount = events.stream()
                    .filter(e -> e instanceof TradeExecutionEvent)
                    .count();

            assertEquals(1, creationEventCount);
            assertEquals(1, tradeEventCount);
        }
    }
}