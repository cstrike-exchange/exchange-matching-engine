package org.louisjohns32.personal.exchange.controllers;

import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.assemblers.OrderBookModelAssembler;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@WebMvcTest(OrderBookApiController.class)
@Import({ OrderBookModelAssembler.class })
public class OrderBookApiControllerTests {
	
	@Autowired
	private MockMvc mvc;
	
	@MockitoBean
	private OrderBookService orderBookService;
	
	// TODO get orderbook tests
	
}
