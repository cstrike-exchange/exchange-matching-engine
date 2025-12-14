package org.louisjohns32.personal.exchange.services;

import java.util.List;

import org.louisjohns32.personal.exchange.entities.OrderBook;

public interface OrderBookRegistry {
	
	public OrderBook getOrderBook(String symbol);
	
	public void createOrderBook(String symbol);
	
	public boolean orderBookExists(String symbol);
	
	public List<String> getSymbols();
}
