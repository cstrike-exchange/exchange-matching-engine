package org.louisjohns32.personal.exchange.entities;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.louisjohns32.personal.exchange.constants.Side;

/**
 * Represents a single price level in the order book.
 * Maintains FIFO queue of orders at the same price using LinkedList.
 * Must be single-threaded
 */
public class OrderBookLevel {

	// contains orders
	
	private final LinkedList<Order> orders;
	private final double price;
	private final Side side;
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
        orders.addLast(order);
        volume += order.getQuantity();
	}
	
	public Order getOrder() {
        return orders.getFirst();
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
        orders.removeIf(order ->(order.getId() == id));
	}
	
	public List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
	}
	
	public boolean isEmpty() {
        return orders.isEmpty();
	}
}
