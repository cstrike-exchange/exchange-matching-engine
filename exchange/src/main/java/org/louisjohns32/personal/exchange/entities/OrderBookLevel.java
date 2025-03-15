package org.louisjohns32.personal.exchange.entities;

import java.util.List;

public class OrderBookLevel {

	// contains orders
	
	private List<Order> bids; // should defo be double ended linked list
	private List<Order> asks;
	
	public long numBids() {
		return bids.size();
	}
	
	public long numAsks() {
		return asks.size();
	}
}
