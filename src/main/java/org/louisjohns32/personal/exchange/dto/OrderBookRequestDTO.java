package org.louisjohns32.personal.exchange.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OrderBookRequestDTO {
	
	@NotBlank(message = "Symbol must not be empty")
    @Size(min = 3, max = 10, message = "Symbol must be between 3 and 10 chars long")
    @Pattern(regexp = "^[A-Z]+$", message = "Symbol must only contain chars in the english alphabet")
	private String symbol;
	
	public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
