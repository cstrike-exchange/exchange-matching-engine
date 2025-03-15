package org.louisjohns32.personal.exchange.entities;

public class Order {
	private long quantity;
	private long price;
	private long filledQuantity;
	
	public long getRemainingQuantity() {
		return quantity - filledQuantity;
	}
	
	public void fill(long quantity) throws IllegalArgumentException {
		if(quantity > getRemainingQuantity()) {
			throw new IllegalArgumentException(String.format(
					"Tried to fill %d units, but order has only %d remaining.", quantity, getRemainingQuantity()));
		}
		
		filledQuantity += quantity; // TODO how should order book level be updated?
	}
}
