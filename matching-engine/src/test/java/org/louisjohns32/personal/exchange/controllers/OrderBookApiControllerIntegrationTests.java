package org.louisjohns32.personal.exchange.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.services.IdGenerator;
import org.louisjohns32.personal.exchange.services.OrderBookRegistry;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.louisjohns32.personal.exchange.services.OrderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Disabled
public class OrderBookApiControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderBookService orderBookService;

    @Autowired
    private OrderBookRegistry registry;

    @Autowired
    private IdGenerator<Long> idGenerator;

    @Autowired
    private OrderQueryService orderQueryService;

    @Nested
    class GetOrderBookTests {
        @Test
        void getOrderBook_returnsCorrectLevels() throws Exception {
            String symbol = "AAPL";
            registry.createOrderBook(symbol);

            // Create orders
            orderBookService.createOrder(symbol, new Order(null, symbol, Side.BUY, 1.0, 150.0));
            orderBookService.createOrder(symbol, new Order(null, symbol, Side.BUY, 1.0, 150.0));
            orderBookService.createOrder(symbol, new Order(null, symbol, Side.BUY, 2.0, 149.0));
            orderBookService.createOrder(symbol, new Order(null, symbol, Side.SELL, 1.0, 151.0));

            // Verify order book structure
            mockMvc.perform(get("/api/orderbook/{symbol}", symbol))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbol").value(symbol))
                    .andExpect(jsonPath("$.bidLevels", hasSize(2)))
                    .andExpect(jsonPath("$.askLevels", hasSize(1)))
                    .andExpect(jsonPath("$.bidLevels[0].price").value(150.0))
                    .andExpect(jsonPath("$.bidLevels[0].volume").value(2.0))
                    .andExpect(jsonPath("$.bidLevels[1].price").value(149.0))
                    .andExpect(jsonPath("$.bidLevels[1].volume").value(2.0))
                    .andExpect(jsonPath("$.askLevels[0].price").value(151.0))
                    .andExpect(jsonPath("$.askLevels[0].volume").value(1.0));
        }

        @Test
        void getOrderBook_orderBookNotFound() throws Exception {
            String symbol = "NONEXISTENT";

            mockMvc.perform(get("/api/orderbook/{symbol}", symbol))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getOrderBook_emptyOrderBook() throws Exception {
            String symbol = "EMPTY";
            registry.createOrderBook(symbol);

            mockMvc.perform(get("/api/orderbook/{symbol}", symbol))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbol").value(symbol))
                    .andExpect(jsonPath("$.bidLevels", hasSize(0)))
                    .andExpect(jsonPath("$.askLevels", hasSize(0)));
        }
    }

    @Nested
    class MultipleSymbolTests {
        @Test
        void ordersForDifferentSymbolsAreIsolated() throws Exception {
            // Create order books
            registry.createOrderBook("AAPL");
            registry.createOrderBook("GOOGL");

            // Create orders for AAPL
            orderBookService.createOrder("AAPL", new Order(null, "AAPL", Side.BUY, 100.0, 150.0));
            orderBookService.createOrder("AAPL", new Order(null, "AAPL", Side.SELL, 100.0, 151.0));

            // Create orders for GOOGL
            orderBookService.createOrder("GOOGL", new Order(null, "GOOGL", Side.BUY, 50.0, 2800.0));
            orderBookService.createOrder("GOOGL", new Order(null, "GOOGL", Side.SELL, 50.0, 2801.0));

            // Verify AAPL order book
            mockMvc.perform(get("/api/orderbook/{symbol}", "AAPL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bidLevels", hasSize(1)))
                    .andExpect(jsonPath("$.askLevels", hasSize(1)))
                    .andExpect(jsonPath("$.bidLevels[0].price").value(150.0))
                    .andExpect(jsonPath("$.askLevels[0].price").value(151.0));

            // Verify GOOGL order book
            mockMvc.perform(get("/api/orderbook/{symbol}", "GOOGL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bidLevels", hasSize(1)))
                    .andExpect(jsonPath("$.askLevels", hasSize(1)))
                    .andExpect(jsonPath("$.bidLevels[0].price").value(2800.0))
                    .andExpect(jsonPath("$.askLevels[0].price").value(2801.0));
        }
    }

    @Nested
    class GetOrderTests {
        @Test
        void getOrder_returnsOrderDetails() throws Exception {
            String symbol = "AAPL";
            registry.createOrderBook(symbol);

            Order createdOrder = orderBookService.createOrder(
                    symbol,
                    new Order(null, symbol, Side.BUY, 100.0, 150.50)
            );

            await().atMost(ofSeconds(5))
                    .pollInterval(ofSeconds(1))
                    .untilAsserted(() -> {
                        mockMvc.perform(get("/api/orders/{orderId}", createdOrder.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(createdOrder.getId()))
                                .andExpect(jsonPath("$.status").value("OPEN"))
                                .andExpect(jsonPath("$.createdAt").exists());
                    });
        }

        @Test
        void getOrder_partiallyFilledOrder() throws Exception {
            String symbol = "AAPL";
            registry.createOrderBook(symbol);

            Order buyOrder = orderBookService.createOrder(
                    symbol,
                    new Order(null, symbol, Side.BUY, 100.0, 150.0)
            );

            orderBookService.createOrder(
                    symbol,
                    new Order(null, symbol, Side.SELL, 30.0, 150.0)
            );

            await().atMost(ofSeconds(5))
                    .pollInterval(ofSeconds(1))
                    .untilAsserted(() -> {
                        mockMvc.perform(get("/api/orders/{orderId}", buyOrder.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(buyOrder.getId()))
                                .andExpect(jsonPath("$.quantity").value(100.0))
                                .andExpect(jsonPath("$.filledQuantity").value(30.0))
                                .andExpect(jsonPath("$.remainingQuantity").value(70.0))
                                .andExpect(jsonPath("$.status").value("PARTIAL"));
                    });
        }

        @Test
        void getOrder_fullyFilledOrder() throws Exception {
            String symbol = "AAPL";
            registry.createOrderBook(symbol);

            Order buyOrder = orderBookService.createOrder(
                    symbol,
                    new Order(null, symbol, Side.BUY, 50.0, 150.0)
            );

            orderBookService.createOrder(
                    symbol,
                    new Order(null, symbol, Side.SELL, 50.0, 150.0)
            );

            await().atMost(ofSeconds(5))
                    .pollInterval(ofSeconds(1))
                    .untilAsserted(() -> {
                        mockMvc.perform(get("/api/orders/{orderId}", buyOrder.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(buyOrder.getId()))
                                .andExpect(jsonPath("$.quantity").value(50.0))
                                .andExpect(jsonPath("$.filledQuantity").value(50.0))
                                .andExpect(jsonPath("$.remainingQuantity").value(0.0))
                                .andExpect(jsonPath("$.status").value("FILLED"));
                    });
        }

        @Test
        void getOrder_notFound() throws Exception {
            mockMvc.perform(get("/api/orders/{orderId}", 999999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getOrder_multiplePartialFills() throws Exception {
            String symbol = "AAPL";
            registry.createOrderBook(symbol);

            Order buyOrder = orderBookService.createOrder(
                    symbol,
                    new Order(null, symbol, Side.BUY, 100.0, 150.0)
            );

            orderBookService.createOrder(symbol, new Order(null, symbol, Side.SELL, 10.0, 150.0));
            orderBookService.createOrder(symbol, new Order(null, symbol, Side.SELL, 20.0, 150.0));
            orderBookService.createOrder(symbol, new Order(null, symbol, Side.SELL, 15.0, 150.0));

            await().atMost(ofSeconds(5))
                    .pollInterval(ofSeconds(1))
                    .untilAsserted(() -> {
                        mockMvc.perform(get("/api/orders/{orderId}", buyOrder.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.filledQuantity").value(45.0))
                                .andExpect(jsonPath("$.remainingQuantity").value(55.0))
                                .andExpect(jsonPath("$.status").value("PARTIAL"));
                    });
        }
    }

    @Nested
    class PostOrderTests {

        private static final String SYMBOL = "AAPL";

        @BeforeEach
        void setUpOrderBook() {
            registry.createOrderBook(SYMBOL);
        }

        @Test
        void postOrder_createsNewOrder_returnsCreated() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.symbol").value(SYMBOL))
                    .andExpect(jsonPath("$.side").value("BUY"))
                    .andExpect(jsonPath("$.quantity").value(100.0))
                    .andExpect(jsonPath("$.price").value(150.0))
                    .andExpect(jsonPath("$.filledQuantity").value(0.0))
                    .andExpect(jsonPath("$.remainingQuantity").value(100.0))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        void postOrder_sellOrder_createsSuccessfully() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "SELL",
            "quantity": 50.0,
            "price": 151.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.side").value("SELL"))
                    .andExpect(jsonPath("$.quantity").value(50.0))
                    .andExpect(jsonPath("$.price").value(151.0))
                    .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        void postOrder_canBeRetrievedById() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            String response = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long orderId = extractOrderIdFromResponse(response);
            Thread.sleep(50);

            await().atMost(ofSeconds(5))
                    .pollInterval(ofSeconds(1))
                    .untilAsserted(() -> {
                        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(orderId))
                                .andExpect(jsonPath("$.symbol").value(SYMBOL))
                                .andExpect(jsonPath("$.side").value("BUY"))
                                .andExpect(jsonPath("$.quantity").value(100.0))
                                .andExpect(jsonPath("$.price").value(150.0));
                    });
        }

        @Test
        void postOrder_appearsInOrderBook() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated());

            Thread.sleep(50);

            mockMvc.perform(get("/api/orderbook/{symbol}", SYMBOL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bidLevels", hasSize(1)))
                    .andExpect(jsonPath("$.bidLevels[0].price").value(150.0))
                    .andExpect(jsonPath("$.bidLevels[0].volume").value(100.0));
        }

        @Test
        void postOrder_fullMatch_returnsFilled() throws Exception {
            orderBookService.createOrder(SYMBOL,
                    new Order(null, SYMBOL, Side.SELL, 100.0, 150.0));

            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.quantity").value(100.0))
                    .andExpect(jsonPath("$.filledQuantity").value(100.0))
                    .andExpect(jsonPath("$.remainingQuantity").value(0.0))
                    .andExpect(jsonPath("$.status").value("FILLED"));
        }

        @Test
        void postOrder_partialMatch_returnsPartial() throws Exception {
            orderBookService.createOrder(SYMBOL,
                    new Order(null, SYMBOL, Side.SELL, 50.0, 150.0));

            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.quantity").value(100.0))
                    .andExpect(jsonPath("$.filledQuantity").value(50.0))
                    .andExpect(jsonPath("$.remainingQuantity").value(50.0))
                    .andExpect(jsonPath("$.status").value("PARTIAL"));
        }

        @Test
        void postOrder_multiplePartialMatches() throws Exception {
            orderBookService.createOrder(SYMBOL,
                    new Order(null, SYMBOL, Side.SELL, 30.0, 150.0));
            orderBookService.createOrder(SYMBOL,
                    new Order(null, SYMBOL, Side.SELL, 20.0, 150.0));
            orderBookService.createOrder(SYMBOL,
                    new Order(null, SYMBOL, Side.SELL, 25.0, 150.0));

            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.quantity").value(100.0))
                    .andExpect(jsonPath("$.filledQuantity").value(75.0))
                    .andExpect(jsonPath("$.remainingQuantity").value(25.0))
                    .andExpect(jsonPath("$.status").value("PARTIAL"));
        }

        @Test
        void postOrder_noMatch_remainsOpen() throws Exception {
            orderBookService.createOrder(SYMBOL,
                    new Order(null, SYMBOL, Side.SELL, 100.0, 160.0));

            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.filledQuantity").value(0.0))
                    .andExpect(jsonPath("$.remainingQuantity").value(100.0))
                    .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        void postOrder_missingField_symbol_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_missingField_side_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_missingField_quantity_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_missingField_price_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_negativeQuantity_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": -100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_negativePrice_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": -150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_zeroQuantity_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 0.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_zeroPrice_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 0.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_invalidSide_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "INVALID",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_malformedJson_returnsBadRequest() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void postOrder_symbolNotFound_returnsNotFound() throws Exception {
            String orderJson = """
        {
            "symbol": "ABCD",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        void postOrder_multipleOrders_uniqueIds() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            String response1 = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String response2 = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long id1 = extractOrderIdFromResponse(response1);
            Long id2 = extractOrderIdFromResponse(response2);

            assert !id1.equals(id2);
            assert id2 > id1;
        }

        @Test
        void postOrder_decimalQuantityAndPrice() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 123.456,
            "price": 150.789
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.quantity").value(123.456))
                    .andExpect(jsonPath("$.price").value(150.789));
        }

        @Test
        void postOrder_veryLargeQuantity() throws Exception {
            String orderJson = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 1000000.0,
            "price": 150.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.quantity").value(1000000.0));
        }

        @Test
        void postOrder_multipleSymbols_isolated() throws Exception {
            String symbol1 = "AAPL";
            String symbol2 = "GOOGL";

            registry.createOrderBook(symbol2);

            String orderJson1 = """
        {
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 100.0,
            "price": 150.0
        }
        """;

            String orderJson2 = """
        {
            "symbol": "GOOGL",
            "side": "BUY",
            "quantity": 50.0,
            "price": 2800.0
        }
        """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson1))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.symbol").value(symbol1));

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson2))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.symbol").value(symbol2));

            Thread.sleep(50);

            mockMvc.perform(get("/api/orderbook/{symbol}", symbol1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bidLevels[0].price").value(150.0))
                    .andExpect(jsonPath("$.bidLevels[0].volume").value(100.0));

            mockMvc.perform(get("/api/orderbook/{symbol}", symbol2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bidLevels[0].price").value(2800.0))
                    .andExpect(jsonPath("$.bidLevels[0].volume").value(50.0));
        }

        private Long extractOrderIdFromResponse(String jsonResponse) throws Exception {
            return new ObjectMapper()
                    .readTree(jsonResponse)
                    .get("id")
                    .asLong();
        }
    }

}