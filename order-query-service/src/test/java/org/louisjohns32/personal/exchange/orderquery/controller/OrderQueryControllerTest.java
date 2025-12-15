package org.louisjohns32.personal.exchange.orderquery.controller;

import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.common.domain.OrderStatus;
import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.orderquery.dto.OrderResponseDTO;
import org.louisjohns32.personal.exchange.orderquery.service.OrderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderQueryController.class)
class OrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderQueryService service;

    @Test
    void getOrder_shouldReturn200AndJson_whenOrderExists() throws Exception {
        OrderResponseDTO mockResponse = OrderResponseDTO.builder()
                .id(123L)
                .symbol("AAPL")
                .side(Side.BUY)
                .price(150.)
                .quantity(10.)
                .status(OrderStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(service.getOrder(123L)).thenReturn(mockResponse);

        // 2. When & Then: Verify HTTP response
        mockMvc.perform(get("/api/orders/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.price").value(150.00));
    }

    @Test
    void getOrder_shouldReturn404_whenServiceThrowsException() throws Exception {
        // No change here, but included for completeness
        when(service.getOrder(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }
}
