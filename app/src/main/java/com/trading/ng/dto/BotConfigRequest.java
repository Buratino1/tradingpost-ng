package com.trading.ng.dto;

import com.trading.ng.service.StrategyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record BotConfigRequest(
        List<String> symbols,
        StrategyType strategy,
        @Min(2) Integer shortSmaPeriod,
        @Min(3) Integer longSmaPeriod,
        @Min(5) Integer vortexPeriod,
        @Min(10) Integer samplingIntervalSeconds,
        @Positive @Max(1) BigDecimal orderSizePercent,
        @Min(0) Integer cooldownSeconds,
        @Positive BigDecimal minOrderSizeEur
) {
}
