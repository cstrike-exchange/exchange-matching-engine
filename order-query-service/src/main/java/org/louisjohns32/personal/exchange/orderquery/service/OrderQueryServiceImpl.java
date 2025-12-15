package org.louisjohns32.personal.exchange.orderquery.service;

import org.louisjohns32.personal.exchange.orderquery.dao.OrderReadRepository;
import org.louisjohns32.personal.exchange.orderquery.dto.OrderResponseDTO;
import org.louisjohns32.personal.exchange.orderquery.entity.OrderEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    @Autowired
    OrderReadRepository orderRepository;

    @Override
    public OrderResponseDTO getOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        return OrderResponseDTO.fromEntity(order);
    }
}
