package org.louisjohns32.personal.exchange.persist.dao;


import org.louisjohns32.personal.exchange.persist.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<TradeEntity, Long> {

}
