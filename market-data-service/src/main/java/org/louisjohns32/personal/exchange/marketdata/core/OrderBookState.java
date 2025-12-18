package org.louisjohns32.personal.exchange.marketdata.core;

import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.common.events.OrderCancellationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderEvent;
import org.louisjohns32.personal.exchange.common.events.OrderRestEvent;
import org.louisjohns32.personal.exchange.common.events.TradeExecutionEvent;
import org.louisjohns32.personal.exchange.marketdata.model.LevelDelta;
import org.louisjohns32.personal.exchange.marketdata.model.LevelSnapshot;
import org.louisjohns32.personal.exchange.marketdata.model.OrderBookDelta;
import org.louisjohns32.personal.exchange.marketdata.model.OrderBookSnapshot;

import java.math.BigDecimal;
import java.util.*;

public class OrderBookState {




    private final String symbol;

    // maintain most recent snapshot
    private OrderBookSnapshot recentSnapshot;

    // maintain deltas
    private final Map<Side, Map<BigDecimal, BigDecimal>> pendingDeltas = new EnumMap<>(Side.class);

    private final TreeMap<BigDecimal, BigDecimal> bidLevels = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<BigDecimal, BigDecimal> askLevels = new TreeMap<>();


    private long currentSequenceNumber = 0;


    // TODO need to construct this state from a snapshot
    public OrderBookState(String symbol) {
        pendingDeltas.put(Side.BUY, new HashMap<>());
        pendingDeltas.put(Side.SELL, new HashMap<>());
        this.symbol = symbol;
    }


    public OrderBookSnapshot getSnapshot() {
        return recentSnapshot;
    }

    // consume OrderEvent, update state and deltas

    // flush delta - returns current delta and resets delta and updates snapshot
    public OrderBookDelta flushDelta() {
        List<LevelDelta> deltas = new ArrayList<>();

        for (var sideEntry : pendingDeltas.entrySet()) {
            Side side = sideEntry.getKey();
            for (var levelEntry : sideEntry.getValue().entrySet()) {
                BigDecimal price = levelEntry.getKey();
                BigDecimal deltaQty = levelEntry.getValue();

                if (!deltaQty.equals(BigDecimal.ZERO)) {
                    deltas.add(new LevelDelta(side, price, deltaQty));
                }
            }
            sideEntry.getValue().clear();
        }

        OrderBookDelta delta = OrderBookDelta.builder()
                .levelDeltas(deltas)
                .timestamp(System.currentTimeMillis())
                .sequenceNumber(currentSequenceNumber)
                .symbol(symbol)
                .build();

        updateSnapshot();
        return delta;
    }


    public synchronized void applyEvent(OrderEvent orderEvent) {
        // TODO what to do if we get out of order events?
        switch (orderEvent) {
            case OrderRestEvent orderRestEvent -> handleOrderRest(orderRestEvent);
            case TradeExecutionEvent tradeExecutionEvent -> handleTradeExecution(tradeExecutionEvent);
            case OrderCancellationEvent orderCancellationEvent -> handleOrderCancellation(orderCancellationEvent);
            default -> {}
        }
        currentSequenceNumber = Math.max(currentSequenceNumber, orderEvent.getSequenceNumber());
    }

    private synchronized void updateSnapshot() {
        recentSnapshot = OrderBookSnapshot.builder()
                .askLevels(toLevelSnapshots(askLevels))
                .bidLevels(toLevelSnapshots(bidLevels))
                .sequenceNumber(currentSequenceNumber)
                .build();
    }


    private void updateLevel(Side side, BigDecimal price, BigDecimal quantityChange) {
        Map<BigDecimal, BigDecimal> levelMap = getSideMap(side);

        levelMap.merge(price, quantityChange, BigDecimal::add);
        BigDecimal newVolume = levelMap.get(price);
        if (newVolume == null || newVolume.compareTo(BigDecimal.ZERO) <= 0) {
            levelMap.remove(price);
        }

        recordDelta(side, price, quantityChange);
    }

    private void handleOrderRest(OrderRestEvent orderRestEvent) {
        updateLevel(
                orderRestEvent.getSide(),
                BigDecimal.valueOf(orderRestEvent.getPrice()),
                BigDecimal.valueOf(orderRestEvent.getQuantity())
        );
    }

    private void handleTradeExecution(TradeExecutionEvent tradeExecutionEvent) {
        updateLevel(
                tradeExecutionEvent.getMakerSide(),
                BigDecimal.valueOf(tradeExecutionEvent.getPrice()),
                BigDecimal.valueOf(-tradeExecutionEvent.getQuantity())
        );
    }

    private void handleOrderCancellation(OrderCancellationEvent orderCancellationEvent) {
        updateLevel(
                orderCancellationEvent.getSide(),
                BigDecimal.valueOf(orderCancellationEvent.getPrice()),
                BigDecimal.valueOf(-orderCancellationEvent.getRemainingQuantity())
        );
    }


    private List<LevelSnapshot> toLevelSnapshots(TreeMap<BigDecimal, BigDecimal> levelMap) {
        List<LevelSnapshot> levelSnapshots = new ArrayList<>();
        for  (var levelEntry : levelMap.entrySet()) {
            levelSnapshots.add(LevelSnapshot.builder()
                    .volume(levelEntry.getValue())
                    .price(levelEntry.getKey())
                    .build()
            );
        }
        return levelSnapshots;
    }

    private Map<BigDecimal, BigDecimal> getSideMap(Side side) {
        return side == Side.BUY ? bidLevels : askLevels;
    }
    private void recordDelta(Side side, BigDecimal price, BigDecimal quantityDelta) {
        pendingDeltas.get(side).merge(price, quantityDelta, BigDecimal::add);
    }
}
