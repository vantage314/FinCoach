package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DebtOptimizerThresholds {
    private final String defaultStrategy;
    private final double extraPayRatio;
    private final Double expectedReturn;
    private final double returnMargin;
    private final List<String> warnings;

    public DebtOptimizerThresholds(String defaultStrategy,
                                   double extraPayRatio,
                                   Double expectedReturn,
                                   double returnMargin,
                                   List<String> warnings) {
        this.defaultStrategy = defaultStrategy;
        this.extraPayRatio = extraPayRatio;
        this.expectedReturn = expectedReturn;
        this.returnMargin = returnMargin;
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public String getDefaultStrategy() { return defaultStrategy; }
    public double getExtraPayRatio() { return extraPayRatio; }
    public Double getExpectedReturn() { return expectedReturn; }
    public double getReturnMargin() { return returnMargin; }
    public List<String> getWarnings() { return warnings; }

    public static DebtOptimizerThresholds fromSnapshot(ScoreRuleSnapshot snapshot) {
        List<String> warnings = new ArrayList<>();
        String strategy = "AVALANCHE";
        double extraPayRatio = 0.5;
        Double expectedReturn = 0.06;
        double returnMargin = 0.01;

        if (snapshot != null) {
            String rawStrategy = snapshot.getString(ScoreRuleDefaults.DEBT_OPTIMIZER_STRATEGY, null);
            if (rawStrategy == null || rawStrategy.isBlank()) {
                warnings.add("SCORE_RULE_PARAM_MISSING:" + ScoreRuleDefaults.DEBT_OPTIMIZER_STRATEGY);
            } else {
                strategy = rawStrategy.trim().toUpperCase();
            }

            expectedReturn = readDecimal(snapshot, ScoreRuleDefaults.DEBT_OPTIMIZER_EXPECTED_RETURN,
                    expectedReturn, warnings);
            extraPayRatio = readDecimal(snapshot, ScoreRuleDefaults.DEBT_OPTIMIZER_EXTRA_PAY_RATIO,
                    extraPayRatio, warnings);
            returnMargin = readDecimal(snapshot, ScoreRuleDefaults.DEBT_OPTIMIZER_RETURN_MARGIN,
                    returnMargin, warnings);

            if (snapshot.getWarnings() != null) {
                for (String w : snapshot.getWarnings()) {
                    if (w != null && !warnings.contains(w)) warnings.add(w);
                }
            }
        } else {
            warnings.add("SCORE_RULESET_FALLBACK_DEFAULT");
        }

        if (extraPayRatio < 0) extraPayRatio = 0;
        if (extraPayRatio > 1) extraPayRatio = 1;
        if (returnMargin < 0) returnMargin = 0;

        return new DebtOptimizerThresholds(strategy, extraPayRatio, expectedReturn, returnMargin, warnings);
    }

    private static Double readDecimal(ScoreRuleSnapshot snapshot,
                                      String key,
                                      Double fallback,
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
