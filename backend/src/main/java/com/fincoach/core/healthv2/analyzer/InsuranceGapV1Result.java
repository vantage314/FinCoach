package com.fincoach.core.healthv2.analyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InsuranceGapV1Result {
    private Map<String, Object> metrics = new LinkedHashMap<>();
    private Map<String, Object> advice = new LinkedHashMap<>();
    private List<String> warnings = new ArrayList<>();
    private List<Map<String, Object>> warningDetails = new ArrayList<>();
    private String summaryLevel;
    private String topGapType;
    private Double topGapValue;
    private Double premiumRatio;

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    public Map<String, Object> getAdvice() { return advice; }
    public void setAdvice(Map<String, Object> advice) { this.advice = advice; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<Map<String, Object>> getWarningDetails() { return warningDetails; }
    public void setWarningDetails(List<Map<String, Object>> warningDetails) { this.warningDetails = warningDetails; }
    public String getSummaryLevel() { return summaryLevel; }
    public void setSummaryLevel(String summaryLevel) { this.summaryLevel = summaryLevel; }
    public String getTopGapType() { return topGapType; }
    public void setTopGapType(String topGapType) { this.topGapType = topGapType; }
    public Double getTopGapValue() { return topGapValue; }
    public void setTopGapValue(Double topGapValue) { this.topGapValue = topGapValue; }
    public Double getPremiumRatio() { return premiumRatio; }
    public void setPremiumRatio(Double premiumRatio) { this.premiumRatio = premiumRatio; }
}
