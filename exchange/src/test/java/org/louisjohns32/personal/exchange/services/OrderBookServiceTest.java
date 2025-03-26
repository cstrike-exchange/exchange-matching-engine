package org.louisjohns32.personal.exchange.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.entities.OrderBookLevel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private MatchingEngineService matchingEngine;

    @Mock
    private OrderBook orderBook;

    @InjectMocks
    private OrderBookServiceImpl orderBookService;

    private Order bidOrder;
    private Order askOrder;

    @BeforeEach
    void setUp() {
        bidOrder = new Order(1, Side.BUY, 5.0, 100.0);
        askOrder = new Order(2, Side.SELL, 5.0, 99.0);
    }

    @Nested
    class CreateOrderTests {

        @Test
        void validOrder_addsToOrderBookAndMatches() {
            when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());

            Order newOrder = orderBookService.createOrder(orderBook, bidOrder);

            assertNotNull(newOrder);
            verify(orderBook, times(1)).addOrder(any(Order.class));
            verify(matchingEngine, times(1)).match(orderBook, newOrder);
        }

        @Test
        void invalidOrder_throwsConstraintViolationException() {
            ConstraintViolation<Order> violation = mock(ConstraintViolation.class);
            when(validator.validate(any(Order.class))).thenReturn(Set.of(violation));

            assertThrows(ConstraintViolationException.class, () -> orderBookService.createOrder(orderBook, bidOrder));

            verify(orderBook, never()).addOrder(any(Order.class));
            verify(matchingEngine, never()).match(any(), any());
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
            bidOrder = spy(new Order(1, Side.BUY, 5.0, 100.0));
            when(orderBook.getOrderById(1)).thenReturn(bidOrder);

            double remainingQty = orderBookService.fillOrder(orderBook, bidOrder, 3.0);

            assertEquals(2.0, remainingQty);
            verify(orderBook, never()).removeOrder(bidOrder);
        }

        @Test
        void fullFill_removesOrder() {
            bidOrder = spy(new Order(1, Side.BUY, 5.0, 100.0));
            when(orderBook.getOrderById(1)).thenReturn(bidOrder);

            double remainingQty = orderBookService.fillOrder(orderBook, bidOrder, 5.0);

            assertEquals(0.0, remainingQty);
            verify(orderBook, times(1)).removeOrder(bidOrder);
        }
    }

    @Nested
    class MatchOrderTests {

        @Nested
        class OrdersMatch {

            @Test
            void executesTrade() {
                OrderBookLevel bidLevel = mock(OrderBookLevel.class);
                OrderBookLevel askLevel = mock(OrderBookLevel.class);
                
                bidOrder = new Order(1, Side.BUY, 5.0, 100.0);
                askOrder = new Order(2, Side.SELL, 5.0, 99.0);

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
        }

        @Nested
        class NoValidTrades {

            @Test
            void doesNotExecuteTrade() {
                OrderBookLevel bidLevel = mock(OrderBookLevel.class);
                OrderBookLevel askLevel = mock(OrderBookLevel.class);

                bidOrder = new Order(1, Side.BUY, 5.0, 98.0);
                askOrder = new Order(2, Side.SELL, 5.0, 99.0);

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
        }

        @Nested
        class PartialFill {

            @Test
            void correctlyHandlesRemainingQuantity() {
                OrderBookLevel bidLevel = mock(OrderBookLevel.class);
                OrderBookLevel askLevel = mock(OrderBookLevel.class);

                bidOrder = new Order(1, Side.BUY, 5.0, 100.0);
                askOrder = new Order(2, Side.SELL, 3.0, 99.0);

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

                assertNull(orderBook.getLowestAskLevel(), "Expected lowest ask level to be null after removal.");
            }
        }
    }
}
