package org.louisjohns32.personal.exchange.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_symbol_executed", columnList = "symbol, executed_at")
})
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(name = "buy_order_id", nullable = false)
    private Long buyOrderId;

    @Column(name = "sell_order_id", nullable = false)
    private Long sellOrderId;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Double quantity;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt = LocalDateTime.now();

    // Constructors, getters, setters

    public Trade() {}

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public Long getBuyOrderId() {
        return buyOrderId;
    }

    public Long getSellOrderId() {
        return sellOrderId;
    }

    public Double getPrice() {
        return price;
    }

    public Double getQuantity() {
        return quantity;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public Trade(String symbol, Long buyOrderId, Long sellOrderId,
                 Double price, Double quantity, Long timestamp) {
        this.symbol = symbol;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.executedAt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneOffset.UTC
        );
    }
}