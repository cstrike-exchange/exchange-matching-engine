package org.louisjohns32.personal.exchange.config;

import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitialDataLoader {
	
	@Autowired
	OrderBookService orderBookService;
	
	@Bean
	CommandLineRunner initOrderBooks() {
		return args -> {
			orderBookService.createOrderBook("NVDA");
			orderBookService.createOrderBook("AMZN");
			orderBookService.createOrderBook("GOOG");
		};
	}

}
