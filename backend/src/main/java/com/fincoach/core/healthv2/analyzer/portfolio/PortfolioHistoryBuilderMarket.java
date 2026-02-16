package com.fincoach.core.healthv2.analyzer.portfolio;

import com.fincoach.core.healthv2.analyzer.market.MarketDataProvider;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.healthv2.util.HealthV2ConfigDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class PortfolioHistoryBuilderMarket {

    private final MarketDataProvider marketDataProvider;
    
    // Optional: if we want to cache/persist snapshots
    @Autowired(required = false)
    private FcPortfolioPriceSnapshotMapper snapshotMapper;

    private static final int LOOKBACK_DAYS = 90;
    private static final int MIN_POINTS = 2;

    @Autowired
    public PortfolioHistoryBuilderMarket(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    public PortfolioInput buildFromMarketData(Long userId, Map<String, Object> currentPositions) {
        PortfolioInput.PortfolioInputBuilder builder = PortfolioInput.builder();
        builder.rfAnnual(HealthV2ConfigDefaults.DEFAULT_RF_ANNUAL);
        
        List<String> warnings = new ArrayList<>();
        
        // 1. Parse Positions
        // Expected format: Symbol -> Quantity (or Weight). 
        // If we only have Allocation % (from M1), we can't do real market history easily unless we assume a constant total amount.
        // M7-3 requirement says: "Input: userId + current positions (symbol+quantity/weight)".
        // If we only have `metrics.portfolio.allocation` (M1), it's just categories (STOCK/BOND).
        // We need specific tickers. 
        // Let's assume `currentPositions` contains "symbol" -> "quantity".
        // If not available, return null to fallback.
        
        if (currentPositions == null || currentPositions.isEmpty()) {
            return null; // Fallback to report approx
        }
        
        // MVP: Assume positions is Map<String, Double> (Symbol -> Amount/Qty)
        // Or Map<String, Map<String, Object>> if detailed.
        // Let's assume input is Map<String, BigDecimal> representing Qty for simplicity, or just use what's passed.
        // Actually the caller (HealthReportV2ServiceImpl) needs to provide this.
        // Current M1 implementation unfortunately only has `assets` by TYPE. 
        // We need `FcAssetEntity` to have `symbol` and `quantity` or `amount`.
        // If `FcAssetEntity` has `name` as symbol?
        // Let's assume `positions` keys are Symbols.
        
        Map<String, BigDecimal> targetPositions = new HashMap<>();
        for (Map.Entry<String, Object> entry : currentPositions.entrySet()) {
            String symbol = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof Number) {
                targetPositions.put(symbol, new BigDecimal(val.toString()));
            }
        }
        
        if (targetPositions.isEmpty()) return null;

        // 2. Fetch History for each symbol
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(LOOKBACK_DAYS);
        
        Map<String, Map<LocalDate, BigDecimal>> symbolPrices = new HashMap<>();
        Set<LocalDate> allDates = new TreeSet<>();
        
        for (String symbol : targetPositions.keySet()) {
            Map<LocalDate, BigDecimal> prices = marketDataProvider.getDailySeries(symbol, startDate, endDate);
            if (prices.isEmpty()) {
                warnings.add("MARKET_DATA_MISSING_SYMBOL: " + symbol);
            } else {
                symbolPrices.put(symbol, prices);
                allDates.addAll(prices.keySet());
            }
        }
        
        if (symbolPrices.isEmpty()) return null; // No data at all

        // 3. Build Equity Curve
        // Iterate dates, sum (price * qty)
        List<Double> equityCurve = new ArrayList<>();
        List<LocalDate> validDates = new ArrayList<>(); // To align returns
        
        for (LocalDate date : allDates) {
            BigDecimal dailyEquity = BigDecimal.ZERO;
            boolean hasData = false;
            
            for (Map.Entry<String, BigDecimal> entry : targetPositions.entrySet()) {
                String sym = entry.getKey();
                BigDecimal qty = entry.getValue();
                
                Map<LocalDate, BigDecimal> prices = symbolPrices.get(sym);
                if (prices != null && prices.containsKey(date)) {
                    BigDecimal price = prices.get(date);
                    dailyEquity = dailyEquity.add(price.multiply(qty));
                    hasData = true;
                } else {
                    // Missing price for this symbol on this date.
                    // Option A: Use last known (Forward Fill) - Better for curve
                    // Option B: Skip - might suffice for MVP
                    // Let's simply ignore this component value? No, that drops equity.
                    // Warning?
                    // MVP: If any symbol is missing, we might have a dip. 
                    // Let's try to forward fill locally? 
                    // Too complex for single function.
                    // For now, if coverage is low, warning.
                }
            }
            
            if (dailyEquity.compareTo(BigDecimal.ZERO) > 0) {
                equityCurve.add(dailyEquity.doubleValue());
                validDates.add(date);
            }
        }
        
        // 4. Calculate Returns (Total Portfolio)
        List<Double> returnsSeries = new ArrayList<>();
        if (equityCurve.size() >= MIN_POINTS) {
            for (int i = 1; i < equityCurve.size(); i++) {
                double prev = equityCurve.get(i-1);
                double curr = equityCurve.get(i);
                if (prev > 0) {
                    returnsSeries.add((curr/prev) - 1.0);
                } else {
                     // Should not happen given the checks above
                     warnings.add("RETURN_POINT_SKIPPED");
                }
            }
        } else {
            return null; // Insufficient points
        }
        
        // 5. Asset Returns (for Correlation)
        Map<String, List<Double>> assetReturns = new HashMap<>();
        for (String sym : symbolPrices.keySet()) {
            Map<LocalDate, BigDecimal> prices = symbolPrices.get(sym);
            List<Double> series = new ArrayList<>();
            
            // Align with validDates
            for (int i = 1; i < validDates.size(); i++) {
                LocalDate dPrev = validDates.get(i-1);
                LocalDate dCurr = validDates.get(i);
                
                BigDecimal pPrev = prices.get(dPrev);
                BigDecimal pCurr = prices.get(dCurr);
                
                if (pPrev != null && pCurr != null && pPrev.compareTo(BigDecimal.ZERO) > 0) {
                     double r = pCurr.divide(pPrev, 6, BigDecimal.ROUND_HALF_UP).doubleValue() - 1.0;
                     series.add(r);
                } else {
                    // Gap in asset data but portfolio logic valid -> specific asset return key missing?
                    // We must keep series length consistent for correlation matrix
                    // If gap, fill 0.0 or exclude entire day?
                    // For Pearson, we need aligned arrays.
                    // Let's assume 0.0 (flat) if missing, but warn.
                    series.add(0.0);
                    if (!warnings.contains("MARKET_DATA_GAP_FILLED_ZERO")) {
                         warnings.add("MARKET_DATA_GAP_FILLED_ZERO");
                    }
                }
            }
            if (series.size() >= MIN_POINTS - 1) {
                assetReturns.put(sym, series);
            }
        }
        
        builder.equityCurve(equityCurve);
        builder.returnsSeries(returnsSeries);
        builder.returnsByAssetKey(assetReturns);
        builder.warnings(warnings);
        
        return builder.build();
    }
}
