package org.louisjohns32.personal.exchange.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class OrderBookExceptionHandler {
	
	@ExceptionHandler(OrderBookNotFoundException.class)
	public ResponseEntity<?> orderBookNotFoundHandler(OrderBookNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(null);
	}
	
}
