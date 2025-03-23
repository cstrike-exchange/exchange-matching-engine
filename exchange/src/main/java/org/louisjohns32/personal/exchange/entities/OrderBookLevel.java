package org.louisjohns32.personal.exchange.entities;

import java.util.LinkedList;

public class OrderBookLevel {

	// contains orders
	
	private LinkedList<Order> bids; // should defo be double ended linked list
	private LinkedList<Order> asks;
	
	public long numBids() {
		return bids.size();
	}
	
	public long numAsks() {
		return asks.size();
	}
	
	public void addOrder(Order order) {
		// TODO get order side
		bids.addLast(order);
	}
}
