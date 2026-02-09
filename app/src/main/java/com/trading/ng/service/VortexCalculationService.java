package com.trading.ng.service;

import com.trading.ng.config.BotProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vortex Indicator (VI) calculation service.
 * <p>
 * The Vortex Indicator identifies trend direction and strength using two oscillating lines:
 * <ul>
 *   <li>VI+ (positive vortex) — measures upward trend movement</li>
 *   <li>VI- (negative vortex) — measures downward trend movement</li>
 * </ul>
 * <p>
 * Buy signal: VI+ crosses above VI- (bullish crossover).
 * Sell signal: VI- crosses above VI+ (bearish crossover).
 * <p>
 * Since the bot uses close-price samples rather than OHLC bars, synthetic
 * high/low values are derived: High[i] = max(close[i], close[i-1]),
 * Low[i] = min(close[i], close[i-1]). This produces meaningful VM+/VM-
 * divergence and allows crossover detection.
 */
@Service
public class VortexCalculationService extends AbstractSignalCalculationService {

    private volatile int period;

    public VortexCalculationService(BotProperties botProperties) {
        this.period = botProperties.vortexPeriod();
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getPeriod() {
        return period;
    }

    /**
     * Compute Vortex Indicator values [VI+, VI-] from close prices.
     * <p>
     * Requires at least {@code period + 2} data points: {@code period} bars each
     * needing the current close, the previous close (for synthetic high/low),
     * and the close two steps back (for the previous bar's synthetic high/low).
     */
    @Override
    public BigDecimal[] computeIndicators(List<BigDecimal> prices) {
        if (prices == null || prices.size() < period + 2) {
            return null;
        }

        BigDecimal sumVmPlus = BigDecimal.ZERO;
        BigDecimal sumVmMinus = BigDecimal.ZERO;
        BigDecimal sumTr = BigDecimal.ZERO;

        int startIdx = prices.size() - period;

        for (int i = startIdx; i < prices.size(); i++) {
            BigDecimal curr = prices.get(i);
            BigDecimal prev = prices.get(i - 1);
            BigDecimal prev2 = prices.get(i - 2);

            // Synthetic OHLC from close prices
            BigDecimal high = curr.max(prev);
            BigDecimal low = curr.min(prev);
            BigDecimal prevHigh = prev.max(prev2);
            BigDecimal prevLow = prev.min(prev2);

            // VM+ = |High[i] - Low[i-1]|
            BigDecimal vmPlus = high.subtract(prevLow).abs();
            // VM- = |Low[i] - High[i-1]|
            BigDecimal vmMinus = low.subtract(prevHigh).abs();
            // TR = max(High - Low, |High - Close[i-1]|, |Low - Close[i-1]|)
            BigDecimal tr = high.subtract(low)
                    .max(high.subtract(prev).abs())
                    .max(low.subtract(prev).abs());

            sumVmPlus = sumVmPlus.add(vmPlus);
            sumVmMinus = sumVmMinus.add(vmMinus);
            sumTr = sumTr.add(tr);
        }

        if (sumTr.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal viPlus = sumVmPlus.divide(sumTr, SCALE, ROUNDING);
        BigDecimal viMinus = sumVmMinus.divide(sumTr, SCALE, ROUNDING);

        return new BigDecimal[]{viPlus, viMinus};
    }

    @Override
    public int getRequiredDataPoints() {
        return period + 2;
    }
}
