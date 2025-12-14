package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.dao.OrderRepository;
import org.louisjohns32.personal.exchange.dao.TradeRepository;
import org.louisjohns32.personal.exchange.dto.OrderCancellationEvent;
import org.louisjohns32.personal.exchange.dto.OrderCreationEvent;
import org.louisjohns32.personal.exchange.dto.OrderEvent;
import org.louisjohns32.personal.exchange.dto.TradeExecutionEvent;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderStatus;
import org.louisjohns32.personal.exchange.entities.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderPersistService {

    @Value("${exchange.kafka.topics.order-events}")
    private String ordersTopic; // TODO use this

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    TradeRepository tradeRepository;


    @KafkaListener(topics = "order.events", groupId = "group_id",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(OrderEvent event) {
        System.out.println("Order event received: " + event);
        switch (event) {
            case OrderCreationEvent orderCreationEvent -> handleOrderCreation(orderCreationEvent);
            case OrderCancellationEvent orderCancellationEvent -> handleOrderCancellation(orderCancellationEvent);
            case TradeExecutionEvent tradeExecutionEvent -> handleTradeExecution(tradeExecutionEvent);
            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + event.getClass().getName()
            );
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
