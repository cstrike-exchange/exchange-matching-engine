package org.louisjohns32.personal.exchange.dto;

public class OrderBookLevelDTO {
	private final double price;
	private final double volume;
	
	public OrderBookLevelDTO(double price, double volume) {
		this.price = price;
		this.volume = volume;
	}

	public double getPrice() {
		return price;
	}

	public double getVolume() {
		return volume;
	}
}
