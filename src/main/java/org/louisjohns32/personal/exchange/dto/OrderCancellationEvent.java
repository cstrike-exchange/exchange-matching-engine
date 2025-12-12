package org.louisjohns32.personal.exchange.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.louisjohns32.personal.exchange.constants.Side;

public class OrderCancellationEvent implements OrderEvent {
    private final Long orderId;
    private final String symbol;
    private final long timestamp;

    @JsonCreator
    public OrderCancellationEvent(
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("timestamp") long timestamp
    ) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.timestamp = timestamp;
    }

    public Long getOrderId() { return orderId; }

    @Override
    public String getSymbol() { return symbol; }

    @Override
    public long getTimestamp() { return timestamp; }
}
