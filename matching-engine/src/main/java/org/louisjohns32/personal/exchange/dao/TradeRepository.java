package org.louisjohns32.personal.exchange.dao;

import org.louisjohns32.personal.exchange.entities.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

}
