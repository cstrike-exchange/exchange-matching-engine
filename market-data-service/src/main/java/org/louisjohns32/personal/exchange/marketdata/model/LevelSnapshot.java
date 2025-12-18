package org.louisjohns32.personal.exchange.marketdata.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LevelSnapshot {

    private BigDecimal price;
    private BigDecimal volume;
}
