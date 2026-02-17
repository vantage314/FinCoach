package com.fincoach.core.healthv2.debug;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PortfolioMarketDebugSnapshot {

    private List<ResolvedTickerDebug> resolvedTickers = new ArrayList<>();
    private String historySource;
    private List<String> historyPath = new ArrayList<>();
    private CacheDebug cache;
    private MarketFetchDebug marketFetch;
    private FallbackDebug fallback;
    private CorrelationDebug correlation;
    private List<String> warnings = new ArrayList<>();
    private Instant generatedAt;

    public List<ResolvedTickerDebug> getResolvedTickers() { return resolvedTickers; }
    public void setResolvedTickers(List<ResolvedTickerDebug> resolvedTickers) { this.resolvedTickers = resolvedTickers; }
    public String getHistorySource() { return historySource; }
    public void setHistorySource(String historySource) { this.historySource = historySource; }
    public List<String> getHistoryPath() { return historyPath; }
    public void setHistoryPath(List<String> historyPath) { this.historyPath = historyPath; }
    public CacheDebug getCache() { return cache; }
    public void setCache(CacheDebug cache) { this.cache = cache; }
    public MarketFetchDebug getMarketFetch() { return marketFetch; }
    public void setMarketFetch(MarketFetchDebug marketFetch) { this.marketFetch = marketFetch; }
    public FallbackDebug getFallback() { return fallback; }
    public void setFallback(FallbackDebug fallback) { this.fallback = fallback; }
    public CorrelationDebug getCorrelation() { return correlation; }
    public void setCorrelation(CorrelationDebug correlation) { this.correlation = correlation; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public static class ResolvedTickerDebug {
        private String input;
        private String normalizedKey;
        private String resolvedTicker;
        private String mappingSource;
        private List<String> warnings;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public String getNormalizedKey() { return normalizedKey; }
        public void setNormalizedKey(String normalizedKey) { this.normalizedKey = normalizedKey; }
        public String getResolvedTicker() { return resolvedTicker; }
        public void setResolvedTicker(String resolvedTicker) { this.resolvedTicker = resolvedTicker; }
        public String getMappingSource() { return mappingSource; }
        public void setMappingSource(String mappingSource) { this.mappingSource = mappingSource; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }

    public static class CacheDebug {
        private boolean preferCache;
        private int lookbackDays;
        private int snapshotsCount;
        private boolean hit;
        private String missReason;

        public boolean isPreferCache() { return preferCache; }
        public void setPreferCache(boolean preferCache) { this.preferCache = preferCache; }
        public int getLookbackDays() { return lookbackDays; }
        public void setLookbackDays(int lookbackDays) { this.lookbackDays = lookbackDays; }
        public int getSnapshotsCount() { return snapshotsCount; }
        public void setSnapshotsCount(int snapshotsCount) { this.snapshotsCount = snapshotsCount; }
        public boolean isHit() { return hit; }
        public void setHit(boolean hit) { this.hit = hit; }
        public String getMissReason() { return missReason; }
        public void setMissReason(String missReason) { this.missReason = missReason; }
    }

    public static class MarketFetchDebug {
        private int symbolsRequested;
        private int symbolsSucceeded;
        private int symbolsFailed;
        private List<String> failedSymbols;
        private String provider;
        private String notes;

        public int getSymbolsRequested() { return symbolsRequested; }
        public void setSymbolsRequested(int symbolsRequested) { this.symbolsRequested = symbolsRequested; }
        public int getSymbolsSucceeded() { return symbolsSucceeded; }
        public void setSymbolsSucceeded(int symbolsSucceeded) { this.symbolsSucceeded = symbolsSucceeded; }
        public int getSymbolsFailed() { return symbolsFailed; }
        public void setSymbolsFailed(int symbolsFailed) { this.symbolsFailed = symbolsFailed; }
        public List<String> getFailedSymbols() { return failedSymbols; }
        public void setFailedSymbols(List<String> failedSymbols) { this.failedSymbols = failedSymbols; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class CorrelationDebug {
        private String alignedMode;
        private int effectivePoints;
        private int minPoints;
        private boolean matrixEmitted;
        private int maxCandidatePoints;
        private double gapRatio;

        public String getAlignedMode() { return alignedMode; }
        public void setAlignedMode(String alignedMode) { this.alignedMode = alignedMode; }
        public int getEffectivePoints() { return effectivePoints; }
        public void setEffectivePoints(int effectivePoints) { this.effectivePoints = effectivePoints; }
        public int getMinPoints() { return minPoints; }
        public void setMinPoints(int minPoints) { this.minPoints = minPoints; }
        public boolean isMatrixEmitted() { return matrixEmitted; }
        public void setMatrixEmitted(boolean matrixEmitted) { this.matrixEmitted = matrixEmitted; }
        public int getMaxCandidatePoints() { return maxCandidatePoints; }
        public void setMaxCandidatePoints(int maxCandidatePoints) { this.maxCandidatePoints = maxCandidatePoints; }
        public double getGapRatio() { return gapRatio; }
        public void setGapRatio(double gapRatio) { this.gapRatio = gapRatio; }
    }

    public static class FallbackDebug {
        private boolean fallback;
        private String reason;
        private String warning;

        public boolean isFallback() { return fallback; }
        public void setFallback(boolean fallback) { this.fallback = fallback; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getWarning() { return warning; }
        public void setWarning(String warning) { this.warning = warning; }
    }
}
