package com.fincoach.core.healthv2.analyzer;

public class AlertV1Input {
    private Integer riskScoreValue;
    private String riskScoreLevel;
    private Double maxDrawdown;
    private Boolean rebalanceTriggered;
    private Double monthlySurplus;
    private Double emergencyFundMonths;
    private Double dti;
    private Double debtToAssets;
    private String insuranceSummaryLevel;
    private Double insuranceTopGapValue;

    public Integer getRiskScoreValue() { return riskScoreValue; }
    public void setRiskScoreValue(Integer riskScoreValue) { this.riskScoreValue = riskScoreValue; }
    public String getRiskScoreLevel() { return riskScoreLevel; }
    public void setRiskScoreLevel(String riskScoreLevel) { this.riskScoreLevel = riskScoreLevel; }
    public Double getMaxDrawdown() { return maxDrawdown; }
    public void setMaxDrawdown(Double maxDrawdown) { this.maxDrawdown = maxDrawdown; }
    public Boolean getRebalanceTriggered() { return rebalanceTriggered; }
    public void setRebalanceTriggered(Boolean rebalanceTriggered) { this.rebalanceTriggered = rebalanceTriggered; }
    public Double getMonthlySurplus() { return monthlySurplus; }
    public void setMonthlySurplus(Double monthlySurplus) { this.monthlySurplus = monthlySurplus; }
    public Double getEmergencyFundMonths() { return emergencyFundMonths; }
    public void setEmergencyFundMonths(Double emergencyFundMonths) { this.emergencyFundMonths = emergencyFundMonths; }
    public Double getDti() { return dti; }
    public void setDti(Double dti) { this.dti = dti; }
    public Double getDebtToAssets() { return debtToAssets; }
    public void setDebtToAssets(Double debtToAssets) { this.debtToAssets = debtToAssets; }
    public String getInsuranceSummaryLevel() { return insuranceSummaryLevel; }
    public void setInsuranceSummaryLevel(String insuranceSummaryLevel) { this.insuranceSummaryLevel = insuranceSummaryLevel; }
    public Double getInsuranceTopGapValue() { return insuranceTopGapValue; }
    public void setInsuranceTopGapValue(Double insuranceTopGapValue) { this.insuranceTopGapValue = insuranceTopGapValue; }
}
