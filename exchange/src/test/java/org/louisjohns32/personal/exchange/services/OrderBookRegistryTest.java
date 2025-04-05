package org.louisjohns32.personal.exchange.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.springframework.beans.factory.annotation.Autowired;

public class OrderBookRegistryTest {
	
	@Autowired
	private OrderBookRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new OrderBookRegistryImpl();
    }

    @Test
    void testCreateOrderBook_registersNewOrderBook() {
        String symbol = "BTCUSD";
        assertFalse(registry.orderBookExists(symbol));

        registry.createOrderBook(symbol);

        assertTrue(registry.orderBookExists(symbol));
        OrderBook ob = registry.getOrderBook(symbol);
        assertNotNull(ob);
        assertEquals(symbol, ob.getSymbol());
    }

    @Test
    void testCreateOrderBook_doesNotOverwriteExisting() {
        String symbol = "ETHUSD";

        registry.createOrderBook(symbol);
        OrderBook first = registry.getOrderBook(symbol);

        registry.createOrderBook(symbol); 
        OrderBook second = registry.getOrderBook(symbol);

        assertSame(first, second, "OrderBook should not be overwritten if it already exists");
    }

    @Test
    void testGetOrderBook_returnsNullIfNotExists() {
        assertNull(registry.getOrderBook("NONEXISTENT"));
    }

    @Test
    void testOrderBookExists_returnsTrueIfExists() {
        String symbol = "AAPL";
        registry.createOrderBook(symbol);
        assertTrue(registry.orderBookExists(symbol));
    }

    @Test
    void testOrderBookExists_returnsFalseIfNotExists() {
        assertFalse(registry.orderBookExists("FAKE"));
    }

    @Test
    void testGetSymbols_returnsAllRegisteredSymbols() {
        registry.createOrderBook("BTC");
        registry.createOrderBook("ETH");
        registry.createOrderBook("AMZN");

        List<String> symbols = registry.getSymbols();

        assertEquals(3, symbols.size());
        assertTrue(symbols.contains("BTC"));
        assertTrue(symbols.contains("ETH"));
        assertTrue(symbols.contains("AMZN"));
    }
}
