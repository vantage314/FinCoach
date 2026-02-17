package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.advice.WarningCollector;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlertEngineV1 {

    public AlertV1Result build(AlertV1Input input, ScoreRuleSnapshot snapshot, LocalDateTime createdAt) {
        AlertV1Result result = new AlertV1Result();
        AlertV1Config config = AlertV1Config.fromSnapshot(snapshot);
        WarningCollector warnings = new WarningCollector();
        warnings.addAll(config.getWarnings());

        List<Map<String, Object>> alerts = new ArrayList<>();
        if (input == null) {
            warnings.add(AlertV1WarningCodes.ALERT_INPUT_MISSING_ALL, "alert input missing");
        } else {
            handleRiskAlert(input, config, alerts, warnings, createdAt);
            handleMddAlert(input, config, alerts, warnings, createdAt);
            handleRebalanceAlert(input, config, alerts, warnings, createdAt);
            handleCashflowAlert(input, config, alerts, warnings, createdAt);
            handleEmergencyAlert(input, config, alerts, warnings, createdAt);
            handleDtiAlert(input, config, alerts, warnings, createdAt);
            handleDebtToAssetsAlert(input, config, alerts, warnings, createdAt);
            handleInsuranceGapAlert(input, config, alerts, warnings, createdAt);
        }

        alerts.sort((a, b) -> {
            int sa = severityRank((String) a.get("severity"));
            int sb = severityRank((String) b.get("severity"));
            if (sa != sb) return Integer.compare(sb, sa);
            String ta = a.get("createdAt") == null ? "" : a.get("createdAt").toString();
            String tb = b.get("createdAt") == null ? "" : b.get("createdAt").toString();
            return tb.compareTo(ta);
        });

        result.setAlerts(alerts);
        result.setWarnings(warnings.codes());
        result.setWarningDetails(warnings.detailMaps());
        result.setOpenCount(alerts.size());
        int criticalCount = 0;
        List<String> topCodes = new ArrayList<>();
        for (Map<String, Object> alert : alerts) {
            String severity = alert.get("severity") == null ? "" : alert.get("severity").toString();
            if ("CRITICAL".equals(severity)) criticalCount++;
            if (topCodes.size() < 3) {
                Object code = alert.get("code");
                if (code != null) topCodes.add(code.toString());
            }
        }
        result.setCriticalCount(criticalCount);
        result.setTopCodes(topCodes);
        result.setLastCreatedAt(createdAt == null ? null : createdAt.toString());
        return result;
    }

    private void handleRiskAlert(AlertV1Input input,
                                 AlertV1Config config,
                                 List<Map<String, Object>> alerts,
                                 WarningCollector warnings,
                                 LocalDateTime createdAt) {
        if (!config.isRiskHighEnabled()) return;
        Integer value = input.getRiskScoreValue();
        String level = input.getRiskScoreLevel();
        boolean triggered = false;
        String detail;
        if (level != null && "HIGH".equalsIgnoreCase(level)) {
            triggered = true;
            detail = "riskScore.level=HIGH";
        } else if (value != null) {
            triggered = value >= config.getRiskHighMin();
            detail = "riskScore=" + value + " >= " + config.getRiskHighMin();
        } else {
            warnings.add(AlertV1WarningCodes.ALERT_INPUT_MISSING_RISK_SCORE, "riskScore missing");
            return;
        }
        if (triggered) {
            alerts.add(buildAlert("ALERT_RISK_HIGH", config.getRiskHighSeverity(),
                    "风险评分偏高",
                    detail,
                    List.of("scores.riskScore"),
                    createdAt));
        }
    }

    private void handleMddAlert(AlertV1Input input,
                                AlertV1Config config,
                                List<Map<String, Object>> alerts,
                                WarningCollector warnings,
                                LocalDateTime createdAt) {
        if (!config.isMddHighEnabled()) return;
        Double mdd = input.getMaxDrawdown();
        if (mdd == null) {
            warnings.add(AlertV1WarningCodes.ALERT_INPUT_MISSING_MDD, "maxDrawdown missing");
            return;
        }
        double value = Math.abs(mdd);
        if (value > config.getMddHighThreshold()) {
            alerts.add(buildAlert("ALERT_MDD_HIGH", config.getMddHighSeverity(),
                    "最大回撤偏高",
                    "maxDrawdown=" + formatRatio(value) + " > " + formatRatio(config.getMddHighThreshold()),
                    List.of("portfolio.performance.maxDrawdown"),
                    createdAt));
        }
    }

    private void handleRebalanceAlert(AlertV1Input input,
                                      AlertV1Config config,
                                      List<Map<String, Object>> alerts,
                                      WarningCollector warnings,
                                      LocalDateTime createdAt) {
        if (!config.isRebalanceTriggeredEnabled()) return;
        Boolean triggered = input.getRebalanceTriggered();
        if (triggered == null) {
            warnings.add(AlertV1WarningCodes.ALERT_INPUT_MISSING_REBALANCE, "rebalance triggered missing");
            return;
        }
        if (triggered) {
            alerts.add(buildAlert("ALERT_REBAL_TRIGGERED", config.getRebalanceTriggeredSeverity(),
                    "触发再平衡",
                    "rebalanceAdviceV1.triggered=true",
                    List.of("portfolio.rebalanceAdviceV1"),
                    createdAt));
        }
    }

    private void handleCashflowAlert(AlertV1Input input,
                                     AlertV1Config config,
                                     List<Map<String, Object>> alerts,
                                     WarningCollector warnings,
                                     LocalDateTime createdAt) {
        if (!config.isCashflowNegativeEnabled()) return;
        Double surplus = input.getMonthlySurplus();
        if (surplus == null) {
            warnings.add(AlertV1WarningCodes.ALERT_INPUT_MISSING_CASHFLOW, "monthlySurplus missing");
            return;
        }
        if (surplus < 0) {
            alerts.add(buildAlert("ALERT_CASHFLOW_NEGATIVE", config.getCashflowNegativeSeverity(),
                    "现金流为负",
                    "monthlySurplus=" + formatAmount(surplus) + " < 0",
                    List.of("debtCashflowV1.cashflowMetrics.monthlySurplus"),
                    createdAt));
        }
    }

    private void handleEmergencyAlert(AlertV1Input input,
                                      AlertV1Config config,
                                      List<Map<String, Object>> alerts,
                                      WarningCollector warnings,
                                      LocalDateTime createdAt) {
        if (!config.isEmergencyFundLowEnabled()) return;
        Double months = input.getEmergencyFundMonths();
        if (months == null) {
            warnings.add(AlertV1WarningCodes.ALERT_INPUT_MISSING_EMERGENCY_MONTHS, "emergencyFundMonths missing");
            return;
        }
        if (months < config.getEmergencyFundLow()) {
            alerts.add(buildAlert("ALERT_EMERGENCY_FUND_LOW", config.getEmergencyFundLowSeverity(),
                    "应急金不足",
                    "emergencyFundMonths=" + formatAmount(months) + " < " + formatAmount(config.getEmergencyFundLow()),
                    List.of("debtCashflowV1.cashflowMetrics.emergencyFundMonths"),
                    createdAt));
        }
    }

    private void handleDtiAlert(AlertV1Input input,
                                AlertV1Config config,
                                List<Map<String, Object>> alerts,
                                WarningCollector warnings,
                                LocalDateTime createdAt) {
        if (!config.isDtiHighEnabled()) return;
        Double dti = input.getDti();
        if (dti == null) {
            warnings.add(AlertV1WarningCodes.ALERT_INPUT_MISSING_DTI, "dti missing");
            return;
        }
        if (dti >= config.getDtiHigh()) {
            alerts.add(buildAlert("ALERT_DTI_HIGH", config.getDtiHighSeverity(),
                    "负债压力偏高",
                    "dti=" + formatRatio(dti) + " >= " + formatRatio(config.getDtiHigh()),
                    List.of("debtCashflowV1.debtMetrics.dti"),
                    createdAt));
        }
    }

    private void handleDebtToAssetsAlert(AlertV1Input input,
                                         AlertV1Config config,
                                         List<Map<String, Object>> alerts,
                                         WarningCollector warnings,
                                         LocalDateTime createdAt) {
        if (!config.isDebtToAssetsHighEnabled()) return;
        Double ratio = input.getDebtToAssets();
        if (ratio == null) {
            warnings.add(AlertV1WarningCodes.ALERT_INPUT_MISSING_DEBT_TO_ASSETS, "debtToAssets missing");
            return;
        }
        if (ratio > config.getDebtToAssetsHigh()) {
            alerts.add(buildAlert("ALERT_DEBT_TO_ASSETS_HIGH", config.getDebtToAssetsHighSeverity(),
                    "负债占比偏高",
                    "debtToAssets=" + formatRatio(ratio) + " > " + formatRatio(config.getDebtToAssetsHigh()),
                    List.of("debtCashflowV1.debtMetrics.debtToAssets"),
                    createdAt));
        }
    }

    private void handleInsuranceGapAlert(AlertV1Input input,
                                         AlertV1Config config,
                                         List<Map<String, Object>> alerts,
                                         WarningCollector warnings,
                                         LocalDateTime createdAt) {
        if (!config.isInsuranceGapHighEnabled()) return;
        String summaryLevel = input.getInsuranceSummaryLevel();
        Double topGap = input.getInsuranceTopGapValue();
        boolean triggered = false;
        String detail = "";
        if (summaryLevel != null && "HIGH".equalsIgnoreCase(summaryLevel)) {
            triggered = true;
            detail = "insurance.summaryLevel=HIGH";
        } else if (topGap != null) {
            triggered = topGap > config.getInsuranceGapValueMin();
            detail = "topGapValue=" + formatAmount(topGap) + " > " + formatAmount(config.getInsuranceGapValueMin());
        } else {
            warnings.add(AlertV1WarningCodes.ALERT_INPUT_MISSING_INSURANCE_GAP, "insurance gap missing");
            return;
        }
        if (triggered) {
            alerts.add(buildAlert("ALERT_INSURANCE_GAP_HIGH", config.getInsuranceGapHighSeverity(),
                    "保险缺口偏高",
                    detail,
                    List.of("insuranceGapV1"),
                    createdAt));
        }
    }

    private Map<String, Object> buildAlert(String code,
                                           String severity,
                                           String title,
                                           String detail,
                                           List<String> related,
                                           LocalDateTime createdAt) {
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("code", code);
        alert.put("severity", severity);
        alert.put("title", title);
        alert.put("detail", detail == null ? "" : detail);
        alert.put("related", related == null ? new ArrayList<>() : related);
        alert.put("createdAt", createdAt == null ? null : createdAt.toString());
        alert.put("status", "OPEN");
        return alert;
    }

    private int severityRank(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) return 3;
        if ("WARN".equalsIgnoreCase(severity)) return 2;
        if ("INFO".equalsIgnoreCase(severity)) return 1;
        return 0;
    }

    private String formatRatio(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatAmount(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
