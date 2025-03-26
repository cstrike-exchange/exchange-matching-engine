package org.louisjohns32.personal.exchange.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.louisjohns32.personal.exchange.constants.Side;

public class OrderBook {
	
	// has levels at different price points
	
	private TreeMap<Double, OrderBookLevel> bidLevels; 
	private TreeMap<Double, OrderBookLevel> askLevels;
	
	private Map<Long, Order> orderMap; // idk if this is the best way to do this
	
	public OrderBook() {
		bidLevels = new TreeMap<Double, OrderBookLevel>();
		askLevels = new TreeMap<Double, OrderBookLevel>();
		orderMap = new HashMap<Long, Order>();
	}
	
	public void addOrder(Order order) {
		OrderBookLevel level = getLevel(order.getPrice(), order.getSide());
		if(level == null) {
			level = createLevel(order.getPrice(), order.getSide());
		}
		level.addOrder(order);
		orderMap.put(order.getId(), order);
	}
	
	public OrderBookLevel getLevel(double price, Side side) {
		if(side == Side.BUY) return bidLevels.get(price);
		return askLevels.get(price);
	}
	
	private OrderBookLevel createLevel(double price, Side side) {
		OrderBookLevel level = new OrderBookLevel(price, side);
		if(side == Side.BUY) bidLevels.put(price, level);
		else askLevels.put(price, level);
		return level;
	}
	
	public OrderBookLevel getHighestBidLevel() {
		return bidLevels.lastEntry().getValue();
	}
	
	public OrderBookLevel getLowestAskLevel() {
		return askLevels.firstEntry().getValue();
	}
	
	public Order getOrderById(long id) {
		return orderMap.get(id);
	}
	
	public void removeOrder(Order order) { // this needs to be atomic
		OrderBookLevel level = getLevel(order.getPrice(), order.getSide()); // TODO change to side
		level.removeOrderById(order.getId());
		orderMap.remove(order.getId());
		if(level.isEmpty()) removeLevel(level);
	}
	
	private void removeLevel(OrderBookLevel level) {
		if(level.getSide() == Side.BUY) bidLevels.remove(level.getPrice());
		else askLevels.remove(level.getPrice());
	}
	
}
