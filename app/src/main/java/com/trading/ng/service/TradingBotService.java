package com.trading.ng.service;

import com.trading.ng.config.BotProperties;
import com.trading.ng.domain.OrderSide;
import com.trading.ng.domain.OrderType;
import com.trading.ng.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TradingBotService {

    private static final Logger log = LoggerFactory.getLogger(TradingBotService.class);
    private static final int PRICE_SCALE = 8;
    private static final int MAX_HISTORY_SIZE = 200;

    private final MarketDataStreamService marketDataService;
    private final OrderService orderService;
    private final PortfolioService portfolioService;
    private final SmaCalculationService smaService;
    private final VortexCalculationService vortexService;

    // Active strategy
    private volatile AbstractSignalCalculationService activeStrategy;
    private volatile StrategyType strategyType;

    // Mutable runtime configuration
    private volatile BigDecimal orderSizePercent;
    private volatile int cooldownSeconds;
    private volatile BigDecimal minOrderSizeEur;
    private volatile int samplingIntervalSeconds;
    private volatile String quoteAsset;
    private volatile List<String> symbols;

    // Bot lifecycle
    private volatile boolean running = false;
    private volatile Instant startedAt;

    // Per-symbol state
    private final Map<String, List<BigDecimal>> priceHistory = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> positions = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastTradeTime = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal[]> prevIndicators = new ConcurrentHashMap<>();

    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final boolean autoStart;

    public TradingBotService(
            MarketDataStreamService marketDataService,
            OrderService orderService,
            PortfolioService portfolioService,
            SmaCalculationService smaService,
            VortexCalculationService vortexService,
            BotProperties botProperties) {

        this.marketDataService = marketDataService;
        this.orderService = orderService;
        this.portfolioService = portfolioService;
        this.smaService = smaService;
        this.vortexService = vortexService;

        this.strategyType = botProperties.strategy();
        this.activeStrategy = selectStrategy(this.strategyType);

        this.samplingIntervalSeconds = botProperties.samplingIntervalSeconds();
        this.orderSizePercent = botProperties.orderSizePercent();
        this.cooldownSeconds = botProperties.cooldownSeconds();
        this.minOrderSizeEur = botProperties.minOrderSizeEur();
        this.quoteAsset = botProperties.quoteAsset();
        this.symbols = new ArrayList<>(botProperties.symbols());
        this.autoStart = botProperties.enabled();
    }

    private AbstractSignalCalculationService selectStrategy(StrategyType type) {
        return switch (type) {
            case SMA -> smaService;
            case VORTEX -> vortexService;
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (autoStart) {
            log.info("Auto-starting trading bot...");
            start();
        }
    }

    public void start() {
        lifecycleLock.lock();
        try {
            if (running) {
                log.warn("Bot is already running");
                return;
            }
            for (String symbol : symbols) {
                priceHistory.putIfAbsent(symbol, Collections.synchronizedList(new ArrayList<>()));
                positions.putIfAbsent(symbol, BigDecimal.ZERO);
            }
            running = true;
            startedAt = Instant.now();
            log.info("Trading bot STARTED — strategy={}, symbols={}", strategyType, symbols);
        } finally {
            lifecycleLock.unlock();
        }
    }

    public void stop() {
        lifecycleLock.lock();
        try {
            if (!running) {
                log.warn("Bot is already stopped");
                return;
            }
            running = false;
            startedAt = null;
            log.info("Trading bot STOPPED");
        } finally {
            lifecycleLock.unlock();
        }
    }

    public boolean isRunning() {
        return running;
    }

    @Scheduled(fixedDelayString = "${bot.sampling-interval-seconds:60}000")
    public void scheduledTick() {
        if (!running) {
            return;
        }
        try {
            tick();
        } catch (Exception e) {
            log.error("Bot tick error: {}", e.getMessage(), e);
        }
    }

    void tick() {
        for (String symbol : symbols) {
            try {
                PriceUpdate priceUpdate = marketDataService.getLatestPrice(symbol);
                if (priceUpdate == null) {
                    log.debug("No price data yet for {}", symbol);
                    continue;
                }

                List<BigDecimal> history = priceHistory.computeIfAbsent(
                        symbol, k -> Collections.synchronizedList(new ArrayList<>()));
                history.add(priceUpdate.price());

                while (history.size() > MAX_HISTORY_SIZE) {
                    history.removeFirst();
                }

                evaluateSymbol(symbol);

            } catch (Exception e) {
                log.error("Error processing symbol {}: {}", symbol, e.getMessage(), e);
            }
        }
    }

    void evaluateSymbol(String symbol) {
        List<BigDecimal> history = priceHistory.get(symbol);
        int requiredDataPoints = activeStrategy.getRequiredDataPoints();
        if (history == null || history.size() < requiredDataPoints) {
            log.debug("{}: insufficient data ({}/{} samples)",
                    symbol, history == null ? 0 : history.size(), requiredDataPoints);
            return;
        }

        BigDecimal[] currIndicators = activeStrategy.computeIndicators(history);
        if (currIndicators == null) {
            return;
        }

        BigDecimal[] prev = prevIndicators.get(symbol);
        prevIndicators.put(symbol, currIndicators);

        if (prev == null) {
            log.debug("{}: first {} calculation, waiting for next tick", symbol, strategyType);
            return;
        }

        SignalType signal = activeStrategy.detectSignal(prev, currIndicators);

        if (signal == SignalType.NONE) {
            return;
        }

        log.info("{}: {} signal detected! [{}] fast={}, slow={}",
                symbol, signal, strategyType, currIndicators[0], currIndicators[1]);

        Instant lastTrade = lastTradeTime.get(symbol);
        if (lastTrade != null && Duration.between(lastTrade, Instant.now()).getSeconds() < cooldownSeconds) {
            log.info("{}: {} signal suppressed — cooldown active (last trade {}s ago)",
                    symbol, signal, Duration.between(lastTrade, Instant.now()).getSeconds());
            return;
        }

        BigDecimal currentPrice = history.getLast();

        if (signal == SignalType.BUY) {
            executeBuySignal(symbol, currentPrice);
        } else {
            executeSellSignal(symbol, currentPrice);
        }
    }

    void executeBuySignal(String symbol, BigDecimal currentPrice) {
        BigDecimal quantity = calculateBuyQuantity(symbol, currentPrice);
        if (quantity == null) {
            log.warn("{}: BUY signal skipped — insufficient balance or below min order size", symbol);
            return;
        }

        log.info("{}: executing BUY — qty={}, approxPrice={}", symbol, quantity, currentPrice);

        try {
            PlaceOrderRequest request = new PlaceOrderRequest(
                    symbol, OrderSide.BUY, OrderType.MARKET, quantity, null, null, null);
            OrderResponse response = orderService.placeOrder(request);

            BigDecimal executedQty = response.executedQty() != null ? response.executedQty() : quantity;
            positions.merge(symbol, executedQty, BigDecimal::add);
            lastTradeTime.put(symbol, Instant.now());

            log.info("{}: BUY order filled — binanceOrderId={}, executedQty={}, status={}",
                    symbol, response.binanceOrderId(), executedQty, response.status());

        } catch (Exception e) {
            log.error("{}: BUY order FAILED — {}", symbol, e.getMessage(), e);
        }
    }

    void executeSellSignal(String symbol, BigDecimal currentPrice) {
        BigDecimal quantity = calculateSellQuantity(symbol);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("{}: SELL signal skipped — no position to sell", symbol);
            return;
        }

        BigDecimal orderValueEur = quantity.multiply(currentPrice);
        if (orderValueEur.compareTo(minOrderSizeEur) < 0) {
            log.warn("{}: SELL signal skipped — order value {} EUR below minimum {} EUR",
                    symbol, orderValueEur, minOrderSizeEur);
            return;
        }

        log.info("{}: executing SELL — qty={}, approxPrice={}", symbol, quantity, currentPrice);

        try {
            PlaceOrderRequest request = new PlaceOrderRequest(
                    symbol, OrderSide.SELL, OrderType.MARKET, quantity, null, null, null);
            OrderResponse response = orderService.placeOrder(request);

            BigDecimal executedQty = response.executedQty() != null ? response.executedQty() : quantity;
            positions.merge(symbol, executedQty.negate(), BigDecimal::add);
            lastTradeTime.put(symbol, Instant.now());

            log.info("{}: SELL order filled — binanceOrderId={}, executedQty={}, status={}",
                    symbol, response.binanceOrderId(), executedQty, response.status());

        } catch (Exception e) {
            log.error("{}: SELL order FAILED — {}", symbol, e.getMessage(), e);
        }
    }

    BigDecimal calculateBuyQuantity(String symbol, BigDecimal currentPrice) {
        AccountBalance eurBalance = portfolioService.getBalance(quoteAsset);
        BigDecimal freeEur = eurBalance.free();
        BigDecimal eurToSpend = freeEur.multiply(orderSizePercent);

        if (eurToSpend.compareTo(minOrderSizeEur) < 0) {
            return null;
        }

        BigDecimal quantity = eurToSpend.divide(currentPrice, PRICE_SCALE, RoundingMode.DOWN);
        return quantity.compareTo(BigDecimal.ZERO) > 0 ? quantity : null;
    }

    BigDecimal calculateSellQuantity(String symbol) {
        String cryptoAsset = symbol.replace(quoteAsset, "");
        AccountBalance cryptoBalance = portfolioService.getBalance(cryptoAsset);
        BigDecimal sellQuantity = cryptoBalance.free().multiply(orderSizePercent)
                .setScale(PRICE_SCALE, RoundingMode.DOWN);
        return sellQuantity.compareTo(BigDecimal.ZERO) > 0 ? sellQuantity : null;
    }

    public void updateConfig(BotConfigRequest request) {
        lifecycleLock.lock();
        try {
            if (request.strategy() != null && request.strategy() != this.strategyType) {
                this.strategyType = request.strategy();
                this.activeStrategy = selectStrategy(this.strategyType);
                prevIndicators.clear();
                log.info("Strategy switched to {}", strategyType);
            }
            if (request.shortSmaPeriod() != null || request.longSmaPeriod() != null) {
                int shortP = request.shortSmaPeriod() != null
                        ? request.shortSmaPeriod() : smaService.getShortPeriod();
                int longP = request.longSmaPeriod() != null
                        ? request.longSmaPeriod() : smaService.getLongPeriod();
                if (shortP >= longP) {
                    throw new IllegalArgumentException(
                            "shortSmaPeriod (" + shortP + ") must be less than longSmaPeriod (" + longP + ")");
                }
                smaService.setPeriods(shortP, longP);
            }
            if (request.vortexPeriod() != null) {
                vortexService.setPeriod(request.vortexPeriod());
            }
            if (request.orderSizePercent() != null) {
                this.orderSizePercent = request.orderSizePercent();
            }
            if (request.cooldownSeconds() != null) {
                this.cooldownSeconds = request.cooldownSeconds();
            }
            if (request.minOrderSizeEur() != null) {
                this.minOrderSizeEur = request.minOrderSizeEur();
            }
            if (request.symbols() != null && !request.symbols().isEmpty()) {
                this.symbols = new ArrayList<>(request.symbols());
                for (String sym : this.symbols) {
                    priceHistory.putIfAbsent(sym, Collections.synchronizedList(new ArrayList<>()));
                    positions.putIfAbsent(sym, BigDecimal.ZERO);
                }
            }

            log.info("Bot config updated: strategy={}, orderSize={}%, cooldown={}s",
                    strategyType, orderSizePercent, cooldownSeconds);

        } finally {
            lifecycleLock.unlock();
        }
    }

    public BotStatusResponse getStatus() {
        Map<String, SymbolBotStatus> symbolStatuses = new LinkedHashMap<>();

        for (String symbol : symbols) {
            List<BigDecimal> history = priceHistory.getOrDefault(symbol, List.of());
            BigDecimal[] indicators = activeStrategy.computeIndicators(history);

            PriceUpdate latestPrice = marketDataService.getLatestPrice(symbol);

            String signalStr;
            int requiredDataPoints = activeStrategy.getRequiredDataPoints();
            if (history.size() < requiredDataPoints) {
                signalStr = "INSUFFICIENT_DATA";
            } else if (indicators != null && indicators[0] != null && indicators[1] != null) {
                signalStr = indicators[0].compareTo(indicators[1]) > 0 ? "BULLISH" : "BEARISH";
            } else {
                signalStr = "NONE";
            }

            List<BigDecimal> recentPrices = history.size() <= 10
                    ? List.copyOf(history)
                    : List.copyOf(history.subList(history.size() - 10, history.size()));

            symbolStatuses.put(symbol, new SymbolBotStatus(
                    symbol,
                    latestPrice != null ? latestPrice.price() : null,
                    indicators != null ? indicators[0] : null,
                    indicators != null ? indicators[1] : null,
                    signalStr,
                    history.size(),
                    requiredDataPoints,
                    positions.getOrDefault(symbol, BigDecimal.ZERO),
                    lastTradeTime.get(symbol),
                    recentPrices
            ));
        }

        return new BotStatusResponse(
                running,
                startedAt,
                strategyType,
                smaService.getShortPeriod(),
                smaService.getLongPeriod(),
                vortexService.getPeriod(),
                samplingIntervalSeconds,
                orderSizePercent,
                cooldownSeconds,
                symbolStatuses
        );
    }
}
