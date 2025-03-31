package org.louisjohns32.personal.exchange.controllers;

import org.louisjohns32.personal.exchange.assemblers.OrderBookModelAssembler;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", produces = { MediaType.APPLICATION_JSON_VALUE})
public class OrderBookApiController {
	
	@Autowired
	private OrderBookService orderBookService;
	
	@Autowired
	private OrderBookModelAssembler orderBookAssembler;
	
	@GetMapping("/{symbol}/orderbook")
	public EntityModel<OrderBook> getOrderBook(@PathVariable String symbol) {
		return orderBookAssembler.toModel(orderBookService.getOrderBook(symbol));
	}
	
	
}
