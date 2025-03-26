package org.louisjohns32.personal.exchange.entities;

import java.util.LinkedList;
import java.util.List;

import org.louisjohns32.personal.exchange.constants.Side;

public class OrderBookLevel {

	// contains orders
	
	private LinkedList<Order> orders;
	private double price;
	private Side side;
	// private Map<Double, LinkedListNode> ordersMap; TODO implement own linked list to allow O(1) removal, java.util.LinkedList doesn't expose the node class
	
	public OrderBookLevel(double price, Side side) {
		this.price = price;
		this.side = side;
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
	}
	
	public Order getOrder() {
		return orders.getFirst();
	}
	
	public void removeOrderById(long id) { // O(n), should be optimised in future (easy to do just have to write up linked list class)
		orders.removeIf(order ->(order.getId() == id));
	}
	
	public List<Order> getOrders() {
		return orders;
	}
	
	public boolean isEmpty() {
		return orders.isEmpty();
	}
}
