package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;

public interface OrderBookService {
	
	 public Order createOrder(OrderBook orderBook, Order order);
	 
	 public void deleteOrderById(long id);
	 
	 public void fillOrder(); // ?
	 
	 
	 
	 
	// create order
	
	// match ?
	
	// how to do matching engine?
}
