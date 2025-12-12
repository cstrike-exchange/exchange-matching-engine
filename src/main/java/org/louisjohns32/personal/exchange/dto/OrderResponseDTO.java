package org.louisjohns32.personal.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderStatus;

import java.time.LocalDateTime;

@Value
@Builder
public class OrderResponseDTO {

    @JsonProperty("id")
    Long id;

    @JsonProperty("symbol")
    String symbol;

    @JsonProperty("side")
    Side side;

    @JsonProperty("quantity")
    Double quantity;

    @JsonProperty("price")
    Double price;

    @JsonProperty("filledQuantity")
    Double filledQuantity;

    @JsonProperty("remainingQuantity")
    Double remainingQuantity;

    @JsonProperty("status")
    OrderStatus status;

    @JsonProperty("createdAt")
    LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    LocalDateTime updatedAt;

    public static OrderResponseDTO fromEntity(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .filledQuantity(order.getFilledQuantity())
                .remainingQuantity(order.getQuantity() - order.getFilledQuantity())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}