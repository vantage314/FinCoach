package com.fincoach.core.healthv2.analyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DebtCashflowV1Result {
    private Map<String, Object> debtMetrics = new LinkedHashMap<>();
    private Map<String, Object> cashflowMetrics = new LinkedHashMap<>();
    private Map<String, Object> combined = new LinkedHashMap<>();
    private Map<String, Object> advice = new LinkedHashMap<>();
    private List<String> warnings = new ArrayList<>();
    private List<Map<String, Object>> warningDetails = new ArrayList<>();
    private Double dti;
    private Double surplusRate;
    private Double emergencyFundMonths;
    private String stressLevel;

    public Map<String, Object> getDebtMetrics() { return debtMetrics; }
    public void setDebtMetrics(Map<String, Object> debtMetrics) { this.debtMetrics = debtMetrics; }
    public Map<String, Object> getCashflowMetrics() { return cashflowMetrics; }
    public void setCashflowMetrics(Map<String, Object> cashflowMetrics) { this.cashflowMetrics = cashflowMetrics; }
    public Map<String, Object> getCombined() { return combined; }
    public void setCombined(Map<String, Object> combined) { this.combined = combined; }
    public Map<String, Object> getAdvice() { return advice; }
    public void setAdvice(Map<String, Object> advice) { this.advice = advice; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<Map<String, Object>> getWarningDetails() { return warningDetails; }
    public void setWarningDetails(List<Map<String, Object>> warningDetails) { this.warningDetails = warningDetails; }
    public Double getDti() { return dti; }
    public void setDti(Double dti) { this.dti = dti; }
    public Double getSurplusRate() { return surplusRate; }
    public void setSurplusRate(Double surplusRate) { this.surplusRate = surplusRate; }
    public Double getEmergencyFundMonths() { return emergencyFundMonths; }
    public void setEmergencyFundMonths(Double emergencyFundMonths) { this.emergencyFundMonths = emergencyFundMonths; }
    public String getStressLevel() { return stressLevel; }
    public void setStressLevel(String stressLevel) { this.stressLevel = stressLevel; }

    public Map<String, Object> toMetricsMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("debtMetrics", debtMetrics);
        map.put("cashflowMetrics", cashflowMetrics);
        map.put("combined", combined);
        return map;
    }
}
