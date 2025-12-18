package org.louisjohns32.personal.exchange.marketdata.model;


import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderBookDelta {
    private String symbol;
    private List<LevelDelta> levelDeltas;
    private long sequenceNumber;
    private long timestamp;
}
