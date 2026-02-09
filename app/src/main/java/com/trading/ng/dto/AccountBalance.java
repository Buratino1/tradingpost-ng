package com.trading.ng.dto;

import java.math.BigDecimal;

public record AccountBalance(
        String asset,
        BigDecimal free,
        BigDecimal locked
) {
}
