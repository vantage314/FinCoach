package com.fincoach.core.healthv2.analyzer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DebtOptimizerV1Result {
    private String strategy;
    private String summaryLevel;
    private BigDecimal monthlySurplus;
    private BigDecimal budgetForExtraPayment;
    private List<Map<String, Object>> plan = new ArrayList<>();
    private Map<String, Object> tradeoffHint = new LinkedHashMap<>();
    private List<String> warnings = new ArrayList<>();
    private List<Map<String, Object>> warningDetails = new ArrayList<>();
    private String topDebtName;

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getSummaryLevel() { return summaryLevel; }
    public void setSummaryLevel(String summaryLevel) { this.summaryLevel = summaryLevel; }
    public BigDecimal getMonthlySurplus() { return monthlySurplus; }
    public void setMonthlySurplus(BigDecimal monthlySurplus) { this.monthlySurplus = monthlySurplus; }
    public BigDecimal getBudgetForExtraPayment() { return budgetForExtraPayment; }
    public void setBudgetForExtraPayment(BigDecimal budgetForExtraPayment) { this.budgetForExtraPayment = budgetForExtraPayment; }
    public List<Map<String, Object>> getPlan() { return plan; }
    public void setPlan(List<Map<String, Object>> plan) { this.plan = plan; }
    public Map<String, Object> getTradeoffHint() { return tradeoffHint; }
    public void setTradeoffHint(Map<String, Object> tradeoffHint) { this.tradeoffHint = tradeoffHint; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<Map<String, Object>> getWarningDetails() { return warningDetails; }
    public void setWarningDetails(List<Map<String, Object>> warningDetails) { this.warningDetails = warningDetails; }
    public String getTopDebtName() { return topDebtName; }
    public void setTopDebtName(String topDebtName) { this.topDebtName = topDebtName; }

    public Map<String, Object> toAdviceMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("strategy", strategy);
        map.put("summaryLevel", summaryLevel);
        map.put("monthlySurplus", monthlySurplus);
        map.put("budgetForExtraPayment", budgetForExtraPayment);
        map.put("plan", plan == null ? new ArrayList<>() : plan);
        map.put("tradeoffHint", tradeoffHint == null ? new LinkedHashMap<>() : tradeoffHint);
        map.put("warnings", warnings == null ? new ArrayList<>() : warnings);
        return map;
    }
}
