package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;

public interface MatchingEngineService {
	
	public void match(OrderBook orderBook, Order newOrder);
	
}
