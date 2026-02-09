package com.trading.ng.service;

import com.trading.ng.config.BotProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SmaCalculationService extends AbstractSignalCalculationService {

    private volatile int shortPeriod;
    private volatile int longPeriod;

    public SmaCalculationService(BotProperties botProperties) {
        this.shortPeriod = botProperties.shortSmaPeriod();
        this.longPeriod = botProperties.longSmaPeriod();
    }

    public void setPeriods(int shortPeriod, int longPeriod) {
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
    }

    public int getShortPeriod() {
        return shortPeriod;
    }

    public int getLongPeriod() {
        return longPeriod;
    }

    @Override
    public BigDecimal[] computeIndicators(List<BigDecimal> prices) {
        BigDecimal shortSma = calculateSma(prices, shortPeriod);
        BigDecimal longSma = calculateSma(prices, longPeriod);
        if (shortSma == null || longSma == null) {
            return null;
        }
        return new BigDecimal[]{shortSma, longSma};
    }

    @Override
    public int getRequiredDataPoints() {
        return longPeriod;
    }

    public BigDecimal calculateSma(List<BigDecimal> prices, int period) {
        if (prices == null || prices.size() < period) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int startIndex = prices.size() - period;
        for (int i = startIndex; i < prices.size(); i++) {
            sum = sum.add(prices.get(i));
        }
        return sum.divide(BigDecimal.valueOf(period), SCALE, ROUNDING);
    }
}
