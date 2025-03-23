package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBookLevel;

public interface OrderBookLevelService {
	public void addOrder(OrderBookLevel level, Order order);
}
