package org.louisjohns32.personal.exchange.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.louisjohns32.personal.exchange.common.domain.Side;

@Builder
@Getter
public final class TradeExecutionEvent implements OrderEvent {
    private final String symbol;
    private final long buyOrderId;
    private final long sellOrderId;
    private final double price;
    private final double quantity;
    private final long timestamp;
    private final long sequenceNumber;
    private final Side makerSide;

    @JsonCreator
    public TradeExecutionEvent(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("buyOrderId") long buyOrderId,
            @JsonProperty("sellOrderId") long sellOrderId,
            @JsonProperty("price") double price,
            @JsonProperty("quantity") double quantity,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("sequenceNumber") long sequenceNumber,
            @JsonProperty("makerSide") Side makerSide){
        this.symbol = symbol;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
        this.makerSide = makerSide;
    }

    @Override
    public String getSymbol() { return symbol; }

    @Override
    public long getSequenceNumber() { return sequenceNumber; }

}
