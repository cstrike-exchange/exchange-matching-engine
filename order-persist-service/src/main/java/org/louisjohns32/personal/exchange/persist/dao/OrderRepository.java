package org.louisjohns32.personal.exchange.persist.dao;

import org.louisjohns32.personal.exchange.persist.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByIdAndSymbol(Long id, String symbol);

}
