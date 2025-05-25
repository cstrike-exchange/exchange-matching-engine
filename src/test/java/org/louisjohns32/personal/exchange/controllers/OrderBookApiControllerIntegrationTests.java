package org.louisjohns32.personal.exchange.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.services.OrderBookRegistry;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderBookApiControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderBookService orderBookService;
    
    @Autowired
    private OrderBookRegistry registry;
    
    @BeforeEach
    void setUp() {
    	 ReflectionTestUtils.setField(orderBookService, "snsTopicArn", "arn");
    }

    @Test
    public void getOrderBook_returnsCorrectLevels() throws Exception {
        String symbol = "AAPL";
        registry.createOrderBook(symbol);

        orderBookService.createOrder(symbol, new Order(1L, Side.BUY, 1.0, 150.0));
        orderBookService.createOrder(symbol, new Order(1L, Side.BUY, 1.0, 150.0));
        orderBookService.createOrder(symbol, new Order(1L, Side.BUY, 2.0, 149.0));
        orderBookService.createOrder(symbol, new Order(2L, Side.SELL, 1.0, 151.0));

        mockMvc.perform(get("/api/orderbook/{symbol}", symbol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value(symbol))
                .andExpect(jsonPath("$.bidLevels", hasSize(2)))
                .andExpect(jsonPath("$.askLevels", hasSize(1)))
                .andExpect(jsonPath("$.bidLevels[0].price").value(150.0))
                .andExpect(jsonPath("$.bidLevels[1].price").value(149.0))
                .andExpect(jsonPath("$.askLevels[0].price").value(151.0));
    }
    
    @Test
    public void getOrderBook_orderBookNotFound() throws Exception {
        String symbol = "SYMBOL";

        mockMvc.perform(get("/api/orderbook/{symbol}", symbol))
                .andExpect(status().isNotFound());
    }
}
