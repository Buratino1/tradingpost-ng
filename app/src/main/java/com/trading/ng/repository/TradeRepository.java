package com.trading.ng.repository;

import com.trading.ng.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findBySymbolOrderByTradeTimeDesc(String symbol);

    List<Trade> findByBinanceOrderId(Long binanceOrderId);

    List<Trade> findBySymbolAndTradeTimeBetweenOrderByTradeTimeDesc(
            String symbol, Instant from, Instant to);
}
