package org.louisjohns32.personal.exchange.orderquery.service;

import org.louisjohns32.personal.exchange.orderquery.dto.OrderResponseDTO;

public interface OrderQueryService {

    OrderResponseDTO getOrder(Long orderId);

}
