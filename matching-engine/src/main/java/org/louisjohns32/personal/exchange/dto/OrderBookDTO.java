package org.louisjohns32.personal.exchange.dto;

import java.util.List;

public class OrderBookDTO {
	private final String symbol;
	private final List<OrderBookLevelDTO> bidLevels;
	private final List<OrderBookLevelDTO> askLevels;
	
	public OrderBookDTO(String symbol, List<OrderBookLevelDTO> bidLevels, List<OrderBookLevelDTO> askLevels) {
        this.symbol = symbol;
        this.bidLevels = bidLevels;
        this.askLevels = askLevels;
    }

	public String getSymbol() {
		return symbol;
	}

	public List<OrderBookLevelDTO> getBidLevels() {
		return bidLevels;
	}


	public List<OrderBookLevelDTO> getAskLevels() {
		return askLevels;
	}

}
