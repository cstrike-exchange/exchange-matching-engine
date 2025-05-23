package org.louisjohns32.personal.exchange.controllers;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.louisjohns32.personal.exchange.assemblers.OrderBookModelAssembler;
import org.louisjohns32.personal.exchange.dto.OrderBookDTO;
import org.louisjohns32.personal.exchange.dto.OrderBookLevelDTO;
import org.louisjohns32.personal.exchange.entities.OrderBook;
import org.louisjohns32.personal.exchange.exceptions.OrderBookNotFoundException;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
