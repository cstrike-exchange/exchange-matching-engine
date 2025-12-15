package org.louisjohns32.personal.exchange.orderquery.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.common.domain.OrderStatus;
import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.orderquery.dao.OrderReadRepository;
import org.louisjohns32.personal.exchange.orderquery.dto.OrderResponseDTO;
import org.louisjohns32.personal.exchange.orderquery.entity.OrderEntity;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderReadRepository repository;

    @InjectMocks
    private OrderQueryServiceImpl service;

    @Test
    void getOrder_shouldReturnDto_whenOrderExists() {
        // 1. Given (The data in the "Database")
        String orderId = "123";
        OrderEntity mockEntity = OrderEntity.builder()
                .id(123L)
                .symbol("AAPL")
                .side(Side.BUY)
                .price(150.)
                .quantity(10.5)
                .filledQuantity(0.0)
                .status(OrderStatus.OPEN)
                .build();
        when(repository.findById(123L)).thenReturn(Optional.of(mockEntity));

        OrderResponseDTO response = service.getOrder(123L);

        assertEquals(123, response.getId());
        assertEquals("AAPL", response.getSymbol());
        assertEquals(Side.BUY, response.getSide());
        assertEquals(OrderStatus.OPEN, response.getStatus());
        assertEquals(10.5, response.getQuantity());

        verify(repository).findById(123L);
    }

    @Test
    void getOrder_shouldThrow404_whenOrderDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            service.getOrder(999L);
        });

        assertEquals(404, exception.getStatusCode().value());
        verify(repository).findById(999L);
    }
}
