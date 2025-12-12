package org.louisjohns32.personal.exchange.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.louisjohns32.personal.exchange.entities.Trade;

import java.time.ZoneOffset;

public class TradeExecutionEvent implements OrderEvent {
    private final String symbol;
    private final long buyOrderId;
    private final long sellOrderId;
    private final double price;
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

    public TradeExecutionEvent(Trade trade){
        this.symbol = trade.getSymbol();
        this.buyOrderId = trade.getBuyOrderId();
        this.sellOrderId = trade.getSellOrderId();
        this.price = trade.getPrice();
        this.quantity = trade.getQuantity();
        this.timestamp = trade.getExecutedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    @Override
    public String getSymbol() { return symbol; }
    public long getBuyOrderId() { return buyOrderId; }
    public long getSellOrderId() { return sellOrderId; }
    public double getPrice() { return price; }
    public double getQuantity() { return quantity; }
    @Override
    public long getTimestamp() { return timestamp; }
}
