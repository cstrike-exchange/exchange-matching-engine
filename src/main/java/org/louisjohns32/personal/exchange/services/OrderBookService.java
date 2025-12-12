package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.dto.OrderBookDTO;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.entities.Trade;

import java.util.List;

public interface OrderBookService {
	
	public OrderBook getOrderBook(String symbol);
	
	public OrderBookDTO getAggregatedOrderBook(String symbol);
	
	public OrderBook createOrderBook(String symbol);
	
	public Order createOrder(OrderBook orderBook, Order order);
	
	public Order createOrder(String symbol, Order order);
	 
	public void deleteOrderById(OrderBook orderBook, long id);
 
	public double fillOrder(OrderBook orderBook, Order order, double amnt); 
 
	public List<Trade> match(OrderBook orderBook, Order newOrder);

}
