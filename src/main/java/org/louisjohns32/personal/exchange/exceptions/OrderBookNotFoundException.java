package org.louisjohns32.personal.exchange.exceptions;

public class OrderBookNotFoundException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3574342887935396934L;
	private String symbol;
	
	public OrderBookNotFoundException(String symbol) {
		this.symbol = symbol;
	}
	
	public String getSymbol() {
		return symbol;
	}

}
