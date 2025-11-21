package org.louisjohns32.personal.exchange.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.dto.OrderBookDTO;
import org.louisjohns32.personal.exchange.dto.OrderBookLevelDTO;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.entities.OrderBookLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Service handling order book operations and matching engine logic.
 * Implements price-time priority matching and publishes lifecycle events to AWS SNS.
 */
@Service
public class OrderBookServiceImpl implements OrderBookService {
	
	private final AtomicLong idGenerator = new AtomicLong(1);
	private final AtomicLong sequenceGenerator = new AtomicLong(1);
	
	@Autowired
	private Validator validator;
	
	@Autowired
	private OrderBookRegistry registry;
	
	@Autowired
	private SnsAsyncClient snsClient;
	
	@Value("${sns_topic_arn}")
    private String snsTopicArn;
	
	
	private  CompletableFuture<Void> publishEventMessage(String message, String eventType) {
		PublishRequest request = PublishRequest.builder()
		        .topicArn(snsTopicArn)
		        .message(message)
		        .messageAttributes(Map.of(
		            "eventType", software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
		                            .dataType("String")
		                            .stringValue(eventType)
		                            .build()
		        ))
		        .build();

		    return snsClient.publish(request)
		            .thenAccept(response -> {
		                System.out.println("Published SNS event: " + eventType);
		            })
		            .exceptionally(e -> {
		                // FUTURE: Add retry logic and alerting for publishing failures
		                System.err.println("Failed to publish SNS event: " + e.getMessage());
		                return null;
		            });
	}
	
	private String buildOrderEventMessage(String eventType, String symbol,  Order order) {
		long sequenceNumber = sequenceGenerator.getAndIncrement();
	    return "{"
	    	+ "\"sequenceNumber\":" + sequenceNumber + ","
	        + "\"eventType\":\"" + eventType + "\","
	        + "\"symbol\":\"" + symbol + "\","
	        + "\"orderId\":" + order.getId() + ","
	        + "\"side\":\"" + order.getSide() + "\","
	        + "\"price\":" + order.getPrice() + ","
	        + "\"quantity\":" + order.getQuantity() + ","
	        + "\"filledQuantity\":" + order.getFilledQuantity()
	        + "}";
	}
	
	private String buildTradeEventMessage(String eventType, String symbol,  Order buyOrder, Order sellOrder, double price, double quantity) {
		long sequenceNumber = sequenceGenerator.getAndIncrement();
		return "{"
			+ "\"sequenceNumber\":" + sequenceNumber + ","
			+ "\"eventType\":\""+ eventType + "\","
			 + "\"symbol\":\"" + symbol + "\","
			+ "\"timestamp\":" + System.currentTimeMillis() + ","
			+ "\"buyOrderId\":" + buyOrder.getId() + ","
			+ "\"sellOrderId\":" + sellOrder.getId() + ","
			+ "\"price\":" + price + ","
			+ "\"quantity\":" + quantity
			+ "}";
	}
	
	
	@Override
	public OrderBook getOrderBook(String symbol) {
		return registry.getOrderBook(symbol);
	}

	@Override
	public OrderBook createOrderBook(String symbol) {
		registry.createOrderBook(symbol);
		return registry.getOrderBook(symbol);
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
		
		publishEventMessage(buildOrderEventMessage("ORDER_CREATED", orderBook.getSymbol(), newOrder), "ORDER_CREATED");
		
		match(orderBook, newOrder);
		return newOrder;
	}

	@Override
	public void deleteOrderById(OrderBook orderBook, long id) {
		// FUTURE: Add validation for order existence
		Order order = orderBook.getOrderById(id);
		orderBook.removeOrder(order);
		publishEventMessage(buildOrderEventMessage("ORDER_CANCELLED", orderBook.getSymbol(), order), "ORDER_CANCELLED");
	}

	@Override
	public double fillOrder(OrderBook orderBook, Order order, double amnt) {
		order.fill(amnt);
		double amntLeft = order.getRemainingQuantity();
		if(amntLeft == 0) {
			orderBook.removeOrder(order);
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
			// FUTURE: Track fill price per order (currently uses opposing order price)
			fillOrder(orderBook, newOrder, amntToFill);
			fillOrder(orderBook, opposingOrder, amntToFill);
			
			 publishEventMessage(buildTradeEventMessage("TRADE_EXECUTED", orderBook.getSymbol(), (newOrder.getSide()==Side.BUY) ? newOrder : opposingOrder,
					 (newOrder.getSide()==Side.SELL) ? newOrder : opposingOrder, opposingOrder.getPrice(), amntToFill), "TRADE_EXECUTED");
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
		
		Collections.reverse(bidDTOs); // could be optimised (change bid map to be descending? )
		
		return new OrderBookDTO(symbol, bidDTOs, askDTOs);
	}

	@Override
	public Order createOrder(String symbol, Order order) {
		OrderBook ob = registry.getOrderBook(symbol);
		return createOrder(ob, order);
	}
}
