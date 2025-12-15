package org.louisjohns32.personal.exchange.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class OrderCancellationEvent implements OrderEvent {
    private final Long orderId;
    private final String symbol;
    private final long timestamp;
    private final long sequenceNumber;

    @JsonCreator
    public OrderCancellationEvent(
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("sequenceNumber") long sequenceNumber
    ) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
    }
}
