package org.louisjohns32.personal.exchange.orderquery.dao;

import org.louisjohns32.personal.exchange.orderquery.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderReadRepository extends JpaRepository<OrderEntity, Long> {

}
