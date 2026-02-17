package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AlertV1Config {
    private final boolean riskHighEnabled;
    private final boolean mddHighEnabled;
    private final boolean rebalanceTriggeredEnabled;
    private final boolean cashflowNegativeEnabled;
    private final boolean emergencyFundLowEnabled;
    private final boolean dtiHighEnabled;
    private final boolean debtToAssetsHighEnabled;
    private final boolean insuranceGapHighEnabled;
    private final int riskHighMin;
    private final double mddHighThreshold;
    private final double emergencyFundLow;
    private final double dtiHigh;
    private final double debtToAssetsHigh;
    private final double insuranceGapValueMin;
    private final String riskHighSeverity;
    private final String mddHighSeverity;
    private final String rebalanceTriggeredSeverity;
    private final String cashflowNegativeSeverity;
    private final String emergencyFundLowSeverity;
    private final String dtiHighSeverity;
    private final String debtToAssetsHighSeverity;
    private final String insuranceGapHighSeverity;
    private final List<String> warnings;

    public AlertV1Config(boolean riskHighEnabled,
                         boolean mddHighEnabled,
                         boolean rebalanceTriggeredEnabled,
                         boolean cashflowNegativeEnabled,
                         boolean emergencyFundLowEnabled,
                         boolean dtiHighEnabled,
                         boolean debtToAssetsHighEnabled,
                         boolean insuranceGapHighEnabled,
                         int riskHighMin,
                         double mddHighThreshold,
                         double emergencyFundLow,
                         double dtiHigh,
                         double debtToAssetsHigh,
                         double insuranceGapValueMin,
                         String riskHighSeverity,
                         String mddHighSeverity,
                         String rebalanceTriggeredSeverity,
                         String cashflowNegativeSeverity,
                         String emergencyFundLowSeverity,
                         String dtiHighSeverity,
                         String debtToAssetsHighSeverity,
                         String insuranceGapHighSeverity,
                         List<String> warnings) {
        this.riskHighEnabled = riskHighEnabled;
        this.mddHighEnabled = mddHighEnabled;
        this.rebalanceTriggeredEnabled = rebalanceTriggeredEnabled;
        this.cashflowNegativeEnabled = cashflowNegativeEnabled;
        this.emergencyFundLowEnabled = emergencyFundLowEnabled;
        this.dtiHighEnabled = dtiHighEnabled;
        this.debtToAssetsHighEnabled = debtToAssetsHighEnabled;
        this.insuranceGapHighEnabled = insuranceGapHighEnabled;
        this.riskHighMin = riskHighMin;
        this.mddHighThreshold = mddHighThreshold;
        this.emergencyFundLow = emergencyFundLow;
        this.dtiHigh = dtiHigh;
        this.debtToAssetsHigh = debtToAssetsHigh;
        this.insuranceGapValueMin = insuranceGapValueMin;
        this.riskHighSeverity = riskHighSeverity;
        this.mddHighSeverity = mddHighSeverity;
        this.rebalanceTriggeredSeverity = rebalanceTriggeredSeverity;
        this.cashflowNegativeSeverity = cashflowNegativeSeverity;
        this.emergencyFundLowSeverity = emergencyFundLowSeverity;
        this.dtiHighSeverity = dtiHighSeverity;
        this.debtToAssetsHighSeverity = debtToAssetsHighSeverity;
        this.insuranceGapHighSeverity = insuranceGapHighSeverity;
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public boolean isRiskHighEnabled() { return riskHighEnabled; }
    public boolean isMddHighEnabled() { return mddHighEnabled; }
    public boolean isRebalanceTriggeredEnabled() { return rebalanceTriggeredEnabled; }
    public boolean isCashflowNegativeEnabled() { return cashflowNegativeEnabled; }
    public boolean isEmergencyFundLowEnabled() { return emergencyFundLowEnabled; }
    public boolean isDtiHighEnabled() { return dtiHighEnabled; }
    public boolean isDebtToAssetsHighEnabled() { return debtToAssetsHighEnabled; }
    public boolean isInsuranceGapHighEnabled() { return insuranceGapHighEnabled; }
    public int getRiskHighMin() { return riskHighMin; }
    public double getMddHighThreshold() { return mddHighThreshold; }
    public double getEmergencyFundLow() { return emergencyFundLow; }
    public double getDtiHigh() { return dtiHigh; }
    public double getDebtToAssetsHigh() { return debtToAssetsHigh; }
    public double getInsuranceGapValueMin() { return insuranceGapValueMin; }
    public String getRiskHighSeverity() { return riskHighSeverity; }
    public String getMddHighSeverity() { return mddHighSeverity; }
    public String getRebalanceTriggeredSeverity() { return rebalanceTriggeredSeverity; }
    public String getCashflowNegativeSeverity() { return cashflowNegativeSeverity; }
    public String getEmergencyFundLowSeverity() { return emergencyFundLowSeverity; }
    public String getDtiHighSeverity() { return dtiHighSeverity; }
    public String getDebtToAssetsHighSeverity() { return debtToAssetsHighSeverity; }
    public String getInsuranceGapHighSeverity() { return insuranceGapHighSeverity; }
    public List<String> getWarnings() { return warnings; }

    public static AlertV1Config fromSnapshot(ScoreRuleSnapshot snapshot) {
        List<String> warnings = new ArrayList<>();
        boolean riskHighEnabled = true;
        boolean mddHighEnabled = true;
        boolean rebalanceTriggeredEnabled = true;
        boolean cashflowNegativeEnabled = true;
        boolean emergencyFundLowEnabled = true;
        boolean dtiHighEnabled = true;
        boolean debtToAssetsHighEnabled = true;
        boolean insuranceGapHighEnabled = true;
        int riskHighMin = 70;
        double mddHighThreshold = 0.30;
        double emergencyFundLow = 3.0;
        double dtiHigh = 0.40;
        double debtToAssetsHigh = 0.60;
        double insuranceGapValueMin = 100000.0;
        String riskHighSeverity = "CRITICAL";
        String mddHighSeverity = "CRITICAL";
        String rebalanceTriggeredSeverity = "WARN";
        String cashflowNegativeSeverity = "CRITICAL";
        String emergencyFundLowSeverity = "WARN";
        String dtiHighSeverity = "WARN";
        String debtToAssetsHighSeverity = "WARN";
        String insuranceGapHighSeverity = "INFO";

        if (snapshot != null) {
            riskHighEnabled = readBool(snapshot, ScoreRuleDefaults.ALERT_RISK_HIGH_ENABLED, riskHighEnabled, warnings);
            mddHighEnabled = readBool(snapshot, ScoreRuleDefaults.ALERT_MDD_HIGH_ENABLED, mddHighEnabled, warnings);
            rebalanceTriggeredEnabled = readBool(snapshot, ScoreRuleDefaults.ALERT_REBAL_TRIGGERED_ENABLED, rebalanceTriggeredEnabled, warnings);
            cashflowNegativeEnabled = readBool(snapshot, ScoreRuleDefaults.ALERT_CASHFLOW_NEGATIVE_ENABLED, cashflowNegativeEnabled, warnings);
            emergencyFundLowEnabled = readBool(snapshot, ScoreRuleDefaults.ALERT_EMERGENCY_FUND_LOW_ENABLED, emergencyFundLowEnabled, warnings);
            dtiHighEnabled = readBool(snapshot, ScoreRuleDefaults.ALERT_DTI_HIGH_ENABLED, dtiHighEnabled, warnings);
            debtToAssetsHighEnabled = readBool(snapshot, ScoreRuleDefaults.ALERT_DEBT_TO_ASSETS_HIGH_ENABLED, debtToAssetsHighEnabled, warnings);
            insuranceGapHighEnabled = readBool(snapshot, ScoreRuleDefaults.ALERT_INSURANCE_GAP_HIGH_ENABLED, insuranceGapHighEnabled, warnings);

            riskHighMin = readInt(snapshot, ScoreRuleDefaults.ALERT_RISK_HIGH_MIN, riskHighMin, warnings);
            mddHighThreshold = readDecimal(snapshot, ScoreRuleDefaults.ALERT_MDD_HIGH, mddHighThreshold, warnings);
            emergencyFundLow = readDecimal(snapshot, ScoreRuleDefaults.EMERGENCY_MONTHS_LOW, emergencyFundLow, warnings);
            dtiHigh = readDecimal(snapshot, ScoreRuleDefaults.DTI_HIGH, dtiHigh, warnings);
            debtToAssetsHigh = readDecimal(snapshot, ScoreRuleDefaults.DEBT_TO_ASSETS_HIGH, debtToAssetsHigh, warnings);
            insuranceGapValueMin = readDecimal(snapshot, ScoreRuleDefaults.ALERT_INSURANCE_GAP_VALUE_MIN, insuranceGapValueMin, warnings);

            riskHighSeverity = normalizeSeverity(snapshot, ScoreRuleDefaults.ALERT_RISK_HIGH_SEVERITY,
                    riskHighSeverity, warnings);
            mddHighSeverity = normalizeSeverity(snapshot, ScoreRuleDefaults.ALERT_MDD_HIGH_SEVERITY,
                    mddHighSeverity, warnings);
            rebalanceTriggeredSeverity = normalizeSeverity(snapshot, ScoreRuleDefaults.ALERT_REBAL_TRIGGERED_SEVERITY,
                    rebalanceTriggeredSeverity, warnings);
            cashflowNegativeSeverity = normalizeSeverity(snapshot, ScoreRuleDefaults.ALERT_CASHFLOW_NEGATIVE_SEVERITY,
                    cashflowNegativeSeverity, warnings);
            emergencyFundLowSeverity = normalizeSeverity(snapshot, ScoreRuleDefaults.ALERT_EMERGENCY_FUND_LOW_SEVERITY,
                    emergencyFundLowSeverity, warnings);
            dtiHighSeverity = normalizeSeverity(snapshot, ScoreRuleDefaults.ALERT_DTI_HIGH_SEVERITY,
                    dtiHighSeverity, warnings);
            debtToAssetsHighSeverity = normalizeSeverity(snapshot, ScoreRuleDefaults.ALERT_DEBT_TO_ASSETS_HIGH_SEVERITY,
                    debtToAssetsHighSeverity, warnings);
            insuranceGapHighSeverity = normalizeSeverity(snapshot, ScoreRuleDefaults.ALERT_INSURANCE_GAP_HIGH_SEVERITY,
                    insuranceGapHighSeverity, warnings);

            if (snapshot.getWarnings() != null) {
                for (String w : snapshot.getWarnings()) {
                    if (w != null && !warnings.contains(w)) warnings.add(w);
                }
            }
        } else {
            warnings.add("SCORE_RULESET_FALLBACK_DEFAULT");
        }

        return new AlertV1Config(
                riskHighEnabled,
                mddHighEnabled,
                rebalanceTriggeredEnabled,
                cashflowNegativeEnabled,
                emergencyFundLowEnabled,
                dtiHighEnabled,
                debtToAssetsHighEnabled,
                insuranceGapHighEnabled,
                riskHighMin,
                mddHighThreshold,
                emergencyFundLow,
                dtiHigh,
                debtToAssetsHigh,
                insuranceGapValueMin,
                riskHighSeverity,
                mddHighSeverity,
                rebalanceTriggeredSeverity,
                cashflowNegativeSeverity,
                emergencyFundLowSeverity,
                dtiHighSeverity,
                debtToAssetsHighSeverity,
                insuranceGapHighSeverity,
                warnings
        );
    }

    private static boolean readBool(ScoreRuleSnapshot snapshot,
                                    String key,
                                    boolean fallback,
                                    List<String> warnings) {
        if (snapshot == null) return fallback;
        Boolean value = snapshot.getBool(key, null);
        if (value == null) {
            warnings.add("SCORE_RULE_PARAM_MISSING:" + key);
            return fallback;
        }
        return value;
    }

    private static int readInt(ScoreRuleSnapshot snapshot,
                               String key,
                               int fallback,
                               List<String> warnings) {
        if (snapshot == null) return fallback;
        Integer value = snapshot.getInt(key, null);
        if (value == null) {
            warnings.add("SCORE_RULE_PARAM_MISSING:" + key);
            return fallback;
        }
        return value;
    }

    private static double readDecimal(ScoreRuleSnapshot snapshot,
                                      String key,
                                      double fallback,
                                      List<String> warnings) {
        if (snapshot == null) return fallback;
        BigDecimal value = snapshot.getDecimal(key, null);
        if (value == null) {
            warnings.add("SCORE_RULE_PARAM_MISSING:" + key);
            return fallback;
        }
        try {
            return value.doubleValue();
        } catch (Exception e) {
            warnings.add("SCORE_RULE_PARAM_INVALID:" + key);
            return fallback;
        }
    }

    private static String normalizeSeverity(ScoreRuleSnapshot snapshot,
                                            String key,
                                            String fallback,
                                            List<String> warnings) {
        if (snapshot == null) return fallback;
        String raw = snapshot.getString(key, null);
        if (raw == null || raw.isBlank()) {
            warnings.add("SCORE_RULE_PARAM_MISSING:" + key);
            return fallback;
        }
        String normalized = raw.trim().toUpperCase();
        if (!"INFO".equals(normalized) && !"WARN".equals(normalized) && !"CRITICAL".equals(normalized)) {
            warnings.add("SCORE_RULE_PARAM_INVALID:" + key);
            return fallback;
        }
        return normalized;
    }
}
