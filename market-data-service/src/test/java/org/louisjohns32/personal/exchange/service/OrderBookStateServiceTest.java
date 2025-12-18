package org.louisjohns32.personal.exchange.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.common.events.OrderCancellationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderRestEvent;
import org.louisjohns32.personal.exchange.common.events.TradeExecutionEvent;
import org.louisjohns32.personal.exchange.marketdata.model.OrderBookSnapshot;
import org.louisjohns32.personal.exchange.marketdata.service.OrderBookRegistry;
import org.louisjohns32.personal.exchange.marketdata.service.OrderBookStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = {OrderBookStateService.class, OrderBookRegistry.class})
class OrderBookStateServiceTest {

    @Autowired
    private OrderBookStateService service;

    private long seq;

    @BeforeEach
    void setUp() {
        seq = 1;
    }

    private OrderBookSnapshot flushAndGetSnapshot(String symbol) {
        service.flushDelta(symbol);
        return service.getOrderBookSnapshot(symbol);
    }

    @Test
    void getOrderBookSnapshot_createsStateIfNotExists() {
        String symbol = "AAPL";
        OrderBookSnapshot snapshot = flushAndGetSnapshot(symbol);
        assertThat(snapshot).isNotNull();
    }

    @Test
    void applyEvent_createsStateAndAppliesEvent() {
        String symbol = "GOOG";

        OrderRestEvent rest = OrderRestEvent.builder()
                .orderId(1L)
                .symbol(symbol)
                .side(Side.BUY)
                .price(100.)
                .quantity(50.)
                .sequenceNumber(seq++)
                .build();

        service.applyEvent(rest);

        OrderBookSnapshot snapshot = flushAndGetSnapshot(symbol);
        assertThat(snapshot.getBidLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && l.getVolume().compareTo(BigDecimal.valueOf(50)) == 0);
    }

