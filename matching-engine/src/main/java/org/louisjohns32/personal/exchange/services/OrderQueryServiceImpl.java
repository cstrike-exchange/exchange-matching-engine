package org.louisjohns32.personal.exchange.services;

import org.springframework.stereotype.Service;

@Service
public class OrderQueryServiceImpl {


    /*
    @Override
    public OrderResponseDTO getOrder(Long orderId) {
        Order order =  orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        return OrderResponseDTO.fromEntity(order);
    }
    */
}
