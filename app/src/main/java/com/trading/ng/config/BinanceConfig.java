package com.trading.ng.config;

import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.common.configuration.SignatureConfiguration;
import com.binance.connector.client.common.websocket.configuration.WebSocketClientConfiguration;
import com.binance.connector.client.spot.rest.SpotRestApiUtil;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.websocket.stream.SpotWebSocketStreamsUtil;
import com.binance.connector.client.spot.websocket.stream.api.SpotWebSocketStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceConfig {

    private static final Logger log = LoggerFactory.getLogger(BinanceConfig.class);

    @Bean
    public SpotRestApi spotRestApi(BinanceProperties props) {
        ClientConfiguration config = SpotRestApiUtil.getClientConfiguration();

        if (props.useTestnet()) {
            config.setUrl("https://testnet.binance.vision");
            log.info("Binance REST API configured for TESTNET");
        } else if (props.baseUrl() != null && !props.baseUrl().isBlank()) {
            config.setUrl(props.baseUrl());
        }

        if (props.apiKey() != null && !props.apiKey().isBlank()) {
            SignatureConfiguration sig = new SignatureConfiguration();
            sig.setApiKey(props.apiKey());
            if (props.privateKeyPath() != null && !props.privateKeyPath().isBlank()) {
                sig.setPrivateKey(props.privateKeyPath());
            }
            if (props.privateKeyPass() != null && !props.privateKeyPass().isBlank()) {
                sig.setPrivateKeyPass(props.privateKeyPass());
            }
            config.setSignatureConfiguration(sig);
        }

        log.info("Binance SpotRestApi initialized (baseUrl={})", config.getUrl());
        return new SpotRestApi(config);
    }

    @Bean
    public SpotWebSocketStreams spotWebSocketStreams(BinanceProperties props) {
        WebSocketClientConfiguration config = SpotWebSocketStreamsUtil.getClientConfiguration();
        config.setUsePool(true);

        if (props.useTestnet()) {
            config.setUrl("wss://testnet.binance.vision");
            log.info("Binance WebSocket Streams configured for TESTNET");
        } else if (props.streamUrl() != null && !props.streamUrl().isBlank()) {
            config.setUrl(props.streamUrl());
        }

        log.info("Binance SpotWebSocketStreams initialized (pool=true)");
        return new SpotWebSocketStreams(config);
    }
}
