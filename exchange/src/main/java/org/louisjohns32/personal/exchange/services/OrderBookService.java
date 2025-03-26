package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;

public interface OrderBookService {
	
	 public Order createOrder(OrderBook orderBook, Order order);
	 
	 public void deleteOrderById(OrderBook orderBook, long id);
	 
	 public double fillOrder(OrderBook orderBook, Order order, double amnt); 
	 
	public void match(OrderBook orderBook, Order newOrder);
	 
	 
	 
	// create order
	
	// match ?
	
	// how to do matching engine?
}
