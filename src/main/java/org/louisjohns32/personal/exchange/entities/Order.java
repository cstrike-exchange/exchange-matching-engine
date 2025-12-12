package org.louisjohns32.personal.exchange.entities;

import org.louisjohns32.personal.exchange.constants.Side;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_symbol_status", columnList = "symbol, status"),
        @Index(name = "idx_symbol_side_price", columnList = "symbol, side, price"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class Order {

    @Id
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Side side;

    @Min(0)
    @Column(nullable = false)
    private Double quantity;

    @Min(0)
    @Column(nullable = false)
    private Double price;

    @Column(name = "filled_quantity", nullable = false)
    private Double filledQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
        this.status = OrderStatus.OPEN;
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
        this.status = OrderStatus.OPEN;
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


    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== GETTERS ==========

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public Side getSide() {
        return side;
    }

    public Double getQuantity() {
        return quantity;
    }

    public Double getPrice() {
        return price;
    }

    public Double getFilledQuantity() {
        return filledQuantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ========== SETTERS ==========


    public void setId(Long id) {
        this.id = id;
    }


    public void setFilledQuantity(Double filledQuantity) {
        this.filledQuantity = filledQuantity;
    }


    public void setStatus(OrderStatus status) {
        this.status = status;
    }



    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}