    @Test
    void multipleSymbols_areIndependent() {
        String symbol1 = "MSFT";
        String symbol2 = "TSLA";

        service.applyEvent(OrderRestEvent.builder()
                .orderId(1L)
                .symbol(symbol1)
                .side(Side.BUY)
                .price(100.)
                .quantity(10.)
                .sequenceNumber(seq++)
                .build());
        service.applyEvent(OrderRestEvent.builder()
                .orderId(2L)
                .symbol(symbol2)
                .side(Side.BUY)
                .price(200.)
                .quantity(20.)
                .sequenceNumber(seq++)
                .build());

        OrderBookSnapshot snapshot1 = flushAndGetSnapshot(symbol1);
        OrderBookSnapshot snapshot2 = flushAndGetSnapshot(symbol2);

        assertThat(snapshot1.getBidLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && l.getVolume().compareTo(BigDecimal.valueOf(10)) == 0);
        assertThat(snapshot2.getBidLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(200)) == 0
                        && l.getVolume().compareTo(BigDecimal.valueOf(20)) == 0);
    }

    @Test
    void applyMultipleEvents_updatesSnapshotCorrectly() {
        String symbol = "AMZN";

        service.applyEvent(OrderRestEvent.builder()
                .orderId(1L)
                .symbol(symbol)
                .side(Side.BUY)
                .price(50.)
                .quantity(5.)
                .sequenceNumber(seq++)
                .build());
        service.applyEvent(OrderRestEvent.builder()
                .orderId(2L)
                .symbol(symbol)
                .side(Side.BUY)
                .price(50.)
                .quantity(10.)
                .sequenceNumber(seq++)
                .build());

        OrderBookSnapshot snapshot = flushAndGetSnapshot(symbol);
        assertThat(snapshot.getBidLevels())
                .anyMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(50)) == 0
                        && l.getVolume().compareTo(BigDecimal.valueOf(15)) == 0);
    }

    @Test
    void sequenceNumber_incrementsCorrectly() {
        String symbol = "NFLX";
        service.applyEvent(OrderRestEvent.builder()
                .orderId(1L)
                .symbol(symbol)
                .side(Side.BUY)
                .price(10.)
                .quantity(1.)
                .sequenceNumber(100)
                .build());
        service.applyEvent(OrderRestEvent.builder()
                .orderId(2L)
                .symbol(symbol)
                .side(Side.BUY)
                .price(10.)
                .quantity(1.)
                .sequenceNumber(105)
                .build());
        OrderBookSnapshot snapshot = flushAndGetSnapshot(symbol);

        assertThat(snapshot.getSequenceNumber()).isEqualTo(105);
    }
    @Test
    void flushDelta_returnsCorrectDeltasAndResets() {
        String symbol = "FB";

        service.applyEvent(OrderRestEvent.builder()
                .orderId(1L)
                .symbol(symbol)
                .side(Side.BUY)
                .price(100.)
                .quantity(50.)
                .sequenceNumber(seq++)
                .build());
        service.applyEvent(OrderRestEvent.builder()
                .orderId(2L)
                .symbol(symbol)
                .side(Side.SELL)
                .price(105.)
                .quantity(30.)
                .sequenceNumber(seq++)
                .build());

        var delta = service.flushDelta(symbol);

        assertThat(delta.getLevelDeltas()).hasSize(2);
        assertThat(delta.getLevelDeltas())
                .anyMatch(l -> l.getSide() == Side.BUY
                        && l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && l.getVolumeDifference().compareTo(BigDecimal.valueOf(50)) == 0);
        assertThat(delta.getLevelDeltas())
                .anyMatch(l -> l.getSide() == Side.SELL
                        && l.getPrice().compareTo(BigDecimal.valueOf(105)) == 0
                        && l.getVolumeDifference().compareTo(BigDecimal.valueOf(30)) == 0);

        var deltaAfterReset = service.flushDelta(symbol);
        assertThat(deltaAfterReset.getLevelDeltas()).isEmpty();
    }
    @Test
    void tradeExecution_reducesDeltaVolume() {
        String symbol = "AAPL";

        service.applyEvent(OrderRestEvent.builder()
                .orderId(1L)
                .symbol(symbol)
                .side(Side.BUY)
                .price(100.)
                .quantity(50.)
                .sequenceNumber(seq++)
                .build());

        service.applyEvent(TradeExecutionEvent.builder()
                .symbol(symbol)
                .makerSide(Side.BUY)
                .price(100.)
                .quantity(30.)
                .sequenceNumber(seq++)
                .build());

        var delta = service.flushDelta(symbol);

        assertThat(delta.getLevelDeltas()).hasSize(1);

        assertThat(delta.getLevelDeltas())
                .anyMatch(l -> l.getSide() == Side.BUY
                        && l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && l.getVolumeDifference().compareTo(BigDecimal.valueOf(20)) == 0);

        assertThat(service.flushDelta(symbol).getLevelDeltas()).isEmpty();
    }

    @Test
    void orderCancellation_reducesDeltaVolume() {
        String symbol = "TSLA";

        service.applyEvent(OrderRestEvent.builder()
                .orderId(1L)
                .symbol(symbol)
                .side(Side.SELL)
                .price(150.)
                .quantity(40.)
                .sequenceNumber(seq++)
                .build());

        service.applyEvent(OrderCancellationEvent.builder()
                .orderId(1L)
                .symbol(symbol)
                .side(Side.SELL)
                .price(150.)
                .remainingQuantity(20.)
                .sequenceNumber(seq++)
                .build());

        var delta = service.flushDelta(symbol);

        assertThat(delta.getLevelDeltas()).hasSize(1);

        assertThat(delta.getLevelDeltas())
                .anyMatch(l -> l.getSide() == Side.SELL
                        && l.getPrice().compareTo(BigDecimal.valueOf(150)) == 0
                        && l.getVolumeDifference().compareTo(BigDecimal.valueOf(20)) == 0);


        assertThat(service.flushDelta(symbol).getLevelDeltas()).isEmpty();
    }

    @Test
    void fullyFilledLevel_removesDeltaAndLevel() {
        String symbol = "AMZN";

        service.applyEvent(OrderRestEvent.builder()
                .orderId(1L)
                .symbol(symbol)
                .side(Side.BUY)
                .price(200.)
                .quantity(50.)
                .sequenceNumber(seq++)
                .build());

        service.applyEvent(TradeExecutionEvent.builder()
                .symbol(symbol)
                .makerSide(Side.BUY)
                .price(200.)
                .quantity(50.)
                .sequenceNumber(seq++)
                .build());

        var delta = service.flushDelta(symbol);

        assertThat(delta.getLevelDeltas()).hasSize(1);

        var snapshot = service.getOrderBookSnapshot(symbol);
        assertThat(snapshot.getBidLevels())
                .noneMatch(l -> l.getPrice().compareTo(BigDecimal.valueOf(200)) == 0);
    }

    @Test
    void mixedBuySellDeltas_areCorrectlySeparated() {
        String symbol = "NFLX";

        service.applyEvent(OrderRestEvent.builder()
                .orderId(1L)
                .symbol(symbol)
                .side(Side.BUY)
                .price(100.)
                .quantity(10.)
                .sequenceNumber(seq++)
                .build());

        service.applyEvent(OrderRestEvent.builder()
                .orderId(2L)
                .symbol(symbol)
                .side(Side.SELL)
                .price(105.)
                .quantity(15.)
                .sequenceNumber(seq++)
                .build());

        var delta = service.flushDelta(symbol);

        assertThat(delta.getLevelDeltas()).hasSize(2);

        assertThat(delta.getLevelDeltas())
                .anyMatch(l -> l.getSide() == Side.BUY
                        && l.getPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && l.getVolumeDifference().compareTo(BigDecimal.valueOf(10)) == 0);

        assertThat(delta.getLevelDeltas())
                .anyMatch(l -> l.getSide() == Side.SELL
                        && l.getPrice().compareTo(BigDecimal.valueOf(105)) == 0
                        && l.getVolumeDifference().compareTo(BigDecimal.valueOf(15)) == 0);
    }

}