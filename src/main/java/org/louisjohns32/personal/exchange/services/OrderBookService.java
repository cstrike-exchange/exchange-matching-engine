package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.dto.OrderBookDTO;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;

public interface OrderBookService {
	
	public OrderBook getOrderBook(String symbol);
	
	public OrderBookDTO getAggregatedOrderBook(String symbol);
	
	public OrderBook createOrderBook(String symbol); // idk about storing orderbooks in this service
	
	public Order createOrder(OrderBook orderBook, Order order);
	
	public Order createOrder(String symbol, Order order);
	 
	public void deleteOrderById(OrderBook orderBook, long id);
 
	public double fillOrder(OrderBook orderBook, Order order, double amnt); 
 
	public void match(OrderBook orderBook, Order newOrder);
	 
	 
	 
	// create order
	
	// match ?
	
	// how to do matching engine?
}
