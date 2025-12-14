package org.louisjohns32.personal.exchange.common.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderCreationEvent.class, name = "ORDER_CREATED"),
        @JsonSubTypes.Type(value = TradeExecutionEvent.class, name = "TRADE_EXECUTED"),
        @JsonSubTypes.Type(value = OrderCancellationEvent.class, name = "ORDER_CANCELLED")
})
public interface OrderEvent {

    String getSymbol();
    
    long getTimestamp();
}
