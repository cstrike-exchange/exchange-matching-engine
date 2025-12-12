package org.louisjohns32.personal.exchange.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.louisjohns32.personal.exchange.constants.Side;

public class OrderCreationEvent implements OrderEvent {
    private final Long orderId;
    private final String symbol;
    private final Side side;
    private final Double quantity;
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

    public Long getOrderId() { return orderId; }

    @Override
    public String getSymbol() { return symbol; }

    public Side getSide() { return side; }

    public Double getQuantity() { return quantity; }

    public Double getPrice() { return price; }

    @Override
    public long getTimestamp() { return timestamp; }
}
