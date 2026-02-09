package com.trading.ng.dto;

import com.trading.ng.domain.OrderSide;
import com.trading.ng.domain.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank String symbol,
        @NotNull OrderSide side,
        @NotNull OrderType type,
        @NotNull @Positive BigDecimal quantity,
        BigDecimal price,
        BigDecimal stopPrice,
        String timeInForce
) {
}
