package org.louisjohns32.personal.exchange.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.dto.OrderBookDTO;
import org.louisjohns32.personal.exchange.dto.OrderBookLevelDTO;
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
	
	private final AtomicLong idGenerator = new AtomicLong(1);
	
	@Autowired
	private Validator validator;
	
	@Autowired
	private OrderBookRegistry registry;
	
	
	@Override
	public OrderBook getOrderBook(String symbol) {
		return registry.getOrderBook(symbol);
	}

	@Override
	public void createOrderBook(String symbol) {
		registry.createOrderBook(symbol);
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

	@Override
	public OrderBookDTO getAggregatedOrderBook(String symbol) {
		OrderBook orderBook = registry.getOrderBook(symbol);
		
		Map<Double, OrderBookLevel> askLevels = orderBook.getAskLevels();
		Map<Double, OrderBookLevel> bidLevels = orderBook.getBidLevels();
		
		List<OrderBookLevelDTO> bidDTOs = new ArrayList<OrderBookLevelDTO>();
		List<OrderBookLevelDTO> askDTOs = new ArrayList<OrderBookLevelDTO>();
		for(Map.Entry<Double, OrderBookLevel> e : bidLevels.entrySet()) {
			bidDTOs.add(new OrderBookLevelDTO(e.getKey(), e.getValue().getVolume()));
		}
		for(Map.Entry<Double, OrderBookLevel> e : askLevels.entrySet()) {
			askDTOs.add(new OrderBookLevelDTO(e.getKey(), e.getValue().getVolume()));
		}
		
		return new OrderBookDTO(symbol, bidDTOs, askDTOs);
	}
}
