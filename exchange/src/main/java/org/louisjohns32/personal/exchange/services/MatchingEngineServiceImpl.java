package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.entities.OrderBookLevel;
import org.springframework.beans.factory.annotation.Autowired;

public class MatchingEngineServiceImpl implements MatchingEngineService {
	
	@Autowired
	private OrderBookService orderBookService;

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
			orderBookService.fillOrder(orderBook, newOrder, amntToFill);
			orderBookService.fillOrder(orderBook, opposingOrder, amntToFill);
			return true;
		}
		return false;
	}
	
	private OrderBookLevel getOpposingSideLevel(OrderBook orderBook, Order order) {
		if(order.getSide() == Side.BUY) return orderBook.getLowestAskLevel();
		else return orderBook.getHighestBidLevel();
	}
}
