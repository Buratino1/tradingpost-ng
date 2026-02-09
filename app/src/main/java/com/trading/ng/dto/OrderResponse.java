package com.trading.ng.dto;

import com.trading.ng.domain.OrderSide;
import com.trading.ng.domain.OrderStatus;
import com.trading.ng.domain.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long binanceOrderId,
        String symbol,
        OrderSide side,
        OrderType type,
        OrderStatus status,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal executedQty,
        Instant createdAt,
        Instant updatedAt
) {
}
