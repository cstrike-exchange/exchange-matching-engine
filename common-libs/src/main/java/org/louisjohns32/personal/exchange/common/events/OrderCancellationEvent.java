package org.louisjohns32.personal.exchange.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public class OrderCancellationEvent implements OrderEvent {
    @Getter
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

    @Override
    public String getSymbol() { return symbol; }

    @Override
    public long getTimestamp() { return timestamp; }
}
