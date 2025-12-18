package org.louisjohns32.personal.exchange.marketdata.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class OrderBookSnapshot {

    private long sequenceNumber;
    private long timestamp;

    List<LevelSnapshot> bidLevels;
    List<LevelSnapshot> askLevels;
}
