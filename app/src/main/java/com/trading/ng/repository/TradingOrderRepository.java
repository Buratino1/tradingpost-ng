package com.trading.ng.repository;

import com.trading.ng.domain.OrderStatus;
import com.trading.ng.domain.TradingOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradingOrderRepository extends JpaRepository<TradingOrder, Long> {

    Optional<TradingOrder> findByBinanceOrderId(Long binanceOrderId);

    List<TradingOrder> findBySymbolOrderByCreatedAtDesc(String symbol);

    List<TradingOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<TradingOrder> findBySymbolAndStatusOrderByCreatedAtDesc(String symbol, OrderStatus status);
}
