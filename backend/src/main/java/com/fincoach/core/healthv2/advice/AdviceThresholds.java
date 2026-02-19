package com.fincoach.core.healthv2.advice;

import com.fincoach.core.healthv2.advice.rules.AdviceRuleDefaults;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleRegistry;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleSnapshot;
import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdviceThresholds {
    private final double rebalanceThreshold;
    private final int emergencyMonthsMin;
    private final double debtPaymentRatioMax;
    private final double surplusRateMin;
    private final List<String> warnings;

    private AdviceThresholds(double rebalanceThreshold,
                             int emergencyMonthsMin,
                             double debtPaymentRatioMax,
                             double surplusRateMin,
                             List<String> warnings) {
        this.rebalanceThreshold = rebalanceThreshold;
        this.emergencyMonthsMin = emergencyMonthsMin;
        this.debtPaymentRatioMax = debtPaymentRatioMax;
        this.surplusRateMin = surplusRateMin;
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public static AdviceThresholds fromSnapshots(ScoreRuleSnapshot scoreSnapshot,
                                                 AdviceRuleSnapshot adviceSnapshot) {
        List<String> warnings = new ArrayList<>();
        double rebalanceThreshold = 0.05;
        int emergencyMonthsMin = 6;
        double debtPaymentRatioMax = 0.35;
        double surplusRateMin = 0.10;

        if (adviceSnapshot != null) {
            rebalanceThreshold = readDecimal(adviceSnapshot, AdviceRuleDefaults.REBALANCE_DRIFT_PCT, rebalanceThreshold, warnings);
            if (adviceSnapshot.getWarnings() != null) {
                for (String w : adviceSnapshot.getWarnings()) {
                    if (!warnings.contains(w)) warnings.add(w);
                }
            }
        } else {
            warnings.add(AdviceRuleRegistry.WARN_FALLBACK_DEFAULT);
        }

        if (scoreSnapshot != null) {
            emergencyMonthsMin = readInt(scoreSnapshot, ScoreRuleDefaults.EMERGENCY_MONTHS_MIN, emergencyMonthsMin, warnings);
            debtPaymentRatioMax = readDecimal(scoreSnapshot, ScoreRuleDefaults.DEBT_PAYMENT_RATIO_MAX, debtPaymentRatioMax, warnings);
            surplusRateMin = readDecimal(scoreSnapshot, ScoreRuleDefaults.SURPLUS_RATE_MIN, surplusRateMin, warnings);
            if (scoreSnapshot.getWarnings() != null) {
                for (String w : scoreSnapshot.getWarnings()) {
                    if (!warnings.contains(w)) warnings.add(w);
                }
            }
        } else {
            warnings.add("SCORE_RULESET_FALLBACK_DEFAULT");
        }

        return new AdviceThresholds(rebalanceThreshold, emergencyMonthsMin, debtPaymentRatioMax, surplusRateMin, warnings);
    }

    private static double readDecimal(AdviceRuleSnapshot snapshot, String key, double fallback, List<String> warnings) {
        if (snapshot.getParams() == null || !snapshot.getParams().containsKey(key)) {
            warnings.add(AdviceRuleRegistry.WARN_PARAM_MISSING_PREFIX + key);
            return fallback;
        }
        BigDecimal value = snapshot.getDecimal(key, BigDecimal.valueOf(fallback));
        return value == null ? fallback : value.doubleValue();
    }

    private static double readDecimal(ScoreRuleSnapshot snapshot, String key, double fallback, List<String> warnings) {
        if (snapshot.getParams() == null || !snapshot.getParams().containsKey(key)) {
            warnings.add("SCORE_RULE_PARAM_MISSING:" + key);
            return fallback;
        }
        BigDecimal value = snapshot.getDecimal(key, BigDecimal.valueOf(fallback));
        return value == null ? fallback : value.doubleValue();
    }

    private static int readInt(ScoreRuleSnapshot snapshot, String key, int fallback, List<String> warnings) {
        if (snapshot.getParams() == null || !snapshot.getParams().containsKey(key)) {
            warnings.add("SCORE_RULE_PARAM_MISSING:" + key);
            return fallback;
        }
        Integer value = snapshot.getInt(key, fallback);
        return value == null ? fallback : value;
    }

    public double getRebalanceThreshold() { return rebalanceThreshold; }
    public int getEmergencyMonthsMin() { return emergencyMonthsMin; }
    public double getDebtPaymentRatioMax() { return debtPaymentRatioMax; }
    public double getSurplusRateMin() { return surplusRateMin; }
    public List<String> getWarnings() { return warnings; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(AdviceRuleDefaults.REBALANCE_DRIFT_PCT, rebalanceThreshold);
        map.put("EMERGENCY_MONTHS_MIN", emergencyMonthsMin);
        map.put("DEBT_PAYMENT_RATIO_MAX", debtPaymentRatioMax);
        map.put("SURPLUS_RATE_MIN", surplusRateMin);
        return map;
    }
}
