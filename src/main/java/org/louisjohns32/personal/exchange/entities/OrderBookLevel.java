package org.louisjohns32.personal.exchange.entities;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.louisjohns32.personal.exchange.constants.Side;

/**
 * Represents a single price level in the order book.
 * Maintains FIFO queue of orders at the same price using LinkedList.
 * Thread-safe using ReentrantReadWriteLock.
 */
public class OrderBookLevel {
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	// contains orders
	
	private LinkedList<Order> orders;
	private double price;
	private Side side;
	private double volume;
	// FUTURE: Implement custom linked list for O(1) removal (currently O(n))
	
	public OrderBookLevel(double price, Side side) {
		this.price = price;
		this.side = side;
		this.volume = 0;
		orders = new LinkedList<Order>();
	}
	
	public Side getSide() {
		return side;
	}
	
	public double getPrice() {
		return price;
	}
	
	public void addOrder(Order order) {
		lock.writeLock().lock();
		try {
			orders.addLast(order);	
			volume += order.getQuantity();
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public Order getOrder() {
		lock.readLock().lock();
		try {
			return orders.getFirst();
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public double getVolume() {
		// FUTURE: Cache volume instead of recomputing (requires tracking partial fills)
		double volume = 0;
		for(Order order : orders) {
			volume += order.getRemainingQuantity();
		}
		return volume;
	}
	
	public void removeOrderById(long id) { // O(n), could be optimized with custom LinkedList
		lock.writeLock().lock();
		try {
			orders.removeIf(order ->(order.getId() == id));
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public List<Order> getOrders() {
		lock.readLock().lock();
		try {
			return Collections.unmodifiableList(orders);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public boolean isEmpty() {
		lock.readLock().lock();
		try {
			return orders.isEmpty();
		} finally {
			lock.readLock().unlock();
		}
	}
}
