package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.dto.OrderResponseDTO;

public interface OrderQueryService {

    public OrderResponseDTO getOrder(Long orderId);

}
