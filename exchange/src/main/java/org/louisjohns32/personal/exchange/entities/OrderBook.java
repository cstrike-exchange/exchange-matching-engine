package org.louisjohns32.personal.exchange.entities;

import java.util.TreeMap;
import java.util.TreeSet;

import org.louisjohns32.personal.exchange.services.OrderBookLevelService;
import org.springframework.beans.factory.annotation.Autowired;

public class OrderBook {
	
	@Autowired
	private OrderBookLevelService levelService;
	
	// has levels at different price points
	
	private TreeMap<Double, OrderBookLevel> bidLevels; 
	private TreeMap<Double, OrderBookLevel> askLevels;
	
	public void addOrder(Order order) {
		OrderBookLevel level = getLevel(order.getPrice(), true);
		if(level == null) {
			level = createLevel(order.getPrice(), true);
		}
		level.addOrder(order);
	}
	
	private OrderBookLevel getLevel(double price,/* Side side*/ boolean bid) {
		if(bid) return bidLevels.get(price);
		return askLevels.get(price);
	}
	
	private OrderBookLevel createLevel(double price,/* Side side*/ boolean bid) {
		OrderBookLevel level = new OrderBookLevel();
		if(bid) {
			bidLevels.put(price, level);
		}
		return askLevels.get(price);
	}
}
