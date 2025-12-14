package org.louisjohns32.personal.exchange.services;


import org.louisjohns32.personal.exchange.common.domain.OrderStatus;
import org.louisjohns32.personal.exchange.common.events.OrderCancellationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderCreationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderEvent;
import org.louisjohns32.personal.exchange.common.events.TradeExecutionEvent;
import org.louisjohns32.personal.exchange.dao.OrderRepository;
import org.louisjohns32.personal.exchange.dao.TradeRepository;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Synchronous database write event publisher
 * Writes orders to OrderDB synchronously, adding significant latency (2+ms), and limiting throughput. This is intended
 * for MVP only, with a migration to Kafka, or WAL in phase 1.
 *
 */
//@Service
public class SyncDBEventPublisher implements EventPublisher {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    TradeRepository tradeRepository;


    @Override
    @Transactional
    public void publish(OrderEvent event) {
        switch (event) {
            case OrderCreationEvent orderCreationEvent -> handleOrderCreation(orderCreationEvent);
            case OrderCancellationEvent orderCancellationEvent -> handleOrderCancellation(orderCancellationEvent);
            case TradeExecutionEvent tradeExecutionEvent -> handleTradeExecution(tradeExecutionEvent);
            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + event.getClass().getName()
            );
        }
    }

    @Override
    @Transactional
    public void publishBatch(List<OrderEvent> events) {
        for (OrderEvent event : events) {
            publish(event);
        }
    }

    private void handleOrderCreation(OrderCreationEvent event) {
        Order order = new Order(
                event.getOrderId(),
                event.getSymbol(),
                event.getSide(),
                event.getQuantity(),
                event.getPrice()
        );

        orderRepository.save(order);
    }

    private void handleOrderCancellation(OrderCancellationEvent event) {

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + event.getOrderId()
                ));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private void handleTradeExecution(TradeExecutionEvent event) {
        Trade trade = new Trade(
                event.getSymbol(),
                event.getBuyOrderId(),
                event.getSellOrderId(),
                event.getPrice(),
                event.getQuantity(),
                event.getTimestamp()
        );

        updateOrderFromTrade(event.getBuyOrderId(), event.getQuantity());
        updateOrderFromTrade(event.getSellOrderId(), event.getQuantity());

        tradeRepository.save(trade);
    }

    private void updateOrderFromTrade(Long orderId, Double fillAmount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + orderId
                ));


        order.setFilledQuantity(order.getFilledQuantity() + fillAmount);


        if (order.isFilled()) {
            order.setStatus(OrderStatus.FILLED);
        } else if (order.getFilledQuantity() > 0) {
            order.setStatus(OrderStatus.PARTIAL);
        }

        orderRepository.save(order);
    }
}
