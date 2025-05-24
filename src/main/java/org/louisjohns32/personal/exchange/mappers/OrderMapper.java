package org.louisjohns32.personal.exchange.mappers;

import org.louisjohns32.personal.exchange.dto.OrderRequestDTO;
import org.louisjohns32.personal.exchange.entities.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequestDTO dto) {
        return new Order(dto.getSide(), dto.getQuantity(), dto.getPrice());
    }
}