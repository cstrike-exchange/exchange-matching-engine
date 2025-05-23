package org.louisjohns32.personal.exchange.assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import org.louisjohns32.personal.exchange.controllers.OrderBookApiController;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;


@Component
public class OrderBookModelAssembler implements RepresentationModelAssembler<OrderBook, EntityModel<OrderBook>> {

	@Override
	public EntityModel<OrderBook> toModel(OrderBook orderBook) {
		return EntityModel.of(orderBook, linkTo(methodOn(OrderBookApiController.class).getOrderBook(orderBook.getSymbol())).withSelfRel());
	}

}
