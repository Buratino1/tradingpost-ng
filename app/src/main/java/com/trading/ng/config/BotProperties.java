package com.trading.ng.config;

import com.trading.ng.service.StrategyType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "bot")
public record BotProperties(
        boolean enabled,
        List<String> symbols,
        StrategyType strategy,
        int shortSmaPeriod,
        int longSmaPeriod,
        int vortexPeriod,
        int samplingIntervalSeconds,
        BigDecimal orderSizePercent,
        int cooldownSeconds,
        BigDecimal minOrderSizeEur,
        String quoteAsset
) {
}
