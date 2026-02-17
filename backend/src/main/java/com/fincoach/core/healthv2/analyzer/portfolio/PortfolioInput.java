package com.fincoach.core.healthv2.analyzer.portfolio;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public class PortfolioInput {
    /**
     * Annual risk-free rate (e.g., 0.02 for 2%)
     * Can be null, analyzer will use default or 0
     */
    private Double rfAnnual;

    /**
     * Historical returns series (e.g., daily or monthly returns)
     * Ordered by time asc
     */
    private List<Double> returnsSeries;

    /**
     * Historical equity curve (net value)
     * Ordered by time asc, used for MaxDrawdown
     */
    private List<Double> equityCurve;

    /**
     * Returns series by asset key (e.g., "STOCK", "GOLD")
     * Used for Correlation Matrix
     */
    private Map<String, List<Double>> returnsByAssetKey;

    /**
     * Returns series by asset key with dates (symbol -> date -> return)
     * Used for correlation alignment
     */
    private Map<String, NavigableMap<LocalDate, Double>> returnsByAssetDate;

    /**
     * Warnings collected during input building (e.g. data gaps)
     */
    private List<String> warnings;

    public PortfolioInput() {}

    @Builder
    public PortfolioInput(Double rfAnnual,
                          List<Double> returnsSeries,
                          List<Double> equityCurve,
                          Map<String, List<Double>> returnsByAssetKey,
                          Map<String, NavigableMap<LocalDate, Double>> returnsByAssetDate,
                          List<String> warnings) {
        this.rfAnnual = rfAnnual;
        this.returnsSeries = returnsSeries;
        this.equityCurve = equityCurve;
        this.returnsByAssetKey = returnsByAssetKey;
        this.returnsByAssetDate = returnsByAssetDate;
        this.warnings = warnings;
    }

    public Double getRfAnnual() { return rfAnnual; }
    public void setRfAnnual(Double rfAnnual) { this.rfAnnual = rfAnnual; }
    public List<Double> getReturnsSeries() { return returnsSeries; }
    public void setReturnsSeries(List<Double> returnsSeries) { this.returnsSeries = returnsSeries; }
    public List<Double> getEquityCurve() { return equityCurve; }
    public void setEquityCurve(List<Double> equityCurve) { this.equityCurve = equityCurve; }
    public Map<String, List<Double>> getReturnsByAssetKey() { return returnsByAssetKey; }
    public void setReturnsByAssetKey(Map<String, List<Double>> returnsByAssetKey) { this.returnsByAssetKey = returnsByAssetKey; }
    public Map<String, NavigableMap<LocalDate, Double>> getReturnsByAssetDate() { return returnsByAssetDate; }
    public void setReturnsByAssetDate(Map<String, NavigableMap<LocalDate, Double>> returnsByAssetDate) { this.returnsByAssetDate = returnsByAssetDate; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
