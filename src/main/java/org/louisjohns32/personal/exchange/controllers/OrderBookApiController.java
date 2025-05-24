package org.louisjohns32.personal.exchange.controllers;

import java.util.Map;
import java.util.stream.Collectors;

import org.louisjohns32.personal.exchange.assemblers.OrderBookModelAssembler;
import org.louisjohns32.personal.exchange.dto.OrderBookDTO;
import org.louisjohns32.personal.exchange.dto.OrderBookRequestDTO;
import org.louisjohns32.personal.exchange.dto.OrderRequestDTO;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api", produces = { MediaType.APPLICATION_JSON_VALUE})
public class OrderBookApiController {
	
	@Autowired
	private OrderBookService orderBookService;
	
	@Autowired
	private OrderBookModelAssembler orderBookAssembler;
	
	@GetMapping("/orderbook/{symbol}")
	public OrderBookDTO getOrderBook(@PathVariable String symbol) {
		return orderBookService.getAggregatedOrderBook(symbol);
	}
	
	@PostMapping("/orderbook/{symbol}") 
	public ResponseEntity<?> createOrder(@RequestBody @Valid OrderRequestDTO orderRequest, BindingResult result) {
		if(result.hasErrors()) {
			Map<String, String> errors = result.getFieldErrors().stream()
		            .collect(Collectors.toMap(
		                FieldError::getField,
		                FieldError::getDefaultMessage,
		                (existing, replacement) -> existing
		            ));
			return ResponseEntity.badRequest().body(errors);
		}
		
		/*
		Order order = orderBookService.createOrder();
		EntityModel<OrderBook> obEntityModel = orderBookAssembler.toModel(ob);
		return ResponseEntity.created(obEntityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
		*/
	}
	
	
	@PostMapping("/orderbook")
	public ResponseEntity<?> createOrderBook(@RequestBody @Valid OrderBookRequestDTO orderBookRequest, BindingResult result) {
		
		if(result.hasErrors()) {
			Map<String, String> errors = result.getFieldErrors().stream()
		            .collect(Collectors.toMap(
		                FieldError::getField,
		                FieldError::getDefaultMessage,
		                (existing, replacement) -> existing
		            ));
			return ResponseEntity.badRequest().body(errors);
		}
		
		OrderBook ob = orderBookService.createOrderBook(orderBookRequest.getSymbol());
		EntityModel<OrderBook> obEntityModel = orderBookAssembler.toModel(ob);
		return ResponseEntity.created(obEntityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
	}
	
	
}
