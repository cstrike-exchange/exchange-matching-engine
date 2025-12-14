package org.louisjohns32.personal.exchange.controllers;

import java.util.Map;
import java.util.stream.Collectors;

import org.louisjohns32.personal.exchange.assemblers.OrderBookModelAssembler;
import org.louisjohns32.personal.exchange.dto.OrderBookDTO;
import org.louisjohns32.personal.exchange.dto.OrderBookRequestDTO;
import org.louisjohns32.personal.exchange.dto.OrderRequestDTO;
import org.louisjohns32.personal.exchange.dto.OrderResponseDTO;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.mappers.OrderMapper;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.louisjohns32.personal.exchange.services.OrderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api", produces = { MediaType.APPLICATION_JSON_VALUE})
public class OrderBookApiController {
	
	@Autowired
	private OrderBookService orderBookService;
	
	@Autowired
	private OrderBookModelAssembler orderBookAssembler;
	
	@Autowired
	private OrderMapper orderMapper;

    @Autowired
    private OrderQueryService orderQueryService;
	
	@GetMapping("/orderbook/{symbol}")
	public OrderBookDTO getOrderBook(@PathVariable String symbol) {
		return orderBookService.getAggregatedOrderBook(symbol);
	}
	
	@PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
	public OrderResponseDTO createOrder(@RequestBody @Valid OrderRequestDTO orderRequest) {
		Order createdOrder = orderBookService.createOrder(orderRequest.getSymbol(), orderMapper.toEntity(orderRequest));
        return OrderResponseDTO.fromEntity(createdOrder);
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

    @GetMapping("/orders/{orderId}")
    public OrderResponseDTO getOrder(@PathVariable Long orderId) {
        return orderQueryService.getOrder(orderId);
    }
}
