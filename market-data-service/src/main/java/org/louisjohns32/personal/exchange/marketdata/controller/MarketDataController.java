package org.louisjohns32.personal.exchange.marketdata.controller;

import org.louisjohns32.personal.exchange.marketdata.model.OrderBookSnapshot;
import org.louisjohns32.personal.exchange.marketdata.service.OrderBookStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MarketDataController {

    @Autowired
    private OrderBookStateService orderBookStateService;

    @CrossOrigin(origins = "*") // TODO disable this after testing
    @GetMapping("/orderbook/{symbol}")
    public OrderBookSnapshot requestSnapshot(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "20") int depth
    ) {
        return orderBookStateService.getOrderBookSnapshot(symbol, depth);
    }
}
