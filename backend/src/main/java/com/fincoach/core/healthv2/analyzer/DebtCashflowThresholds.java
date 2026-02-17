package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DebtCashflowThresholds {
    private final double dtiHigh;
    private final double dtiMed;
    private final double surplusRateMin;
    private final double emergencyLow;
    private final double emergencyOk;
    private final double debtToAssetsHigh;
    private final double liquidAssetEstimateRatio;
    private final List<String> warnings;

    public DebtCashflowThresholds(double dtiHigh,
                                  double dtiMed,
                                  double surplusRateMin,
                                  double emergencyLow,
                                  double emergencyOk,
                                  double debtToAssetsHigh,
                                  double liquidAssetEstimateRatio,
                                  List<String> warnings) {
        this.dtiHigh = dtiHigh;
        this.dtiMed = dtiMed;
        this.surplusRateMin = surplusRateMin;
        this.emergencyLow = emergencyLow;
        this.emergencyOk = emergencyOk;
        this.debtToAssetsHigh = debtToAssetsHigh;
        this.liquidAssetEstimateRatio = liquidAssetEstimateRatio;
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public double getDtiHigh() { return dtiHigh; }
    public double getDtiMed() { return dtiMed; }
    public double getSurplusRateMin() { return surplusRateMin; }
    public double getEmergencyLow() { return emergencyLow; }
    public double getEmergencyOk() { return emergencyOk; }
    public double getDebtToAssetsHigh() { return debtToAssetsHigh; }
    public double getLiquidAssetEstimateRatio() { return liquidAssetEstimateRatio; }
    public List<String> getWarnings() { return warnings; }

    public static DebtCashflowThresholds fromSnapshot(ScoreRuleSnapshot snapshot) {
        List<String> warnings = new ArrayList<>();
        double dtiHigh = 0.40;
        double dtiMed = 0.20;
        double surplusRateMin = 0.10;
        double emergencyLow = 3.0;
        double emergencyOk = 6.0;
        double debtToAssetsHigh = 0.60;
        double liquidAssetEstimateRatio = 0.10;

        if (snapshot != null) {
            dtiHigh = readDecimal(snapshot, ScoreRuleDefaults.DTI_HIGH, dtiHigh, warnings);
            dtiMed = readDecimal(snapshot, ScoreRuleDefaults.DTI_MED, dtiMed, warnings);
            surplusRateMin = readDecimal(snapshot, ScoreRuleDefaults.SURPLUS_RATE_MIN, surplusRateMin, warnings);
            emergencyLow = readDecimal(snapshot, ScoreRuleDefaults.EMERGENCY_MONTHS_LOW, emergencyLow, warnings);
            emergencyOk = readDecimal(snapshot, ScoreRuleDefaults.EMERGENCY_MONTHS_OK, emergencyOk, warnings);
            debtToAssetsHigh = readDecimal(snapshot, ScoreRuleDefaults.DEBT_TO_ASSETS_HIGH, debtToAssetsHigh, warnings);
            liquidAssetEstimateRatio = readDecimal(snapshot, ScoreRuleDefaults.LIQUID_ASSET_ESTIMATE_RATIO,
                    liquidAssetEstimateRatio, warnings);
            if (snapshot.getWarnings() != null) {
                for (String w : snapshot.getWarnings()) {
                    if (w != null && !warnings.contains(w)) warnings.add(w);
                }
            }
        } else {
            warnings.add("SCORE_RULESET_FALLBACK_DEFAULT");
        }

        if (dtiMed >= dtiHigh) {
            dtiMed = Math.max(0, dtiHigh - 0.01);
        }
        if (emergencyLow >= emergencyOk) {
            emergencyLow = Math.max(0, emergencyOk - 1.0);
        }
        return new DebtCashflowThresholds(
                dtiHigh, dtiMed, surplusRateMin, emergencyLow, emergencyOk,
                debtToAssetsHigh, liquidAssetEstimateRatio, warnings);
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
}
