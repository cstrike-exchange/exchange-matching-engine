package org.louisjohns32.personal.exchange.config;

import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("!test")
public class InitialDataLoader {
	
	@Autowired
	OrderBookService orderBookService;
	
	@Bean
	CommandLineRunner initOrderBooks() {
		return args -> {
			orderBookService.createOrderBook("NVDA");
			orderBookService.createOrderBook("AMZN");
			orderBookService.createOrderBook("GOOG");
			
			OrderBook amazonOB = orderBookService.getOrderBook("AMZN");
			List<Order> orders = new ArrayList<Order>();
			orders.add(new Order("AMZN", Side.BUY, 2., 192.17));
			orders.add(new Order("AMZN", Side.BUY, 3., 192.17));
			orders.add(new Order("AMZN", Side.BUY, 1., 192.17));
			orders.add(new Order("AMZN", Side.BUY, 20., 192.16));
			orders.add(new Order("AMZN", Side.SELL, 21., 192.20));
			orders.add(new Order("AMZN", Side.SELL, 30., 192.20));
			orders.add(new Order("AMZN", Side.SELL, 10., 192.21));
			
			for(Order order : orders) {
				orderBookService.createOrder(amazonOB, order);
			}
		};
	}

}
