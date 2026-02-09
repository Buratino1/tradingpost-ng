package com.trading.ng.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "binance")
public record BinanceProperties(
        String apiKey,
        String privateKeyPath,
        String privateKeyPass,
        String baseUrl,
        String streamUrl,
        boolean useTestnet,
        List<String> symbols
) {
}
