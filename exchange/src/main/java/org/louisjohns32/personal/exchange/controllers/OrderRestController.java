package org.louisjohns32.personal.exchange.controllers;

import java.util.Optional;

import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderRestController {
	
	@Autowired
	private OrderBookService orderBookService;

	@PostMapping
	public ResponseEntity<?> placeOrder(@RequestBody Order order) {
		throw new RuntimeException("placeOrder not implemented");
	}
	
	// cancel order
	
	@GetMapping
	public Optional<Order> getOrderBook(){
		throw new RuntimeException("getOrderBook not implemented");
	}
	
	// how should orderbook layers be represented?
	// should you be able to get a specific order?
	
}
