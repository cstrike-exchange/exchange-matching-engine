package org.louisjohns32.personal.exchange.persist.service;

import org.louisjohns32.personal.exchange.common.domain.OrderStatus;
import org.louisjohns32.personal.exchange.common.events.OrderCancellationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderCreationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderEvent;
import org.louisjohns32.personal.exchange.common.events.TradeExecutionEvent;
import org.louisjohns32.personal.exchange.persist.dao.OrderRepository;
import org.louisjohns32.personal.exchange.persist.dao.TradeRepository;
import org.louisjohns32.personal.exchange.persist.entity.OrderEntity;
import org.louisjohns32.personal.exchange.persist.entity.TradeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPersistService {

    @Value("${exchange.kafka.topics.order-events}")
    private String ordersTopic;

    @Value("${exchange.kafka.group-id}")
    private String groupId;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    TradeRepository tradeRepository;


    @KafkaListener(topics = "${exchange.kafka.topics.order-events}", groupId = "${exchange.kafka.group-id}",
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
        OrderEntity order = new OrderEntity(
                event.getOrderId(),
                event.getSymbol(),
                event.getSide(),
                event.getQuantity(),
                event.getPrice()
        );

        orderRepository.save(order);
    }


    private void handleOrderCancellation(OrderCancellationEvent event) {

        OrderEntity order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + event.getOrderId()
                ));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private void handleTradeExecution(TradeExecutionEvent event) {
        TradeEntity trade = new TradeEntity(
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
        OrderEntity order = orderRepository.findById(orderId)
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
