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
		
		//matchingEngine.match(orderBook);
		return newOrder;
	}

	@Override
	public void deleteOrderById(long id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fillOrder() {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
}
