package org.louisjohns32.personal.exchange.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {
	/**
	 *
	 */
    @Serial
    private static final long serialVersionUID = 1L;
    private final Long orderId;

	public OrderNotFoundException(Long orderId) {
		this.orderId = orderId;
	}
	
	public Long getOrderId() {
		return orderId;
	}

}
