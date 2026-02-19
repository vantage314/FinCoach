package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.entity.FcInsuranceConfigEntity;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InsuranceGapCalculator {

    public InsuranceGapCalcResult calculate(FcInsuranceProfileEntity profile, FcInsuranceConfigEntity config) {
        List<String> warnings = new ArrayList<>();
        Map<String, Object> metrics = new LinkedHashMap<>();

        BigDecimal income = profile == null ? BigDecimal.ZERO : safe(profile.getAnnualIncome());
        BigDecimal annualPremium = profile == null ? BigDecimal.ZERO : safe(profile.getAnnualPremiumTotal());

        BigDecimal targetMedical = safe(config == null ? null : config.getTargetMedical(), new BigDecimal("500000"));
        BigDecimal targetAccident = safe(config == null ? null : config.getTargetAccident(), new BigDecimal("500000"));
        BigDecimal targetCi = safe(config == null ? null : config.getTargetCi(), new BigDecimal("500000"));
        BigDecimal lifeMultiplier = safe(config == null ? null : config.getTargetLifeMultiplier(), new BigDecimal("5"));
        BigDecimal ratioWarn = safe(config == null ? null : config.getPremiumRatioWarn(), new BigDecimal("0.10"));
        BigDecimal ratioDanger = safe(config == null ? null : config.getPremiumRatioDanger(), new BigDecimal("0.20"));

        BigDecimal existingMedical = profile == null ? BigDecimal.ZERO : safe(profile.getExistingCoverMedical());
        BigDecimal existingAccident = profile == null ? BigDecimal.ZERO : safe(profile.getExistingCoverAccident());
        BigDecimal existingCi = profile == null ? BigDecimal.ZERO : safe(profile.getExistingCoverCi());
        BigDecimal existingLife = profile == null ? BigDecimal.ZERO : safe(profile.getExistingCoverLife());

        BigDecimal medicalGap = gap(targetMedical, existingMedical);
        BigDecimal accidentGap = gap(targetAccident, existingAccident);
        BigDecimal ciGap = gap(targetCi, existingCi);
        BigDecimal lifeTarget = income.multiply(lifeMultiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal lifeGap = gap(lifeTarget, existingLife);

        BigDecimal premiumRatio = BigDecimal.ZERO;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            premiumRatio = annualPremium.divide(income, 6, RoundingMode.HALF_UP);
        } else {
            warnings.add(InsuranceWarningCodes.INCOME_MISSING_FOR_PREMIUM_RATIO);
        }

        if (premiumRatio.compareTo(ratioDanger) >= 0) {
            warnings.add(InsuranceWarningCodes.PREMIUM_RATIO_DANGER);
        } else if (premiumRatio.compareTo(ratioWarn) >= 0) {
            warnings.add(InsuranceWarningCodes.PREMIUM_RATIO_WARN);
        }

        if (medicalGap.compareTo(BigDecimal.ZERO) > 0
                || accidentGap.compareTo(BigDecimal.ZERO) > 0
                || ciGap.compareTo(BigDecimal.ZERO) > 0
                || lifeGap.compareTo(BigDecimal.ZERO) > 0) {
            warnings.add(InsuranceWarningCodes.INSURANCE_GAP_HIGH);
        }

        metrics.put("medicalGap", medicalGap);
        metrics.put("accidentGap", accidentGap);
        metrics.put("ciGap", ciGap);
        metrics.put("lifeGap", lifeGap);
        metrics.put("lifeTarget", lifeTarget);
        metrics.put("premiumRatio", premiumRatio);

        return new InsuranceGapCalcResult(metrics, warnings);
    }

    private BigDecimal gap(BigDecimal target, BigDecimal existing) {
        if (target == null) return BigDecimal.ZERO;
        BigDecimal diff = target.subtract(existing == null ? BigDecimal.ZERO : existing);
        return diff.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : diff;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal safe(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    public record InsuranceGapCalcResult(Map<String, Object> metrics, List<String> warnings) {}
}
