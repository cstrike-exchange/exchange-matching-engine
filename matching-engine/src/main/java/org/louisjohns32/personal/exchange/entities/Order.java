package org.louisjohns32.personal.exchange.entities;


import lombok.Getter;
import lombok.Setter;
import org.louisjohns32.personal.exchange.common.domain.OrderStatus;
import org.louisjohns32.personal.exchange.common.domain.Side;

import java.time.LocalDateTime;

@Getter
@Setter
public class Order {

    private Long id;

    private String symbol;

    private Side side;

    private Double quantity;

    private Double price;

    private Double filledQuantity;

    private OrderStatus status = OrderStatus.OPEN;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    protected Order() {
    }


    public Order(Long id, String symbol, Side side, Double quantity, Double price) {
        this.id = id;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.filledQuantity = 0.0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    public Order(Long id, Order order) {
        this.id = id;
        this.symbol = order.getSymbol();
        this.side = order.getSide();
        this.quantity = order.getQuantity();
        this.price = order.getPrice();
        this.filledQuantity = order.getFilledQuantity();
        this.status = order.getStatus();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    public Order(String symbol, Side side, Double quantity, Double price) {
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.filledQuantity = 0.0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Double getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    public void fill(Double amount) {
        if (amount > getRemainingQuantity()) {
            throw new IllegalArgumentException("Fill amount exceeds remaining quantity");
        }
        this.filledQuantity += amount;
        this.updatedAt = LocalDateTime.now();

        // Update status based on fill
        if (isFilled()) {
            this.status = OrderStatus.FILLED;
        } else if (filledQuantity > 0) {
            this.status = OrderStatus.PARTIAL;
        }
    }

    public boolean isFilled() {
        return filledQuantity.equals(quantity);
    }
}