package org.louisjohns32.personal.exchange.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.louisjohns32.personal.exchange.common.domain.Side;

@Builder
public class OrderCreationEvent implements OrderEvent {
    @Getter
    private final Long orderId;
    private final String symbol;
    @Getter
    private final Side side;
    @Getter
    private final Double quantity;
    @Getter
    private final Double price;
    private final long timestamp;

    @JsonCreator
    public OrderCreationEvent(
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("side") Side side,
            @JsonProperty("quantity") Double quantity,
            @JsonProperty("price") Double price,
            @JsonProperty("timestamp") long timestamp) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }

    @Override
    public String getSymbol() { return symbol; }

    @Override
    public long getTimestamp() { return timestamp; }
}
