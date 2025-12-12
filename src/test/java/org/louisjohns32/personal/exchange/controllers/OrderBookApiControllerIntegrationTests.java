package org.louisjohns32.personal.exchange.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.dao.OrderRepository;
import org.louisjohns32.personal.exchange.dao.TradeRepository;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.services.OrderBookRegistry;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrderBookApiControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderBookService orderBookService;

    @Autowired
    private OrderBookRegistry registry;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        tradeRepository.deleteAll();
    }

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
    class OrderPersistenceTests {
        @Test
        void orderIsSavedToDatabase() throws Exception {
            String symbol = "AAPL";
            registry.createOrderBook(symbol);

            // Create order
            Order createdOrder = orderBookService.createOrder(
                    symbol,
                    new Order(null, symbol, Side.BUY, 100.0, 150.0)
            );

            // Verify it's in database
            Order dbOrder = orderRepository.findById(createdOrder.getId())
                    .orElseThrow(() -> new AssertionError("Order not found in database"));

            assert dbOrder.getSymbol().equals("AAPL");
            assert dbOrder.getSide() == Side.BUY;
            assert dbOrder.getQuantity().equals(100.0);
            assert dbOrder.getPrice().equals(150.0);
        }

        @Test
        void tradeIsSavedToDatabase() throws Exception {
            String symbol = "AAPL";
            registry.createOrderBook(symbol);

            // Create matching orders
            orderBookService.createOrder(symbol, new Order(null, symbol, Side.BUY, 50.0, 150.0));
            orderBookService.createOrder(symbol, new Order(null, symbol, Side.SELL, 50.0, 150.0));

            // Verify trade was saved
            var trades = tradeRepository.findAll();
            assert trades.size() == 1;
            assert trades.get(0).getQuantity().equals(50.0);
            assert trades.get(0).getPrice().equals(150.0);
        }

        @Test
        void orderStatusUpdatedAfterTrade() throws Exception {
            String symbol = "AAPL";
            registry.createOrderBook(symbol);

            // Create orders
            Order buyOrder = orderBookService.createOrder(
                    symbol,
                    new Order(null, symbol, Side.BUY, 100.0, 150.0)
            );

            orderBookService.createOrder(
                    symbol,
                    new Order(null, symbol, Side.SELL, 100.0, 150.0)
            );

            // Verify order status updated in database
            Order dbOrder = orderRepository.findById(buyOrder.getId()).orElseThrow();
            assert dbOrder.getStatus().toString().equals("FILLED");
            assert dbOrder.getFilledQuantity().equals(100.0);
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

            mockMvc.perform(get("/api/orders/{orderId}", createdOrder.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(createdOrder.getId()))
                    .andExpect(jsonPath("$.symbol").value("AAPL"))
                    .andExpect(jsonPath("$.side").value("BUY"))
                    .andExpect(jsonPath("$.quantity").value(100.0))
                    .andExpect(jsonPath("$.price").value(150.50))
                    .andExpect(jsonPath("$.filledQuantity").value(0.0))
                    .andExpect(jsonPath("$.remainingQuantity").value(100.0))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.createdAt").exists());
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

            mockMvc.perform(get("/api/orders/{orderId}", buyOrder.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(buyOrder.getId()))
                    .andExpect(jsonPath("$.quantity").value(100.0))
                    .andExpect(jsonPath("$.filledQuantity").value(30.0))
                    .andExpect(jsonPath("$.remainingQuantity").value(70.0))
                    .andExpect(jsonPath("$.status").value("PARTIAL"));
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

            mockMvc.perform(get("/api/orders/{orderId}", buyOrder.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(buyOrder.getId()))
                    .andExpect(jsonPath("$.quantity").value(50.0))
                    .andExpect(jsonPath("$.filledQuantity").value(50.0))
                    .andExpect(jsonPath("$.remainingQuantity").value(0.0))
                    .andExpect(jsonPath("$.status").value("FILLED"));
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

            mockMvc.perform(get("/api/orders/{orderId}", buyOrder.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.filledQuantity").value(45.0))
                    .andExpect(jsonPath("$.remainingQuantity").value(55.0))
                    .andExpect(jsonPath("$.status").value("PARTIAL"));
        }
    }

}