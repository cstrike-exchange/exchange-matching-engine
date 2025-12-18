package org.louisjohns32.personal.exchange.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.louisjohns32.personal.exchange.common.domain.Side;

@Builder
@Getter
public final class OrderCreationEvent implements OrderEvent {
    private final Long orderId;
    private final String symbol;
    private final Side side;
    private final Double quantity;
    private final Double price;
    private final long timestamp;
    private final long sequenceNumber;

    @JsonCreator
    public OrderCreationEvent(
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("side") Side side,
            @JsonProperty("quantity") Double quantity,
            @JsonProperty("price") Double price,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("sequenceNumber") long sequenceNumber
    ) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }
}
