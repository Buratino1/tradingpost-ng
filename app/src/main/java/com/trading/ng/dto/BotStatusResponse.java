package com.trading.ng.dto;

import com.trading.ng.service.StrategyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record BotStatusResponse(
        boolean running,
        Instant startedAt,
        StrategyType strategy,
        int shortSmaPeriod,
        int longSmaPeriod,
        int vortexPeriod,
        int samplingIntervalSeconds,
        BigDecimal orderSizePercent,
        int cooldownSeconds,
        Map<String, SymbolBotStatus> symbols
) {
}
