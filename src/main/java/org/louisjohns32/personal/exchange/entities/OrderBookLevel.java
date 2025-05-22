package org.louisjohns32.personal.exchange.entities;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.louisjohns32.personal.exchange.constants.Side;

public class OrderBookLevel {
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	// contains orders
	
	private LinkedList<Order> orders;
	private double price;
	private Side side;
	private double volume;
	// private Map<Double, LinkedListNode> ordersMap; TODO implement own linked list to allow O(1) removal, java.util.LinkedList doesn't expose the node class
	
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
		// TODO return this.volume instead of iterating over orders 
		double volume = 0;
		for(Order order : orders) {
			volume += order.getRemainingQuantity();
		}
		return volume;
	}
	
	public void removeOrderById(long id) { // O(n), should be optimised in future (easy to do just have to write up linked list class)
		lock.writeLock().lock();
		try {
			// TODO reduce volume 
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
