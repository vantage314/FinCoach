package com.fincoach.core.healthv2.analyzer.portfolio;

import com.fincoach.core.healthv2.debug.PortfolioDebugContextHolder;
import com.fincoach.core.healthv2.debug.PortfolioMarketDebugSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class PortfolioHistoryFacade {

    private static final Logger log = LoggerFactory.getLogger(PortfolioHistoryFacade.class);

    public static final String WARN_SNAPSHOT_PREFER_CACHE_ENABLED = "SNAPSHOT_PREFER_CACHE_ENABLED";
    public static final String WARN_SNAPSHOT_PERSIST_SKIPPED = "SNAPSHOT_PERSIST_SKIPPED";

    @Autowired
    private PortfolioHistoryBuilderMarket marketBuilder;
    
    @Autowired
    private PortfolioHistoryBuilder reportBuilder;

    @Autowired
    private PortfolioHistoryBuilderSnapshot snapshotBuilder;

    @Value("${fincoach.portfolio.snapshot.preferCache:false}")
    private boolean preferCache;

    @Value("${fincoach.portfolio.snapshot.lookbackDays:90}")
    private int lookbackDays;

    @Value("${fincoach.portfolio.snapshot.enabled:false}")
    private boolean snapshotEnabled;

    /**
     * Build input trying Cache -> Market Data -> Report History.
     */
    public FacadeResult build(Long userId, BigDecimal netWorth, Map<String, Object> allocation, Map<String, Object> positions) {
        PortfolioInput input = null;
        String source = "UNKNOWN";
        List<String> facadeWarnings = new ArrayList<>();
        PortfolioMarketDebugSnapshot debug = initDebug();
        List<String> historyPath = debug.getHistoryPath();
        String fallbackReason = null;
        
        // 0. Prefer Cache
        if (preferCache) {
            facadeWarnings.add(WARN_SNAPSHOT_PREFER_CACHE_ENABLED);
            historyPath.add("CACHE");
            try {
                PortfolioInput cached = snapshotBuilder.buildFromSnapshots(userId, lookbackDays);
                mergeWarnings(facadeWarnings, cached != null ? cached.getWarnings() : null);
                fillCacheDebug(debug, cached);
                if (cached != null && cached.getReturnsSeries() != null && cached.getReturnsSeries().size() >= 2) {
                    source = "SNAPSHOT_CACHE";
                    mergeWarnings(cached, facadeWarnings);
                    debug.setHistorySource(source);
                    debug.setWarnings(new ArrayList<>(cached.getWarnings()));
                    debug.setHistoryPath(historyPath);
                    fillFallbackDebug(debug, fallbackReason);
                    fillMarketDebug(debug, null, positions);
                    return new FacadeResult(cached, source);
                }
                if (fallbackReason == null) {
                    fallbackReason = PortfolioHistoryBuilderSnapshot.WARN_CACHE_MISS;
                }
            } catch (Exception e) {
                facadeWarnings.add(PortfolioHistoryBuilderSnapshot.WARN_DB_UNAVAILABLE);
                log.warn("[PortfolioFacade] Snapshot builder failed: {}", e.getMessage());
                fillCacheDebug(debug, null);
                if (fallbackReason == null) {
                    fallbackReason = PortfolioHistoryBuilderSnapshot.WARN_DB_UNAVAILABLE;
                }
            }
        } else {
            fillCacheDebug(debug, null);
        }

        // 1. Try Market Data
        historyPath.add("MARKET");
        try {
            input = marketBuilder.buildFromMarketData(userId, positions);
            if (input != null && input.getReturnsSeries() != null && input.getReturnsSeries().size() >= 2) {
                source = "MARKET_DATA_DAILY_CLOSE";
                if (!snapshotEnabled) {
                    if (input.getWarnings() == null || !input.getWarnings().contains(WARN_SNAPSHOT_PERSIST_SKIPPED)) {
                        facadeWarnings.add(WARN_SNAPSHOT_PERSIST_SKIPPED);
                    }
                }
                mergeWarnings(input, facadeWarnings);
                debug.setHistorySource(source);
                debug.setWarnings(input.getWarnings() == null ? Collections.emptyList() : new ArrayList<>(input.getWarnings()));
                debug.setHistoryPath(historyPath);
                fillFallbackDebug(debug, fallbackReason);
                fillMarketDebug(debug, input, positions);
                return new FacadeResult(input, source);
            } else {
                input = null; // Insufficient data
            }
        } catch (Exception e) {
            log.warn("[PortfolioFacade] Market builder failed: {}", e.getMessage());
            input = null;
        }
        
        // 2. Fallback to Report History
        if (input == null) {
            historyPath.add("REPORT");
            try {
                input = reportBuilder.buildFromRecentReports(userId, netWorth, allocation);
                source = "REPORT_NET_WORTH_APPROX";
                if (input != null) {
                    if (input.getWarnings() == null) {
                        input.setWarnings(new ArrayList<>());
                    }
                    input.getWarnings().add("MARKET_DATA_FALLBACK_TO_REPORT_APPROX");
                }
                mergeWarnings(input, facadeWarnings);
                fallbackReason = "MARKET_DATA_FALLBACK_TO_REPORT_APPROX";
                debug.setHistorySource(source);
                debug.setWarnings(input == null || input.getWarnings() == null ? Collections.emptyList() : new ArrayList<>(input.getWarnings()));
                debug.setHistoryPath(historyPath);
                fillFallbackDebug(debug, fallbackReason);
                fillMarketDebug(debug, input, positions);
            } catch (Exception e) {
                log.error("[PortfolioFacade] Report builder failed: {}", e.getMessage());
            }
        }
        
        return new FacadeResult(input, source);
    }

    private PortfolioMarketDebugSnapshot initDebug() {
        PortfolioMarketDebugSnapshot snapshot = new PortfolioMarketDebugSnapshot();
        snapshot.setGeneratedAt(Instant.now());
        snapshot.setHistoryPath(new ArrayList<>());
        snapshot.setResolvedTickers(new ArrayList<>());
        snapshot.setWarnings(new ArrayList<>());
        PortfolioDebugContextHolder.set(snapshot);
        return snapshot;
    }

    private void fillCacheDebug(PortfolioMarketDebugSnapshot debug, PortfolioInput cached) {
        if (debug == null) return;
        PortfolioMarketDebugSnapshot.CacheDebug cache = new PortfolioMarketDebugSnapshot.CacheDebug();
        cache.setPreferCache(preferCache);
        cache.setLookbackDays(lookbackDays);
        int snapshotsCount = 0;
        if (cached != null && cached.getEquityCurve() != null) {
            snapshotsCount = cached.getEquityCurve().size();
        }
        cache.setSnapshotsCount(snapshotsCount);
        boolean hit = cached != null && cached.getReturnsSeries() != null && cached.getReturnsSeries().size() >= 2;
        cache.setHit(hit);
        if (!hit) {
            String missReason = null;
            List<String> warnings = cached != null ? cached.getWarnings() : null;
            if (warnings != null) {
                if (warnings.contains(PortfolioHistoryBuilderSnapshot.WARN_DB_UNAVAILABLE)) {
                    missReason = PortfolioHistoryBuilderSnapshot.WARN_DB_UNAVAILABLE;
                } else if (warnings.contains(PortfolioHistoryBuilderSnapshot.WARN_CACHE_MISS)) {
                    missReason = PortfolioHistoryBuilderSnapshot.WARN_CACHE_MISS;
                }
            }
            cache.setMissReason(missReason);
        }
        debug.setCache(cache);
    }

    private void fillMarketDebug(PortfolioMarketDebugSnapshot debug, PortfolioInput input, Map<String, Object> positions) {
        if (debug == null) return;
        PortfolioMarketDebugSnapshot.MarketFetchDebug marketFetch = new PortfolioMarketDebugSnapshot.MarketFetchDebug();
        List<String> failedSymbols = extractMarketFailedSymbols(input == null ? null : input.getWarnings());
        int requested = 0;
        if (debug.getResolvedTickers() != null && !debug.getResolvedTickers().isEmpty()) {
            requested = debug.getResolvedTickers().size();
        } else if (positions != null) {
            requested = positions.size();
        }
        marketFetch.setSymbolsRequested(requested);
        marketFetch.setFailedSymbols(failedSymbols);
        marketFetch.setSymbolsFailed(failedSymbols.size());
        marketFetch.setSymbolsSucceeded(Math.max(0, requested - failedSymbols.size()));
        marketFetch.setProvider("Stooq");
        if (requested == 0) {
            marketFetch.setNotes("not attempted");
        } else if (failedSymbols.isEmpty()) {
            marketFetch.setNotes("all success");
        } else {
            marketFetch.setNotes("partial success");
        }
        debug.setMarketFetch(marketFetch);
    }

    private void fillFallbackDebug(PortfolioMarketDebugSnapshot debug, String fallbackReason) {
        if (debug == null) return;
        PortfolioMarketDebugSnapshot.FallbackDebug fallback = new PortfolioMarketDebugSnapshot.FallbackDebug();
        boolean isFallback = fallbackReason != null && !fallbackReason.isBlank();
        fallback.setFallback(isFallback);
        fallback.setReason(fallbackReason);
        fallback.setWarning(fallbackReason);
        debug.setFallback(fallback);
    }

    private List<String> extractMarketFailedSymbols(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) return new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (String w : warnings) {
            if (w == null) continue;
            if (w.startsWith("MARKET_DATA_HTTP_ERROR: ")) {
                failed.add(w.substring("MARKET_DATA_HTTP_ERROR: ".length()));
            } else if (w.startsWith("MARKET_DATA_MISSING_SYMBOL: ")) {
                failed.add(w.substring("MARKET_DATA_MISSING_SYMBOL: ".length()));
            }
        }
        return failed;
    }

    private void mergeWarnings(PortfolioInput input, List<String> warnings) {
        if (input == null || warnings == null || warnings.isEmpty()) return;
        if (input.getWarnings() == null) {
            input.setWarnings(new ArrayList<>());
        }
        for (String w : warnings) {
            if (!input.getWarnings().contains(w)) {
                input.getWarnings().add(w);
            }
        }
    }

    private void mergeWarnings(List<String> target, List<String> source) {
        if (target == null || source == null) return;
        for (String w : source) {
            if (!target.contains(w)) {
                target.add(w);
            }
        }
    }

    void setMarketBuilder(PortfolioHistoryBuilderMarket marketBuilder) {
        this.marketBuilder = marketBuilder;
    }

    void setReportBuilder(PortfolioHistoryBuilder reportBuilder) {
        this.reportBuilder = reportBuilder;
    }

    void setSnapshotBuilder(PortfolioHistoryBuilderSnapshot snapshotBuilder) {
        this.snapshotBuilder = snapshotBuilder;
    }

    void setPreferCache(boolean preferCache) {
        this.preferCache = preferCache;
    }

    void setLookbackDays(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    void setSnapshotEnabled(boolean snapshotEnabled) {
        this.snapshotEnabled = snapshotEnabled;
    }
    
    public static class FacadeResult {
        public PortfolioInput input;
        public String source;
        
        public FacadeResult(PortfolioInput input, String source) {
            this.input = input;
            this.source = source;
        }
    }
}
