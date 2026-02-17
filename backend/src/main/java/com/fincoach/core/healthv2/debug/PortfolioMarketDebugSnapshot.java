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
    private CorrelationMatrixSummary correlationMatrixSummary;
    private CorrelationMatrixData correlationMatrix;
    private ScoreSummary scoreSummary;
    private DebtCashflowSummary debtCashflowSummary;
    private DebtOptimizerSummary debtOptimizerSummary;
    private InsuranceGapSummary insuranceGapSummary;
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
    public CorrelationMatrixSummary getCorrelationMatrixSummary() { return correlationMatrixSummary; }
    public void setCorrelationMatrixSummary(CorrelationMatrixSummary correlationMatrixSummary) { this.correlationMatrixSummary = correlationMatrixSummary; }
    public CorrelationMatrixData getCorrelationMatrix() { return correlationMatrix; }
    public void setCorrelationMatrix(CorrelationMatrixData correlationMatrix) { this.correlationMatrix = correlationMatrix; }
    public ScoreSummary getScoreSummary() { return scoreSummary; }
    public void setScoreSummary(ScoreSummary scoreSummary) { this.scoreSummary = scoreSummary; }
    public DebtCashflowSummary getDebtCashflowSummary() { return debtCashflowSummary; }
    public void setDebtCashflowSummary(DebtCashflowSummary debtCashflowSummary) { this.debtCashflowSummary = debtCashflowSummary; }
    public DebtOptimizerSummary getDebtOptimizerSummary() { return debtOptimizerSummary; }
    public void setDebtOptimizerSummary(DebtOptimizerSummary debtOptimizerSummary) { this.debtOptimizerSummary = debtOptimizerSummary; }
    public InsuranceGapSummary getInsuranceGapSummary() { return insuranceGapSummary; }
    public void setInsuranceGapSummary(InsuranceGapSummary insuranceGapSummary) { this.insuranceGapSummary = insuranceGapSummary; }
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

    public static class CorrelationMatrixSummary {
        private int assetsCount;
        private int sampleSize;
        private List<String> warnings = new ArrayList<>();

        public int getAssetsCount() { return assetsCount; }
        public void setAssetsCount(int assetsCount) { this.assetsCount = assetsCount; }
        public int getSampleSize() { return sampleSize; }
        public void setSampleSize(int sampleSize) { this.sampleSize = sampleSize; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }

    public static class CorrelationMatrixData {
        private List<String> assets = new ArrayList<>();
        private List<List<Double>> matrix = new ArrayList<>();
        private String method;
        private int sampleSize;
        private String startDate;
        private String endDate;

        public List<String> getAssets() { return assets; }
        public void setAssets(List<String> assets) { this.assets = assets; }
        public List<List<Double>> getMatrix() { return matrix; }
        public void setMatrix(List<List<Double>> matrix) { this.matrix = matrix; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public int getSampleSize() { return sampleSize; }
        public void setSampleSize(int sampleSize) { this.sampleSize = sampleSize; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
    }

    public static class ScoreSummary {
        private ScoreSummaryItem risk;
        private ScoreSummaryItem assetHealth;
        private ScoreSummaryItem behavior;

        public ScoreSummaryItem getRisk() { return risk; }
        public void setRisk(ScoreSummaryItem risk) { this.risk = risk; }
        public ScoreSummaryItem getAssetHealth() { return assetHealth; }
        public void setAssetHealth(ScoreSummaryItem assetHealth) { this.assetHealth = assetHealth; }
        public ScoreSummaryItem getBehavior() { return behavior; }
        public void setBehavior(ScoreSummaryItem behavior) { this.behavior = behavior; }
    }

    public static class ScoreSummaryItem {
        private Integer value;
        private String level;
        private List<String> warnings = new ArrayList<>();

        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }

    public static class DebtCashflowSummary {
        private Double dti;
        private Double surplusRate;
        private Double emergencyFundMonths;
        private String stressLevel;
        private int warningsCount;

        public Double getDti() { return dti; }
        public void setDti(Double dti) { this.dti = dti; }
        public Double getSurplusRate() { return surplusRate; }
        public void setSurplusRate(Double surplusRate) { this.surplusRate = surplusRate; }
        public Double getEmergencyFundMonths() { return emergencyFundMonths; }
        public void setEmergencyFundMonths(Double emergencyFundMonths) { this.emergencyFundMonths = emergencyFundMonths; }
        public String getStressLevel() { return stressLevel; }
        public void setStressLevel(String stressLevel) { this.stressLevel = stressLevel; }
        public int getWarningsCount() { return warningsCount; }
        public void setWarningsCount(int warningsCount) { this.warningsCount = warningsCount; }
    }

    public static class DebtOptimizerSummary {
        private String strategy;
        private String topDebtName;
        private Double budgetForExtraPayment;
        private int warningsCount;

        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public String getTopDebtName() { return topDebtName; }
        public void setTopDebtName(String topDebtName) { this.topDebtName = topDebtName; }
        public Double getBudgetForExtraPayment() { return budgetForExtraPayment; }
        public void setBudgetForExtraPayment(Double budgetForExtraPayment) { this.budgetForExtraPayment = budgetForExtraPayment; }
        public int getWarningsCount() { return warningsCount; }
        public void setWarningsCount(int warningsCount) { this.warningsCount = warningsCount; }
    }

    public static class InsuranceGapSummary {
        private Double premiumRatio;
        private String topGapType;
        private Double topGapValue;
        private int warningsCount;

        public Double getPremiumRatio() { return premiumRatio; }
        public void setPremiumRatio(Double premiumRatio) { this.premiumRatio = premiumRatio; }
        public String getTopGapType() { return topGapType; }
        public void setTopGapType(String topGapType) { this.topGapType = topGapType; }
        public Double getTopGapValue() { return topGapValue; }
        public void setTopGapValue(Double topGapValue) { this.topGapValue = topGapValue; }
        public int getWarningsCount() { return warningsCount; }
        public void setWarningsCount(int warningsCount) { this.warningsCount = warningsCount; }
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
