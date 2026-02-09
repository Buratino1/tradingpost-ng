package com.trading.ng.service;

import com.binance.connector.client.spot.websocket.stream.api.SpotWebSocketStreams;
import com.binance.connector.client.spot.websocket.stream.model.MiniTickerRequest;
import com.binance.connector.client.spot.websocket.stream.model.MiniTickerResponse;
import com.binance.connector.client.spot.websocket.stream.model.TradeRequest;
import com.binance.connector.client.spot.websocket.stream.model.TradeResponse;
import com.binance.connector.client.spot.websocket.stream.model.KlineRequest;
import com.binance.connector.client.spot.websocket.stream.model.KlineResponse;
import com.binance.connector.client.common.websocket.service.StreamBlockingQueueWrapper;
import com.trading.ng.config.BinanceProperties;
import com.trading.ng.dto.PriceUpdate;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Subscribes to Binance WebSocket streams for real-time market data.
 * Maintains an in-memory cache of latest prices per symbol.
 * Virtual threads handle the blocking queue polling.
 */
@Service
public class MarketDataStreamService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataStreamService.class);

    private final SpotWebSocketStreams wsStreams;
    private final BinanceProperties props;

    private final Map<String, PriceUpdate> latestPrices = new ConcurrentHashMap<>();
    private final List<Consumer<PriceUpdate>> priceListeners = new CopyOnWriteArrayList<>();
    private final List<Thread> pollingThreads = new CopyOnWriteArrayList<>();

    private volatile boolean running = true;

    public MarketDataStreamService(SpotWebSocketStreams wsStreams, BinanceProperties props) {
        this.wsStreams = wsStreams;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startStreams() {
        List<String> symbols = props.symbols();
        if (symbols == null || symbols.isEmpty()) {
            log.warn("No symbols configured — WebSocket streams will not start");
            return;
        }

        for (String symbol : symbols) {
            String sym = symbol.toLowerCase();
            subscribeMiniTicker(sym);
            subscribeTrades(sym);
        }

        log.info("WebSocket streams started for symbols: {}", symbols);
    }

    private void subscribeMiniTicker(String symbol) {
        MiniTickerRequest request = new MiniTickerRequest();
        request.setSymbol(symbol);
        StreamBlockingQueueWrapper<MiniTickerResponse> queue = wsStreams.miniTicker(request);

        Thread poller = Thread.ofVirtual().name("ws-ticker-" + symbol).start(() -> {
            log.debug("Mini ticker polling started for {}", symbol);
            while (running) {
                try {
                    MiniTickerResponse event = queue.take();
                    PriceUpdate update = new PriceUpdate(
                            symbol.toUpperCase(),
                            new BigDecimal(event.getcLowerCase()),        // close price
                            new BigDecimal(event.getvLowerCase()),        // base asset volume
                            new BigDecimal(event.getcLowerCase()).subtract(new BigDecimal(event.getoLowerCase())),  // price change (close - open)
                            Instant.ofEpochMilli(event.getE())           // event time
                    );
                    latestPrices.put(symbol.toUpperCase(), update);
                    notifyListeners(update);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error processing mini ticker for {}: {}", symbol, e.getMessage());
                }
            }
        });
        pollingThreads.add(poller);
    }

    private void subscribeTrades(String symbol) {
        TradeRequest request = new TradeRequest();
        request.setSymbol(symbol);
        StreamBlockingQueueWrapper<TradeResponse> queue = wsStreams.trade(request);

        Thread poller = Thread.ofVirtual().name("ws-trade-" + symbol).start(() -> {
            log.debug("Trade stream polling started for {}", symbol);
            while (running) {
                try {
                    TradeResponse event = queue.take();
                    log.trace("Trade {} {} price={} qty={}",
                            symbol, event.getmLowerCase() ? "SELL" : "BUY",  // m = buyer is maker
                            event.getpLowerCase(), event.getqLowerCase());   // p = price, q = quantity
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error processing trade for {}: {}", symbol, e.getMessage());
                }
            }
        });
        pollingThreads.add(poller);
    }

    public PriceUpdate getLatestPrice(String symbol) {
        return latestPrices.get(symbol.toUpperCase());
    }

    public Collection<PriceUpdate> getAllLatestPrices() {
        return latestPrices.values();
    }

    public void addPriceListener(Consumer<PriceUpdate> listener) {
        priceListeners.add(listener);
    }

    private void notifyListeners(PriceUpdate update) {
        for (Consumer<PriceUpdate> listener : priceListeners) {
            try {
                listener.accept(update);
            } catch (Exception e) {
                log.error("Price listener error: {}", e.getMessage());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebSocket streams...");
        running = false;
        for (Thread t : pollingThreads) {
            t.interrupt();
        }
    }
}
