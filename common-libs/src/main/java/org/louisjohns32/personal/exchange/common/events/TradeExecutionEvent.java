package org.louisjohns32.personal.exchange.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public class TradeExecutionEvent implements OrderEvent {
    private final String symbol;
    @Getter
    private final long buyOrderId;
    @Getter
    private final long sellOrderId;
    @Getter
    private final double price;
    @Getter
    private final double quantity;
    private final long timestamp;

    @JsonCreator
    public TradeExecutionEvent(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("buyOrderId") long buyOrderId,
            @JsonProperty("sellOrderId") long sellOrderId,
            @JsonProperty("price") double price,
            @JsonProperty("quantity") double quantity,
            @JsonProperty("timestamp") long timestamp ){
        this.symbol = symbol;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    @Override
    public String getSymbol() { return symbol; }

    @Override
    public long getTimestamp() { return timestamp; }
}
