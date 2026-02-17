package com.fincoach.core.healthv2.dto.admin;

import java.util.ArrayList;
import java.util.List;

public class AdminMarketDebugLatestDTO {
    private String requestId;
    private long timestamp;
    private Long userId;
    private boolean enabled;
    private List<ResolvedTickerDTO> resolvedTickers = new ArrayList<>();
    private HistoryDTO history;
    private CacheDTO cache;
    private MarketFetchDTO marketFetch;
    private FallbackDTO fallback;
    private CorrelationDTO correlation;
    private List<String> warnings = new ArrayList<>();

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<ResolvedTickerDTO> getResolvedTickers() { return resolvedTickers; }
    public void setResolvedTickers(List<ResolvedTickerDTO> resolvedTickers) { this.resolvedTickers = resolvedTickers; }
    public HistoryDTO getHistory() { return history; }
    public void setHistory(HistoryDTO history) { this.history = history; }
    public CacheDTO getCache() { return cache; }
    public void setCache(CacheDTO cache) { this.cache = cache; }
    public MarketFetchDTO getMarketFetch() { return marketFetch; }
    public void setMarketFetch(MarketFetchDTO marketFetch) { this.marketFetch = marketFetch; }
    public FallbackDTO getFallback() { return fallback; }
    public void setFallback(FallbackDTO fallback) { this.fallback = fallback; }
    public CorrelationDTO getCorrelation() { return correlation; }
    public void setCorrelation(CorrelationDTO correlation) { this.correlation = correlation; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public static class ResolvedTickerDTO {
        private String keyword;
        private String normalizedKey;
        private String ticker;
        private String source;
        private Integer priority;
        private List<String> warnings = new ArrayList<>();

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public String getNormalizedKey() { return normalizedKey; }
        public void setNormalizedKey(String normalizedKey) { this.normalizedKey = normalizedKey; }
        public String getTicker() { return ticker; }
        public void setTicker(String ticker) { this.ticker = ticker; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }

    public static class HistoryDTO {
        private String source;
        private List<String> path = new ArrayList<>();
        private int points;
        private int returnsPoints;

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public List<String> getPath() { return path; }
        public void setPath(List<String> path) { this.path = path; }
        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }
        public int getReturnsPoints() { return returnsPoints; }
        public void setReturnsPoints(int returnsPoints) { this.returnsPoints = returnsPoints; }
    }

    public static class CacheDTO {
        private boolean preferCache;
        private boolean enabled;
        private boolean hit;
        private String missReason;
        private int snapshotsCount;
        private int lookbackDays;

        public boolean isPreferCache() { return preferCache; }
        public void setPreferCache(boolean preferCache) { this.preferCache = preferCache; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isHit() { return hit; }
        public void setHit(boolean hit) { this.hit = hit; }
        public String getMissReason() { return missReason; }
        public void setMissReason(String missReason) { this.missReason = missReason; }
        public int getSnapshotsCount() { return snapshotsCount; }
        public void setSnapshotsCount(int snapshotsCount) { this.snapshotsCount = snapshotsCount; }
        public int getLookbackDays() { return lookbackDays; }
        public void setLookbackDays(int lookbackDays) { this.lookbackDays = lookbackDays; }
    }

    public static class MarketFetchDTO {
        private String provider;
        private List<String> calledSymbols = new ArrayList<>();
        private List<String> succeededSymbols = new ArrayList<>();
        private List<String> failedSymbols = new ArrayList<>();
        private String errorSummary;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public List<String> getCalledSymbols() { return calledSymbols; }
        public void setCalledSymbols(List<String> calledSymbols) { this.calledSymbols = calledSymbols; }
        public List<String> getSucceededSymbols() { return succeededSymbols; }
        public void setSucceededSymbols(List<String> succeededSymbols) { this.succeededSymbols = succeededSymbols; }
        public List<String> getFailedSymbols() { return failedSymbols; }
        public void setFailedSymbols(List<String> failedSymbols) { this.failedSymbols = failedSymbols; }
        public String getErrorSummary() { return errorSummary; }
        public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
    }

    public static class FallbackDTO {
        private boolean used;
        private String why;
        private Integer reportLookbackDays;

        public boolean isUsed() { return used; }
        public void setUsed(boolean used) { this.used = used; }
        public String getWhy() { return why; }
        public void setWhy(String why) { this.why = why; }
        public Integer getReportLookbackDays() { return reportLookbackDays; }
        public void setReportLookbackDays(Integer reportLookbackDays) { this.reportLookbackDays = reportLookbackDays; }
    }

    public static class CorrelationDTO {
        private int minPoints;
        private int effectivePoints;
        private String alignmentMode;
        private boolean matrixEmitted;

        public int getMinPoints() { return minPoints; }
        public void setMinPoints(int minPoints) { this.minPoints = minPoints; }
        public int getEffectivePoints() { return effectivePoints; }
        public void setEffectivePoints(int effectivePoints) { this.effectivePoints = effectivePoints; }
        public String getAlignmentMode() { return alignmentMode; }
        public void setAlignmentMode(String alignmentMode) { this.alignmentMode = alignmentMode; }
        public boolean isMatrixEmitted() { return matrixEmitted; }
        public void setMatrixEmitted(boolean matrixEmitted) { this.matrixEmitted = matrixEmitted; }
    }
}
