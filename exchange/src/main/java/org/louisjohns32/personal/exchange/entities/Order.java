package org.louisjohns32.personal.exchange.entities;


import jakarta.validation.constraints.Min;


public class Order {
	private final long id;
	
	@Min(0)
	private final double quantity;
	
	@Min(0)
	private final double price;
	
	private double filledQuantity;
	
	public double getRemainingQuantity() {
		return quantity - filledQuantity;
	}
	
	

	public Order(long id, @Min(0) double quantity, @Min(0) double price, double filledQuantity) {
		super();
		this.id = 0;
		this.quantity = quantity;
		this.price = price;
		this.filledQuantity = filledQuantity;
	}



	public Order(long id, Order order) {
		this.id = id;
		this.quantity = order.getQuantity();
		this.price = order.getPrice();
		this.filledQuantity = order.getFilledQuantity();
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

	public void setFilledQuantity(long filledQuantity) {
		this.filledQuantity = filledQuantity;
	}
	
	
	
	
	
	
}
