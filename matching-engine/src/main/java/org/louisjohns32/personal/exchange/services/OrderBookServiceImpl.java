package org.louisjohns32.personal.exchange.services;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.common.events.OrderCancellationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderCreationEvent;
import org.louisjohns32.personal.exchange.common.events.OrderEvent;
import org.louisjohns32.personal.exchange.common.events.TradeExecutionEvent;
import org.louisjohns32.personal.exchange.dto.OrderBookDTO;
import org.louisjohns32.personal.exchange.dto.OrderBookLevelDTO;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.entities.OrderBookLevel;
import org.louisjohns32.personal.exchange.entities.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;


/**
 * Service handling order book operations and matching engine logic.
 * Implements price-time priority matching and publishes lifecycle events.
 */
@Service
public class OrderBookServiceImpl implements OrderBookService {

    @Autowired
	private IdGenerator<Long> idGenerator;

	@Autowired
	private Validator validator;
	
	@Autowired
	private OrderBookRegistry registry;

    @Autowired
    private EventPublisher publisher;
	
	
	@Override
	public synchronized OrderBook getOrderBook(String symbol) {
		return registry.getOrderBook(symbol);
	}

	@Override
	public synchronized OrderBook createOrderBook(String symbol) {
		registry.createOrderBook(symbol);
		return registry.getOrderBook(symbol);
	}
	
	@Override
	public synchronized Order createOrder(OrderBook orderBook, Order order) {
		Set<ConstraintViolation<Order>> violations = validator.validate(order);
		if(!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
		
		long orderId = idGenerator.nextId();
		Order newOrder = new Order(orderId, order);

        // Add order
		orderBook.addOrder(newOrder);

        List<OrderEvent> events = new ArrayList<>();
        events.add(new OrderCreationEvent(
                newOrder.getId(),
                newOrder.getSymbol(),
                newOrder.getSide(),
                newOrder.getQuantity(),
                newOrder.getPrice(),
                newOrder.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        ));

        // Match order
        events.addAll(match(orderBook, newOrder).stream().map(this::buildTradeEvent).toList());

        publisher.publishBatch(events);
        return newOrder;
	}

	@Override
	public synchronized void deleteOrderById(OrderBook orderBook, long id) {
		Order order = orderBook.getOrderById(id);
		orderBook.removeOrder(order);

        publisher.publish(new OrderCancellationEvent(order.getId(), order.getSymbol(), System.currentTimeMillis()));
	}

	@Override
	public synchronized double fillOrder(OrderBook orderBook, Order order, double amnt) {
		order.fill(amnt);
		double amntLeft = order.getRemainingQuantity();
		if(amntLeft == 0) {
			orderBook.removeOrder(order);
		}
		
		return amntLeft;
	}
	
	@Override
	public synchronized List<Trade> match(OrderBook orderBook, Order newOrder) {
		OrderBookLevel opposingLevel;
        Trade executedTrade;
        List<Trade> trades = new ArrayList<>();
		do {
			opposingLevel = getOpposingSideLevel(orderBook, newOrder);
            executedTrade = matchWithLevel(orderBook, newOrder, opposingLevel);
            trades.add(executedTrade);
		} while(executedTrade != null);
        trades.removeLast();
        return trades;
	}
	
	private Trade matchWithLevel(OrderBook orderBook, Order newOrder, OrderBookLevel opposingLevel) {
		if(newOrder.isFilled() || opposingLevel == null) return null;
		if(
				(newOrder.getSide() == Side.BUY && opposingLevel.getPrice() <= newOrder.getPrice())
				|| (newOrder.getSide() == Side.SELL && opposingLevel.getPrice() >= newOrder.getPrice())
		) {
			Order opposingOrder = opposingLevel.getOrder();
			double amntToFill = Math.min(opposingOrder.getRemainingQuantity(), newOrder.getRemainingQuantity());
			// FUTURE: Track fill price per order (currently uses opposing order price)
			fillOrder(orderBook, newOrder, amntToFill);
			fillOrder(orderBook, opposingOrder, amntToFill);

            Long buyOrderId,  sellOrderId;
            if (newOrder.getSide() == Side.BUY) {
                buyOrderId = newOrder.getId();
                sellOrderId = opposingOrder.getId();
            } else {
                buyOrderId = opposingOrder.getId();
                sellOrderId = newOrder.getId();
            }
            return new Trade(
                    orderBook.getSymbol(),
                    buyOrderId,
                    sellOrderId,
                    opposingOrder.getPrice(),
                    amntToFill,
                    Instant.now().toEpochMilli()
            );
		}
		return null;
	}
	
	private OrderBookLevel getOpposingSideLevel(OrderBook orderBook, Order order) {
		if(order.getSide() == Side.BUY) return orderBook.getLowestAskLevel();
		else return orderBook.getHighestBidLevel();
	}

	@Override
	public synchronized OrderBookDTO getAggregatedOrderBook(String symbol) {
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
		
		Collections.reverse(bidDTOs); // could be optimised (change bid map to be descending? )
		
		return new OrderBookDTO(symbol, bidDTOs, askDTOs);
	}

	@Override
	public synchronized Order createOrder(String symbol, Order order) {
		OrderBook ob = registry.getOrderBook(symbol);
		return createOrder(ob, order);
	}

    private TradeExecutionEvent buildTradeEvent(Trade trade) {
        return new TradeExecutionEvent(trade.getSymbol(), trade.getBuyOrderId(),
                trade.getSellOrderId(),
                trade.getPrice(),
                trade.getQuantity(),
                trade.getExecutedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
            );
    }
}
