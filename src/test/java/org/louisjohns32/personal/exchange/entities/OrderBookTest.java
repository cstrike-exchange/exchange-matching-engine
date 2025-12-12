package org.louisjohns32.personal.exchange.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.constants.Side;

public class OrderBookTest {

    private OrderBook orderBook;
    private static final String SYMBOL = "SYMBOL";

    @BeforeEach
    public void setup() {
        orderBook = new OrderBook(SYMBOL);
    }

    @Nested
    public class AddOrderTests {
        @Test
        public void addValidBid() {
            Order inputOrder = new Order(1L, SYMBOL, Side.BUY, 2.5, 13.3);
            orderBook.addOrder(inputOrder);
            assertThat(orderBook.getOrderById(1)).isEqualTo(inputOrder);
        }

        @Test
        public void addValidAsk() {
            Order inputOrder = new Order(1L, SYMBOL, Side.SELL, 2.5, 13.3);
            orderBook.addOrder(inputOrder);
            assertThat(orderBook.getOrderById(1)).isEqualTo(inputOrder);
        }

        @Test
        public void addBidUpdatesHighestBidLevel() {
            Order bid = new Order(1L, SYMBOL, Side.BUY, 2.5, 13.3);
            orderBook.addOrder(bid);
            assertThat(orderBook.getHighestBidLevel().getOrder()).isEqualTo(bid);
        }

        @Test
        public void addAskUpdatesLowestAskLevel() {
            Order ask = new Order(1L, SYMBOL, Side.SELL, 2.5, 13.3);
            orderBook.addOrder(ask);
            assertThat(orderBook.getLowestAskLevel().getOrder()).isEqualTo(ask);
        }

        @Test
        public void addMultipleOrdersSamePriceLevel() {
            Order bid1 = new Order(1L, SYMBOL, Side.BUY, 2.5, 13.3);
            Order bid2 = new Order(2L, SYMBOL, Side.BUY, 1.0, 13.3);
            orderBook.addOrder(bid1);
            orderBook.addOrder(bid2);

            OrderBookLevel bidLevel = orderBook.getHighestBidLevel();
            assertThat(bidLevel.getOrders()).containsExactly(bid1, bid2);
        }

        @Test
        public void addMultipleOrdersDifferentPriceLevels() {
            Order bid1 = new Order(1L, SYMBOL, Side.BUY, 2.5, 13.3);
            Order bid2 = new Order(2L, SYMBOL, Side.BUY, 1.0, 14.0);
            orderBook.addOrder(bid1);
            orderBook.addOrder(bid2);

            assertThat(orderBook.getHighestBidLevel().getPrice()).isEqualTo(14.0);
            assertThat(orderBook.getHighestBidLevel().getOrder()).isEqualTo(bid2);
        }
    }

    @Nested
    public class RemoveOrderTests {
        @Test
        public void removeExistingOrder() {
            Order bid = new Order(1L, SYMBOL, Side.BUY, 2.5, 13.3);
            orderBook.addOrder(bid);
            orderBook.removeOrder(bid);
            assertThat(orderBook.getOrderById(1)).isNull();
        }

        @Test
        public void removeOrderUpdatesBidLevels() {
            Order bid1 = new Order(1L, SYMBOL, Side.BUY, 2.5, 13.3);
            Order bid2 = new Order(2L, SYMBOL, Side.BUY, 1.0, 14.0);
            orderBook.addOrder(bid1);
            orderBook.addOrder(bid2);

            orderBook.removeOrder(bid2);
            assertThat(orderBook.getHighestBidLevel().getOrder()).isEqualTo(bid1);
        }

        @Test
        public void removeOrderUpdatesAskLevels() {
            Order ask1 = new Order(1L, SYMBOL, Side.SELL, 2.5, 12.0);
            Order ask2 = new Order(2L, SYMBOL, Side.SELL, 1.0, 13.3);
            orderBook.addOrder(ask1);
            orderBook.addOrder(ask2);

            orderBook.removeOrder(ask1);
            assertThat(orderBook.getLowestAskLevel().getOrder()).isEqualTo(ask2);
        }
    }

    @Nested
    public class GetOrderTests {
        @Test
        public void getOrderByIdReturnsCorrectOrder() {
            Order bid = new Order(1L, SYMBOL, Side.BUY, 2.5, 13.3);
            orderBook.addOrder(bid);
            assertThat(orderBook.getOrderById(1)).isEqualTo(bid);
        }

        @Test
        public void getOrderByIdReturnsNullForNonExistentOrder() {
            assertThat(orderBook.getOrderById(99)).isNull();
        }
    }
}