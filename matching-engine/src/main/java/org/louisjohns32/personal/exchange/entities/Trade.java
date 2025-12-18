package org.louisjohns32.personal.exchange.entities;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.louisjohns32.personal.exchange.common.domain.Side;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class Trade {

    private Long id;

    private String symbol;

    private Long buyOrderId;

    private Long sellOrderId;

    private Double price;

    private Double quantity;

    private Side makerSide;

    private LocalDateTime executedAt = LocalDateTime.now();

    public Trade() {}

    public Trade(String symbol, Long buyOrderId, Long sellOrderId,
                 Double price, Double quantity, Long timestamp, Side makerSide) {
        this.symbol = symbol;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.executedAt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneOffset.UTC
        );
        this.makerSide = makerSide;
    }
}