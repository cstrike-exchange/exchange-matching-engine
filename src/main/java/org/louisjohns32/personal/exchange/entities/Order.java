package org.louisjohns32.personal.exchange.entities;


import org.louisjohns32.personal.exchange.constants.Side;

import jakarta.validation.constraints.Min;


public class Order {
	private final long id;
	
	@Min(0)
	private final double quantity;
	
	@Min(0)
	private final double price;
	
	private double filledQuantity;
	
	private Side side;
	
	public double getRemainingQuantity() {
		return quantity - filledQuantity;
	}
	
	

	public Order(long id, Side side, @Min(0) double quantity, @Min(0) double price) {
		super();
		this.id = id;
		this.quantity = quantity;
		this.price = price;
		this.filledQuantity = 0;
		this.side = side;
	}
	
	
	public Order(Side side, @Min(0) double quantity, @Min(0) double price) {
		super();
		this.id=0;
		this.quantity = quantity;
		this.price = price;
		this.filledQuantity = 0;
		this.side = side;
	}

	
	public Order(long id, Order order) {
		this.id = id;
		this.quantity = order.getQuantity();
		this.price = order.getPrice();
		this.filledQuantity = order.getFilledQuantity();
		this.side = order.getSide();
	}
	

	
	
	public long getId() {
		return id;
	}


	public double getQuantity() {
		return quantity;
	}

	public double getPrice() {
		return price;
	}

	public double getFilledQuantity() {
		return filledQuantity;
	}

	public void setFilledQuantity(double filledQuantity) {
		this.filledQuantity = filledQuantity;
	}
	
	public void fill(double amnt) {
		if(amnt > getRemainingQuantity()) {
			throw new IllegalArgumentException("Fill amount exceeds remaining quantity");
		}
		filledQuantity += amnt;
	}
	
	public boolean isFilled() {
		return filledQuantity == quantity;
	}
	
	public Side getSide() {
		return side;
	}
	
}
