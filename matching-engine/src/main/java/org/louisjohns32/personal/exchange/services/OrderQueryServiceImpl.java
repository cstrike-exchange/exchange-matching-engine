package org.louisjohns32.personal.exchange.services;

import org.louisjohns32.personal.exchange.dao.OrderRepository;
import org.louisjohns32.personal.exchange.dto.OrderResponseDTO;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.exceptions.OrderNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public OrderResponseDTO getOrder(Long orderId) {
        Order order =  orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        return OrderResponseDTO.fromEntity(order);
    }
}
