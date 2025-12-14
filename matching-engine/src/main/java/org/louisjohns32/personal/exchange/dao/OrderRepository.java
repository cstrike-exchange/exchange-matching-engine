package org.louisjohns32.personal.exchange.dao;

import org.louisjohns32.personal.exchange.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndSymbol(Long id, String symbol);

}
