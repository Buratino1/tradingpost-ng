package com.trading.ng.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SymbolBotStatus(
        String symbol,
        BigDecimal currentPrice,
        BigDecimal fastIndicator,
        BigDecimal slowIndicator,
        String signal,
        int priceHistorySize,
        int requiredDataPoints,
        BigDecimal positionQuantity,
        Instant lastTradeTime,
        List<BigDecimal> recentPrices
) {
}
