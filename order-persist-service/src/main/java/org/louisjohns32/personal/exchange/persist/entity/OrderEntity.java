package org.louisjohns32.personal.exchange.persist.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.louisjohns32.personal.exchange.common.domain.OrderStatus;
import org.louisjohns32.personal.exchange.common.domain.Side;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_symbol_status", columnList = "symbol, status"),
        @Index(name = "idx_symbol_side_price", columnList = "symbol, side, price"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class OrderEntity {

    @Setter
    @Id
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Side side;

    @Min(0)
    @Column(nullable = false)
    private Double quantity;

    @Min(0)
    @Column(nullable = false)
    private Double price;

    @Setter
    @Column(name = "filled_quantity", nullable = false)
    private Double filledQuantity;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    protected OrderEntity() {
    }


    public OrderEntity(Long id, String symbol, Side side, Double quantity, Double price) {
        this.id = id;
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


    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
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
