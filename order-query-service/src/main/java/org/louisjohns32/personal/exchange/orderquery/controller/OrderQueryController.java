package org.louisjohns32.personal.exchange.orderquery.controller;

import org.louisjohns32.personal.exchange.orderquery.dto.OrderResponseDTO;
import org.louisjohns32.personal.exchange.orderquery.service.OrderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", produces = { MediaType.APPLICATION_JSON_VALUE})
public class OrderQueryController {

    @Autowired
    private OrderQueryService queryService;

    @GetMapping("/orders/{orderId}")
    public OrderResponseDTO getOrder(@PathVariable Long orderId) {
        return queryService.getOrder(orderId);
    }

}
