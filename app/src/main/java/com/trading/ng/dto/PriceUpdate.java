package com.trading.ng.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceUpdate(
        String symbol,
        BigDecimal price,
        BigDecimal volume24h,
        BigDecimal priceChange24h,
        Instant timestamp
) {
}
