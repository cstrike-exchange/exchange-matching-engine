package org.louisjohns32.personal.exchange.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.common.events.OrderCancellationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderRestEvent;
import org.louisjohns32.personal.exchange.common.events.TradeExecutionEvent;
import org.louisjohns32.personal.exchange.marketdata.core.OrderBookState;
import org.louisjohns32.personal.exchange.marketdata.model.OrderBookDelta;
import org.louisjohns32.personal.exchange.marketdata.model.OrderBookSnapshot;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderBookStateTest {

    private OrderBookState orderBookState;
    private long seq;

    @BeforeEach
    void setUp() {
        orderBookState = new OrderBookState("GOOG");
        seq = 1;
    }

    @Test
    void restOrder_addsBidLevel() {
        orderBookState.applyEvent(restBuy(100, 50));
        orderBookState.flushDelta();

        OrderBookSnapshot snapshot = orderBookState.getSnapshot();

        assertThat(snapshot.getBidLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && l.getVolume().compareTo(BigDecimal.valueOf(50)) == 0);
    }

    @Test
    void multipleRestOrders_aggregateAtSamePrice() {
        orderBookState.applyEvent(restBuy(100, 50));
        orderBookState.applyEvent(restBuy(100, 20));
        orderBookState.flushDelta();

        OrderBookSnapshot snapshot = orderBookState.getSnapshot();

        assertThat(snapshot.getBidLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && l.getVolume().compareTo(BigDecimal.valueOf(70)) == 0);
    }

    @Test
    void tradeExecution_reducesLevelVolume() {
        orderBookState.applyEvent(restBuy(100, 50));
        orderBookState.applyEvent(tradeBuy(100, 30));
        orderBookState.flushDelta();

        OrderBookSnapshot snapshot = orderBookState.getSnapshot();

        assertThat(snapshot.getBidLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && l.getVolume().compareTo(BigDecimal.valueOf(20)) == 0);
    }

    @Test
    void tradeExecution_removesLevelWhenFullyFilled() {
        orderBookState.applyEvent(restBuy(100, 50));
        orderBookState.applyEvent(tradeBuy(100, 50));
        orderBookState.flushDelta();

        OrderBookSnapshot snapshot = orderBookState.getSnapshot();

        assertThat(snapshot.getBidLevels())
                .noneMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0);
    }

    @Test
    void cancellation_reducesLevelVolume() {
        orderBookState.applyEvent(restBuy(100, 50));
        orderBookState.applyEvent(cancelBuy(100, 20));
        orderBookState.flushDelta();

        OrderBookSnapshot snapshot = orderBookState.getSnapshot();

        assertThat(snapshot.getBidLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && l.getVolume().compareTo(BigDecimal.valueOf(30)) == 0);
    }

    @Test
    void cancellation_removesLevelWhenFullyCancelled() {
        orderBookState.applyEvent(restBuy(100, 50));
        orderBookState.applyEvent(cancelBuy(100, 50));
        orderBookState.flushDelta();

        OrderBookSnapshot snapshot = orderBookState.getSnapshot();

        assertThat(snapshot.getBidLevels())
                .noneMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0);
    }

    @Test
    void flushDelta_returnsOnlyChangesAndResets() {
        orderBookState.applyEvent(restBuy(100, 50));

        OrderBookDelta delta1 = orderBookState.flushDelta();
        OrderBookDelta delta2 = orderBookState.flushDelta();

        assertThat(delta1.getLevelDeltas()).hasSize(1);
        assertThat(delta2.getLevelDeltas()).isEmpty();
    }
    @Test
    void bidAndAskLevelsAreIsolated() {
        orderBookState.applyEvent(restBuy(100, 50));
        orderBookState.applyEvent(restSell(101, 40));
        orderBookState.flushDelta();

        OrderBookSnapshot snapshot = orderBookState.getSnapshot();

        assertThat(snapshot.getBidLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0);

        assertThat(snapshot.getAskLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(101)) == 0);
    }

    private OrderRestEvent restBuy(double price, double qty) {
        return OrderRestEvent.builder()
                .orderId(1L)
                .symbol("GOOG")
                .side(Side.BUY)
                .price(price)
                .quantity(qty)
                .sequenceNumber(seq++)
                .build();
    }

    private OrderRestEvent restSell(double price, double qty) {
        return OrderRestEvent.builder()
                .orderId(2L)
                .symbol("GOOG")
                .side(Side.SELL)
                .price(price)
                .quantity(qty)
                .sequenceNumber(seq++)
                .build();
    }

    private TradeExecutionEvent tradeBuy(double price, double qty) {
        return TradeExecutionEvent.builder()
                .symbol("GOOG")
                .makerSide(Side.BUY)
                .price(price)
                .quantity(qty)
                .sequenceNumber(seq++)
                .build();
    }

    private OrderCancellationEvent cancelBuy(double price, double qty) {
        return OrderCancellationEvent.builder()
                .orderId(1L)
                .symbol("GOOG")
                .side(Side.BUY)
                .price(price)
                .remainingQuantity(qty)
                .sequenceNumber(seq++)
                .build();
    }
}
