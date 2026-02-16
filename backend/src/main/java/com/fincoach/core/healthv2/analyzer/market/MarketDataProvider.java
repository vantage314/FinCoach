package com.fincoach.core.healthv2.analyzer.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Interface for fetching market data.
 */
public interface MarketDataProvider {

    /**
     * Get daily close price for a symbol on a specific date.
     * @param symbol Ticker symbol (e.g. "AAPL.US")
     * @param date Date
     * @return Close price, or null if not found/error
     */
    BigDecimal getDailyClose(String symbol, LocalDate date);

    /**
     * Get series of daily close prices.
     * @param symbol Ticker symbol
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of Date -> Price (only valid points)
     */
    Map<LocalDate, BigDecimal> getDailySeries(String symbol, LocalDate startDate, LocalDate endDate);
}
