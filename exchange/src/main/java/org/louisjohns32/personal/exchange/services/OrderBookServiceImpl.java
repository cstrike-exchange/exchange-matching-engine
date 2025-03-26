package org.louisjohns32.personal.exchange.services;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class OrderBookServiceImpl implements OrderBookService {
	
	private static final AtomicLong idGenerator = new AtomicLong(1);
	
	@Autowired
	private Validator validator;
	
	@Autowired
	private MatchingEngineService matchingEngine;

	@Override
	@Transactional
	public Order createOrder(OrderBook orderBook, Order order) {
		Set<ConstraintViolation<Order>> violations = validator.validate(order);
		if(!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
		
		long orderId = idGenerator.getAndIncrement();
		Order newOrder = new Order(orderId, order);
		
		orderBook.addOrder(newOrder);
		
		matchingEngine.match(orderBook, newOrder);
		return newOrder;
	}

	@Override
	@Transactional
	public void deleteOrderById(OrderBook orderBook, long id) {
		// TODO throw not found exception if no order with id
		Order order = orderBook.getOrderById(id);
		orderBook.removeOrder(order);
	}

	@Override
	@Transactional
	public double fillOrder(OrderBook orderBook, Order order, double amnt) {
		order.fill(amnt);
		double amntLeft = order.getRemainingQuantity();
		if(amntLeft == 0) {
			deleteOrderById(orderBook, order.getId());
		}
		return amntLeft;
	}
	
	
	
	
}
