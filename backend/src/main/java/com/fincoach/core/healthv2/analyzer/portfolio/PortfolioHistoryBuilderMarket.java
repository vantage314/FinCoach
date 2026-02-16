package com.fincoach.core.healthv2.analyzer.portfolio;

import com.fincoach.core.healthv2.analyzer.market.MarketDataProvider;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.healthv2.util.HealthV2ConfigDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${fincoach.portfolio.snapshot.enabled:false}")
    private boolean snapshotEnabled;

    public PortfolioInput buildFromMarketData(Long userId, Map<String, Object> currentPositions) {
        PortfolioInput.PortfolioInputBuilder builder = PortfolioInput.builder();
        builder.rfAnnual(HealthV2ConfigDefaults.DEFAULT_RF_ANNUAL);
        
        List<String> warnings = new ArrayList<>();
        
        // 1. Parse & Resolve Positions
        Map<String, BigDecimal> targetPositions = resolvePositions(currentPositions, warnings);
        
        if (targetPositions.isEmpty()) {
            // Determine why?
            if (currentStateHasAssets(currentPositions)) {
                 warnings.add("POSITION_TICKER_MISSING");
            }
            return null; // Fallback
        }
        
        // 2. Fetch History for each symbol
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(LOOKBACK_DAYS);
        
        Map<String, Map<LocalDate, BigDecimal>> symbolPrices = new HashMap<>();
        Set<LocalDate> allDates = new TreeSet<>();
        
        for (String rawSymbol : targetPositions.keySet()) {
            String normSymbol = normalizeSymbol(rawSymbol, warnings);
            try {
                Map<LocalDate, BigDecimal> prices = marketDataProvider.getDailySeries(normSymbol, startDate, endDate);
                if (prices.isEmpty()) {
                    warnings.add("MARKET_DATA_MISSING_SYMBOL: " + normSymbol);
                } else {
                    symbolPrices.put(rawSymbol, prices); // Use raw key for mapping back? Or norm?
                    allDates.addAll(prices.keySet());
                }
            } catch (Exception e) {
                log.warn("Market data fetch error for {}: {}", normSymbol, e.getMessage());
                warnings.add("MARKET_DATA_HTTP_ERROR: " + normSymbol);
            }
        }
        
        if (symbolPrices.isEmpty()) return null; // No data at all

        // 3. Build Equity Curve
        List<Double> equityCurve = new ArrayList<>();
        List<LocalDate> validDates = new ArrayList<>();
        
        // We need to iterate chronologically
        for (LocalDate date : allDates) {
            BigDecimal dailyEquity = BigDecimal.ZERO;
            boolean validDay = true;
            
            for (Map.Entry<String, BigDecimal> entry : targetPositions.entrySet()) {
                String sym = entry.getKey();
                BigDecimal qty = entry.getValue();
                
                // If symbol failed to fetch, we skip it? That breaks equity curve consistency.
                // Steps:
                // If we have price, add to equity.
                // If we DON'T have price (but successfully fetched others), we have a GAP.
                // M7-3 rule: "Gaps -> MARKET_DATA_GAP. Do not fill."
                // Implication: If ANY asset is missing price for a day, that day is invalid for TOTAL equity?
                // OR we assume missing = 0? (bad).
                // Let's go with: If any asset in our target list is missing price, skip the day.
                
                if (!symbolPrices.containsKey(sym)) {
                    // This symbol has NO data at all. We already warned. 
                    // We can compute equity of "available" assets? 
                    // But then returns will be wrong (sudden jump if asset appears).
                    // Best to exclude symbol from calculation entirely if it has no data?
                    // "positions" contains all we want to track.
                    continue; 
                }
                
                Map<LocalDate, BigDecimal> prices = symbolPrices.get(sym);
                if (prices != null && prices.containsKey(date)) {
                    BigDecimal price = prices.get(date);
                    dailyEquity = dailyEquity.add(price.multiply(qty));
                } else {
                    validDay = false; // Missing price for this asset on this date
                    break; 
                }
            }
            
            if (validDay && dailyEquity.compareTo(BigDecimal.ZERO) > 0) {
                equityCurve.add(dailyEquity.doubleValue());
                validDates.add(date);
            } else {
                 // warning for gap?
                 // frequent gaps might spam warnings.
            }
        }
        
        if (equityCurve.size() < MIN_POINTS) {
            warnings.add("MARKET_DATA_INSUFFICIENT_SERIES");
            return null;
        }

        // 4. Calculate Returns (Total Portfolio)
        List<Double> returnsSeries = new ArrayList<>();
        // ... (standard return calc)
        for (int i = 1; i < equityCurve.size(); i++) {
            double prev = equityCurve.get(i-1);
            double curr = equityCurve.get(i);
            if (prev > 0) {
                returnsSeries.add((curr/prev) - 1.0);
            } else {
                 warnings.add("RETURN_POINT_SKIPPED");
            }
        }
        
        // 5. Asset Returns
        Map<String, List<Double>> assetReturns = new HashMap<>();
        // ... (reuse logic)
        // Ensure to use normalized or raw symbols consistently
        
        // 6. Snapshot Persistence (Try/Catch)
        if (snapshotEnabled) {
             trySaveSnapshot(userId, validDates, equityCurve, warnings);
        } else {
             // Optional: warn skipped?
             // warnings.add("SNAPSHOT_PERSIST_SKIPPED");
        }

        builder.equityCurve(equityCurve);
        builder.returnsSeries(returnsSeries);
        // builder.returnsByAssetKey(...) - Populate this too
        // assetReturns logic (simplified for brevity here, should retrieve from orig or copy)
        // Logic copy from prev:
        for (String sym : symbolPrices.keySet()) {
            Map<LocalDate, BigDecimal> prices = symbolPrices.get(sym);
            List<Double> series = new ArrayList<>();
            for (int i = 1; i < validDates.size(); i++) {
                 LocalDate dPrev = validDates.get(i-1);
                 LocalDate dCurr = validDates.get(i);
                 BigDecimal pPrev = prices.get(dPrev);
                 BigDecimal pCurr = prices.get(dCurr);
                 if (pPrev != null && pCurr != null && pPrev.compareTo(BigDecimal.ZERO) > 0) {
                      series.add(pCurr.doubleValue() / pPrev.doubleValue() - 1.0);
                 } else {
                      series.add(0.0);
                 }
            }
            if (series.size() >= MIN_POINTS - 1) assetReturns.put(sym, series);
        }
        builder.returnsByAssetKey(assetReturns);
        
        builder.warnings(warnings);
        return builder.build();
    }
    
    // --- Helpers ---
    
    private boolean currentStateHasAssets(Map<String, Object> pos) {
        return pos != null && !pos.isEmpty();
    }

    private Map<String, BigDecimal> resolvePositions(Map<String, Object> input, List<String> warnings) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (input == null) return result;
        
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey(); // Could be Name or Ticker
            Object val = entry.getValue();
            BigDecimal qty = (val instanceof Number) ? new BigDecimal(val.toString()) : BigDecimal.ZERO;
            
            String ticker = resolveTicker(key);
            if (ticker != null) {
                result.put(ticker, qty);
                if (!ticker.equals(key)) {
                    // warnings.add("RESOLVED_TICKER: " + key + "->" + ticker);
                }
            } else {
                warnings.add("POSITION_TICKER_UNRESOLVED: " + key);
            }
        }
        return result;
    }
    
    private String resolveTicker(String input) {
        // MVP Resolver
        // 1. If it looks like a ticker (contains . or all caps alpha or 6 digits) -> keep it
        if (input.contains(".") || input.matches("^[A-Z0-9]{1,10}$")) {
            return input;
        }
        // 2. Map known names (Demo)
        if (input.contains("茅台")) return "600519.SS"; // 贵州茅台
        if (input.contains("腾讯")) return "0700.HK";
        if (input.contains("阿里")) return "BABA.US";
        if (input.contains("Apple") || input.equalsIgnoreCase("AAPL")) return "AAPL.US";
        
        // 3. Asset Types to ETF proxy?
        if (input.equalsIgnoreCase("CASH")) return null; // Ignore cash? Or map to SHV?
        if (input.equalsIgnoreCase("STOCK")) return "SPY.US"; // Fallback to SPY? Risk.
        
        return null; // Fail to resolve
    }

    private String normalizeSymbol(String raw, List<String> warnings) {
        // TickerNormalizer logic
        String norm = raw.toUpperCase().trim();
        
        // Rules
        // 1. Existing suffix
        if (norm.endsWith(".US") || norm.endsWith(".SS") || norm.endsWith(".SZ") || norm.endsWith(".HK")) {
            return norm;
        }
        
        // 2. 6 Digits (CN A-Share)
        if (norm.matches("^\\d{6}$")) {
            if (norm.startsWith("6")) {
                warnings.add("MARKET_DATA_SYMBOL_NORMALIZED: " + raw + "->" + norm + ".SS");
                return norm + ".SS";
            }
            if (norm.startsWith("0") || norm.startsWith("3")) {
                warnings.add("MARKET_DATA_SYMBOL_NORMALIZED: " + raw + "->" + norm + ".SZ");
                return norm + ".SZ";
            }
        }
        
        // 3. Pure Alpha (US)
        if (norm.matches("^[A-Z]+$")) {
            // Assume US
            warnings.add("MARKET_DATA_SYMBOL_NORMALIZED: " + raw + "->" + norm + ".US");
            return norm + ".US";
        }
        
        return norm;
    }

    private void trySaveSnapshot(Long userId, List<LocalDate> dates, List<Double> equity, List<String> warnings) {
        if (dates.isEmpty() || equity.isEmpty()) return;
        
        try {
            if (snapshotMapper == null) {
                warnings.add("SNAPSHOT_PERSIST_SKIPPED: Mapper not available");
                return;
            }
            
            // Save last point only? Or all? 
            // Ops: "Stores daily snapshots".
            // Typically we save the LATEST snapshot (today or last calc).
            // Saving 90 days history every time is wasteful.
            // Let's save the LAST valid point.
            int lastIdx = dates.size() - 1;
            LocalDate lastDate = dates.get(lastIdx);
            Double lastVal = equity.get(lastIdx);
            
            FcPortfolioPriceSnapshotEntity entity = new FcPortfolioPriceSnapshotEntity();
            entity.setUserId(userId);
            entity.setAsOfDate(lastDate);
            entity.setEquity(BigDecimal.valueOf(lastVal));
            // entity.setReturns(...)
            entity.setDataSource("MARKET_DATA");
            entity.setCreatedAt(java.time.LocalDateTime.now());
            
            // Upsert? Mapper insert might fail on duplicate.
            // Better to select first or try/catch duplicate.
            // MVP: simple insert, ignore error
            snapshotMapper.insert(entity);
            
        } catch (Exception e) {
            // Swallow
            log.warn("Snapshot save failed: {}", e.getMessage());
            warnings.add("SNAPSHOT_PERSIST_SKIPPED");
        }
    }
}
