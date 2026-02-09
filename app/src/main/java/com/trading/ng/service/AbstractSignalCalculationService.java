package com.trading.ng.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public abstract class AbstractSignalCalculationService {

    protected static final int SCALE = 8;
    protected static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Compute indicator values from price history.
     *
     * @param prices list of close prices (most recent last)
     * @return array of [fastLine, slowLine] indicator values, or null if insufficient data
     */
    public abstract BigDecimal[] computeIndicators(List<BigDecimal> prices);

    /**
     * Minimum number of price data points required to compute indicators.
     */
    public abstract int getRequiredDataPoints();

    /**
     * Detect crossover signal between previous and current indicator values.
     * Buy when fast line crosses above slow line, sell when it crosses below.
     *
     * @param prev previous [fastLine, slowLine] values
     * @param curr current [fastLine, slowLine] values
     * @return detected signal type
     */
    public SignalType detectSignal(BigDecimal[] prev, BigDecimal[] curr) {
        if (prev == null || curr == null) {
            return SignalType.NONE;
        }
        if (prev[0] == null || prev[1] == null || curr[0] == null || curr[1] == null) {
            return SignalType.NONE;
        }

        boolean wasBelow = prev[0].compareTo(prev[1]) <= 0;
        boolean isAbove = curr[0].compareTo(curr[1]) > 0;

        boolean wasAbove = prev[0].compareTo(prev[1]) >= 0;
        boolean isBelow = curr[0].compareTo(curr[1]) < 0;

        if (wasBelow && isAbove) {
            return SignalType.BUY;
        }
        if (wasAbove && isBelow) {
            return SignalType.SELL;
        }
        return SignalType.NONE;
    }
}
