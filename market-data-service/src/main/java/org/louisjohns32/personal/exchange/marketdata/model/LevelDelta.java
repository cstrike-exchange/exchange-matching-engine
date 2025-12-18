package org.louisjohns32.personal.exchange.marketdata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.louisjohns32.personal.exchange.common.domain.Side;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class LevelDelta {
    private Side side;
    private BigDecimal price;
    private BigDecimal volumeDifference;
}
