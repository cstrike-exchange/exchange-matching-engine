package org.louisjohns32.personal.exchange.services;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.springframework.stereotype.Component;

@Component
public class OrderBookRegistryImpl implements OrderBookRegistry {
	
	private ConcurrentHashMap<String, OrderBook> orderBookMap;
	
	public OrderBookRegistryImpl() {
		orderBookMap = new ConcurrentHashMap<String, OrderBook>();
	}

	@Override
	public OrderBook getOrderBook(String symbol) {
		return orderBookMap.get(symbol);
	}

	@Override
	public void createOrderBook(String symbol) {
		orderBookMap.putIfAbsent(symbol, new OrderBook(symbol));
	}

	@Override
	public boolean orderBookExists(String symbol) {
		return orderBookMap.containsKey(symbol);
	}

	@Override
	public List<String> getSymbols() {
		return Collections.list(orderBookMap.keys());
	}
	
}
