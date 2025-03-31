package org.louisjohns32.personal.exchange.services;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.entities.OrderBookLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class OrderBookServiceImpl implements OrderBookService {
	
	private static final AtomicLong idGenerator = new AtomicLong(1);
	
	@Autowired
	private Validator validator;
	
	private ConcurrentHashMap<String, OrderBook> orderBookMap;
	
	public OrderBookServiceImpl() {
		orderBookMap = new ConcurrentHashMap<String, OrderBook>();
	}
	
	@Override
	public OrderBook getOrderBook(String symbol) {
		return orderBookMap.get(symbol);
	}

	@Override
	public void createOrderBook(String symbol) {
		orderBookMap.putIfAbsent(symbol, new OrderBook(symbol));
	}
	
	@Override
	public Order createOrder(OrderBook orderBook, Order order) {
		Set<ConstraintViolation<Order>> violations = validator.validate(order);
		if(!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
		
		long orderId = idGenerator.getAndIncrement();
		Order newOrder = new Order(orderId, order);
		
		orderBook.addOrder(newOrder);
		
		match(orderBook, newOrder);
		return newOrder;
	}

	@Override
	public void deleteOrderById(OrderBook orderBook, long id) {
		// TODO throw not found exception if no order with id
		Order order = orderBook.getOrderById(id);
		orderBook.removeOrder(order);
	}

	@Override
	public double fillOrder(OrderBook orderBook, Order order, double amnt) {
		order.fill(amnt);
		double amntLeft = order.getRemainingQuantity();
		if(amntLeft == 0) {
			deleteOrderById(orderBook, order.getId());
		}
		return amntLeft;
	}
	
	@Override
	public void match(OrderBook orderBook, Order newOrder) {
		OrderBookLevel opposingLevel;
		do {
			opposingLevel = getOpposingSideLevel(orderBook, newOrder);
		} while(matchWithLevel(orderBook, newOrder, opposingLevel));
	}
	
	private boolean matchWithLevel(OrderBook orderBook, Order newOrder, OrderBookLevel opposingLevel) {
		if(newOrder.isFilled() || opposingLevel == null) return false;
		if(
				(newOrder.getSide() == Side.BUY && opposingLevel.getPrice() <= newOrder.getPrice())
				|| (newOrder.getSide() == Side.SELL && opposingLevel.getPrice() >= newOrder.getPrice())
		) {
			Order opposingOrder = opposingLevel.getOrder();
			double amntToFill = Math.min(opposingOrder.getRemainingQuantity(), newOrder.getRemainingQuantity());
			// TODO set newOrder filled price
			fillOrder(orderBook, newOrder, amntToFill);
			fillOrder(orderBook, opposingOrder, amntToFill);
			return true;
		}
		return false;
	}
	
	private OrderBookLevel getOpposingSideLevel(OrderBook orderBook, Order order) {
		if(order.getSide() == Side.BUY) return orderBook.getLowestAskLevel();
		else return orderBook.getHighestBidLevel();
	}
}
