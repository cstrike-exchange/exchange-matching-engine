package org.louisjohns32.personal.exchange.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.assemblers.OrderBookModelAssembler;
import org.louisjohns32.personal.exchange.common.domain.Side;
import org.louisjohns32.personal.exchange.dto.OrderBookDTO;
import org.louisjohns32.personal.exchange.dto.OrderBookLevelDTO;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.exceptions.OrderBookNotFoundException;
import org.louisjohns32.personal.exchange.mappers.OrderMapper;
import org.louisjohns32.personal.exchange.services.IdGenerator;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.louisjohns32.personal.exchange.services.OrderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(OrderBookApiController.class)
@Import({ OrderBookModelAssembler.class, OrderMapper.class })
public class OrderBookApiControllerTests {
	
	@Autowired
	private MockMvc mvc;
	
	@MockitoBean
	private OrderBookService orderBookService;

    @MockitoBean
    private OrderQueryService orderQueryService;

    @MockitoBean
    private IdGenerator<Long> idGenerator;

    @BeforeEach
    public void setup() {
        AtomicLong idCounter = new AtomicLong(1000L);
        when(idGenerator.nextId()).thenAnswer(invocation -> idCounter.getAndIncrement());
    }
	
	@Test
    public void getOrderBookValid() throws Exception {
        String symbol = "SYMB";

        OrderBookDTO mockResponse = new OrderBookDTO(
            symbol,
            List.of(
                new OrderBookLevelDTO(192.17, 6.0),
                new OrderBookLevelDTO(192.16, 20.0)
            ),
            List.of(
                new OrderBookLevelDTO(192.2, 51.0),
                new OrderBookLevelDTO(192.21, 10.0)
            )
        );

        when(orderBookService.getAggregatedOrderBook(eq(symbol))).thenReturn(mockResponse);

        mvc.perform(get("/api/orderbook/{symbol}", symbol))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value(symbol))
            .andExpect(jsonPath("$.bidLevels[0].price").value(192.17))
            .andExpect(jsonPath("$.bidLevels[0].volume").value(6.0))
            .andExpect(jsonPath("$.askLevels[1].price").value(192.21))
            .andExpect(jsonPath("$.askLevels[1].volume").value(10.0));
    }
	 
	@Test
    public void getOrderBookNotFound() throws Exception {
        String symbol = "SYMB";

        OrderBookDTO mockResponse = new OrderBookDTO(
            symbol,
            List.of(
                new OrderBookLevelDTO(192.17, 6.0),
                new OrderBookLevelDTO(192.16, 20.0)
            ),
            List.of(
                new OrderBookLevelDTO(192.2, 51.0),
                new OrderBookLevelDTO(192.21, 10.0)
            )
        );

        when(orderBookService.getAggregatedOrderBook(eq(symbol))).thenThrow(OrderBookNotFoundException.class);

        mvc.perform(get("/api/orderbook/{symbol}", symbol))
            .andExpect(status().isNotFound());
    }
	
	@Nested
	public class CreateOrderTests {

	    @Test
	    public void createValidOrderCallsService() throws Exception {
	        String symbol = "SYMB";
	        
	        Order mockOrder = new Order(1L, symbol, Side.BUY, 10., 100.);
	        
	        when(orderBookService.createOrder(eq(symbol), any(Order.class)))
	            .thenReturn(mockOrder);
	        
	        String orderRequestJson = "{"
	                + "\"quantity\": 10,"
	                + "\"price\": 100,"
	                + "\"side\": \"BUY\","
	                + "\"symbol\": \"" + symbol + "\""
	                + "}";
	        
	        mvc.perform(post("/api/orders", symbol)
	                .content(orderRequestJson)
	                .contentType(MediaType.APPLICATION_JSON))
	            .andExpect(status().isCreated());
	        
	        verify(orderBookService).createOrder(eq(symbol), any(Order.class));
	    }

	    @Test
	    public void createOrder_invalidInput_returnsBadRequest() throws Exception {
	        String symbol = "SYMB";

	        String orderRequestJson = "{"
	                + "\"quantity\": 10,"
	                + "\"price\": 0,"   
	                + "\"side\": \"BUY\","
	                + "\"symbol\": \"" + symbol + "\""
	                + "}";

	        mvc.perform(post("/api/orders", symbol)
	                .content(orderRequestJson)
	                .contentType(MediaType.APPLICATION_JSON))
	            .andExpect(status().isBadRequest());
	    }

	}
	
	@Nested
	public class CreateOrderBookTests {
		@Test
		public void createValidOrderBookCallsService() throws Exception {
			OrderBook ob = new OrderBook("SYMB");
			when(orderBookService.createOrderBook(eq(ob.getSymbol()))).thenReturn(ob);
			mvc.perform(post("/api/orderbook").content("{"
					+ "\"symbol\": \"" + ob.getSymbol() + "\""
					+ "}").contentType(MediaType.APPLICATION_JSON));
			verify(orderBookService).createOrderBook(eq(ob.getSymbol()));
		}
		
		@Test
		public void createValidOrderBookCorrectResponse() throws Exception {
			OrderBook ob = new OrderBook("SYMB");
			
			when(orderBookService.createOrderBook(eq(ob.getSymbol()))).thenReturn(ob);
			
			mvc.perform(post("/api/orderbook").content("{"
					+ "\"symbol\": \"" + ob.getSymbol() + "\""
					+ "}")
					.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated())
			   		.andExpect(header().string("location", endsWith("/api/orderbook/" + ob.getSymbol())));
		}
		
		@Nested
		public class CreateOrderBookValidationTests {

		    @Test
		    public void createOrderBook_blankSymbol_returnsBadRequest() throws Exception {
		        mvc.perform(post("/api/orderbook")
		                .content("{ \"symbol\": \"\" }")
		                .contentType(MediaType.APPLICATION_JSON))
		           .andExpect(status().isBadRequest());
		    }

		    @Test
		    public void createOrderBook_symbolTooShort_returnsBadRequest() throws Exception {
		        mvc.perform(post("/api/orderbook")
		                .content("{ \"symbol\": \"AB\" }")
		                .contentType(MediaType.APPLICATION_JSON))
		           .andExpect(status().isBadRequest());
		    }

		    @Test
		    public void createOrderBook_symbolTooLong_returnsBadRequest() throws Exception {
		        mvc.perform(post("/api/orderbook")
		                .content("{ \"symbol\": \"TOOLONGSYMB\" }")
		                .contentType(MediaType.APPLICATION_JSON))
		           .andExpect(status().isBadRequest());
		    }

		    @Test
		    public void createOrderBook_symbolWithNonAlphabeticalChars_returnsBadRequest() throws Exception {
		        mvc.perform(post("/api/orderbook")
		                .content("{ \"symbol\": \"ABC123\" }")
		                .contentType(MediaType.APPLICATION_JSON))
		           .andExpect(status().isBadRequest());
		    }
		}
	}
}
