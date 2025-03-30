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
	
	public OrderBook() {
		bidLevels = new ConcurrentSkipListMap<Double, OrderBookLevel>();
		askLevels = new ConcurrentSkipListMap<Double, OrderBookLevel>();
		orderMap = new ConcurrentHashMap<Long, Order>();
	}
	
	public void addOrder(Order order) {
		levelLock.writeLock().lock();
		try {
			OrderBookLevel level = getLevel(order.getPrice(), order.getSide());
			if(level == null) {
				level = createLevel(order.getPrice(), order.getSide());
			}
			level.addOrder(order);
		} finally {
			levelLock.writeLock().unlock();
		}
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
		levelLock.writeLock().lock();
		try {
			OrderBookLevel level = getLevel(order.getPrice(), order.getSide());
			level.removeOrderById(order.getId());
			orderMap.remove(order.getId());
			if(level.isEmpty()) removeLevel(level);
		} finally {
			levelLock.writeLock().unlock();
		}
	}
	
	private void removeLevel(OrderBookLevel level) {
		if(level.getSide() == Side.BUY) bidLevels.remove(level.getPrice());
		else askLevels.remove(level.getPrice());
	}
	
}
