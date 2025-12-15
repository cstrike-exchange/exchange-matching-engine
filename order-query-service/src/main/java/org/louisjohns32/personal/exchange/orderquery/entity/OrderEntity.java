package org.louisjohns32.personal.exchange.orderquery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.louisjohns32.personal.exchange.common.domain.OrderStatus;
import org.louisjohns32.personal.exchange.common.domain.Side;

import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "orders")
@Builder
@AllArgsConstructor
public class OrderEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Side side;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double price;

    @Column(name = "filled_quantity", nullable = false)
    private Double filledQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected OrderEntity() {
    }
}