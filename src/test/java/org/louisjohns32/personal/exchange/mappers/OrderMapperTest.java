package org.louisjohns32.personal.exchange.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.dto.OrderRequestDTO;
import org.louisjohns32.personal.exchange.entities.Order;

public class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toEntity_shouldMapAllFieldsCorrectly() {
        OrderRequestDTO dto = new OrderRequestDTO(100.0, 200.0, Side.BUY, "AAPL");

        Order order = mapper.toEntity(dto);

        assertNull(order.getId());
        assertEquals(dto.getQuantity(), order.getQuantity());
        assertEquals(dto.getPrice(), order.getPrice());
        assertEquals(dto.getSide(), order.getSide());
        assertEquals(0.0, order.getFilledQuantity());
    }

    @Test
    void toEntity_shouldHandleSELLSideCorrectly() {
        OrderRequestDTO dto = new OrderRequestDTO(10.0, 500.0, Side.SELL, "GOOG");
        Order order = mapper.toEntity(dto);

        assertEquals(Side.SELL, order.getSide());
        assertEquals(10.0, order.getQuantity());
        assertEquals(500.0, order.getPrice());
    }
}