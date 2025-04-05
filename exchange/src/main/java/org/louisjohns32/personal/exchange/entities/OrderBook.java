package org.louisjohns32.personal.exchange.entities;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.louisjohns32.personal.exchange.constants.Side;

public class OrderBook {
	
	private final ReentrantReadWriteLock levelLock = new ReentrantReadWriteLock();
	
	private ConcurrentSkipListMap<Double, OrderBookLevel> bidLevels; 
	private ConcurrentSkipListMap<Double, OrderBookLevel> askLevels;
	
	private Map<Long, Order> orderMap; 
	
	private final String symbol;
	
	public OrderBook(String symbol) {
		this.symbol = symbol;
		bidLevels = new ConcurrentSkipListMap<Double, OrderBookLevel>();
		askLevels = new ConcurrentSkipListMap<Double, OrderBookLevel>();
		orderMap = new ConcurrentHashMap<Long, Order>();
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	public void addOrder(Order order) {
		OrderBookLevel level;
		levelLock.writeLock().lock();
		try {
			level = getLevel(order.getPrice(), order.getSide());
			if(level == null) {
				level = createLevel(order.getPrice(), order.getSide());
			}
		} finally {
			levelLock.writeLock().unlock();
		}
		level.addOrder(order);
		orderMap.put(order.getId(), order);
	}
	
	public OrderBookLevel getLevel(double price, Side side) {
		if(side == Side.BUY) return bidLevels.get(price);
		return askLevels.get(price);
	}
	
	private OrderBookLevel createLevel(double price, Side side) {
		levelLock.writeLock().lock();
		try {
			OrderBookLevel level = new OrderBookLevel(price, side);
			if(side == Side.BUY) bidLevels.put(price, level);
			else askLevels.put(price, level);
			return level;
		} finally {
			levelLock.writeLock().unlock();
		}
	}
	
	public OrderBookLevel getHighestBidLevel() {
		Entry<Double, OrderBookLevel> entry = bidLevels.lastEntry();
		if(entry == null) return null;
		return entry.getValue();
	}
	
	public OrderBookLevel getLowestAskLevel() {
		Entry<Double, OrderBookLevel> entry = askLevels.firstEntry();
		if(entry == null) return null;
		return entry.getValue();
	}
	
	public Order getOrderById(long id) {
		return orderMap.get(id);
	}
	
	public void removeOrder(Order order) { 
		OrderBookLevel level = getLevel(order.getPrice(), order.getSide());
		level.removeOrderById(order.getId());
		orderMap.remove(order.getId());
		if(level.isEmpty()) removeLevel(level);
	}
	
	public Map<Double, OrderBookLevel> getBidLevels() {
		return bidLevels;
	}
	
	public Map<Double, OrderBookLevel> getAskLevels() {
		return askLevels;
	}

	
	private void removeLevel(OrderBookLevel level) {
		if(level.getSide() == Side.BUY) bidLevels.remove(level.getPrice());
		else askLevels.remove(level.getPrice());
	}
	
}
