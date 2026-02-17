package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InsuranceGapThresholds {
    private final double premiumRatioMax;
    private final double lifeMultiplier;
    private final double lifeDependentBonusMultiplier;
    private final double criticalIllnessMultiplier;
    private final double accidentMultiplier;
    private final List<String> warnings;

    public InsuranceGapThresholds(double premiumRatioMax,
                                  double lifeMultiplier,
                                  double lifeDependentBonusMultiplier,
                                  double criticalIllnessMultiplier,
                                  double accidentMultiplier,
                                  List<String> warnings) {
        this.premiumRatioMax = premiumRatioMax;
        this.lifeMultiplier = lifeMultiplier;
        this.lifeDependentBonusMultiplier = lifeDependentBonusMultiplier;
        this.criticalIllnessMultiplier = criticalIllnessMultiplier;
        this.accidentMultiplier = accidentMultiplier;
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public double getPremiumRatioMax() { return premiumRatioMax; }
    public double getLifeMultiplier() { return lifeMultiplier; }
    public double getLifeDependentBonusMultiplier() { return lifeDependentBonusMultiplier; }
    public double getCriticalIllnessMultiplier() { return criticalIllnessMultiplier; }
    public double getAccidentMultiplier() { return accidentMultiplier; }
    public List<String> getWarnings() { return warnings; }

    public static InsuranceGapThresholds fromSnapshot(ScoreRuleSnapshot snapshot) {
        List<String> warnings = new ArrayList<>();
        double premiumRatioMax = 0.10;
        double lifeMultiplier = 10.0;
        double lifeDependentBonusMultiplier = 1.0;
        double criticalIllnessMultiplier = 3.0;
        double accidentMultiplier = 1.0;

        if (snapshot != null) {
            premiumRatioMax = readDecimal(snapshot, ScoreRuleDefaults.INS_PREMIUM_RATIO_MAX, premiumRatioMax, warnings);
            lifeMultiplier = readDecimal(snapshot, ScoreRuleDefaults.INS_LIFE_MULTIPLIER, lifeMultiplier, warnings);
            lifeDependentBonusMultiplier = readDecimal(snapshot, ScoreRuleDefaults.INS_LIFE_DEPENDENT_BONUS_MULTIPLIER,
                    lifeDependentBonusMultiplier, warnings);
            criticalIllnessMultiplier = readDecimal(snapshot, ScoreRuleDefaults.INS_CRITICAL_ILLNESS_MULTIPLIER,
                    criticalIllnessMultiplier, warnings);
            accidentMultiplier = readDecimal(snapshot, ScoreRuleDefaults.INS_ACCIDENT_MULTIPLIER, accidentMultiplier, warnings);

            if (snapshot.getWarnings() != null) {
                for (String w : snapshot.getWarnings()) {
                    if (w != null && !warnings.contains(w)) warnings.add(w);
                }
            }
        } else {
            warnings.add("SCORE_RULESET_FALLBACK_DEFAULT");
        }

        if (premiumRatioMax < 0) premiumRatioMax = 0.10;
        if (lifeMultiplier < 0) lifeMultiplier = 10.0;
        if (criticalIllnessMultiplier < 0) criticalIllnessMultiplier = 3.0;
        if (accidentMultiplier < 0) accidentMultiplier = 1.0;
        if (lifeDependentBonusMultiplier < 0) lifeDependentBonusMultiplier = 0;

        return new InsuranceGapThresholds(
                premiumRatioMax,
                lifeMultiplier,
                lifeDependentBonusMultiplier,
                criticalIllnessMultiplier,
                accidentMultiplier,
                warnings);
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
