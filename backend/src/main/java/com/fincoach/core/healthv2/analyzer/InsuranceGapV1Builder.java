package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.advice.WarningCollector;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InsuranceGapV1Builder {

    public InsuranceGapV1Result build(FcInsuranceProfileEntity profile,
                                      Map<String, Object> coverage,
                                      BigDecimal annualIncome,
                                      BigDecimal monthlyIncome,
                                      BigDecimal totalDebt,
                                      BigDecimal emergencyFundMonths,
                                      ScoreRuleSnapshot ruleSnapshot) {
        InsuranceGapV1Result result = new InsuranceGapV1Result();
        WarningCollector warnings = new WarningCollector();
        InsuranceGapThresholds thresholds = InsuranceGapThresholds.fromSnapshot(ruleSnapshot);
        warnings.addAll(thresholds.getWarnings());

        Integer dependentsCount = profile == null ? null : profile.getDependentsCount();
        Integer childrenCount = profile == null ? null : profile.getChildrenCount();
        int dependents = dependentsCount == null ? 0 : dependentsCount;
        int children = childrenCount == null ? 0 : childrenCount;
        boolean hasDependentsInfo = dependentsCount != null || childrenCount != null;
        if (!hasDependentsInfo) {
            warnings.add(InsuranceWarningCodes.MISSING_DEPENDENTS_INFO, "dependents info missing");
        }
        boolean highResponsibility = dependents > 0 || children > 0;

        BigDecimal income = resolveAnnualIncome(profile, annualIncome, monthlyIncome);
        if (income == null || income.compareTo(BigDecimal.ZERO) <= 0) {
            warnings.add(InsuranceWarningCodes.INSURANCE_INCOME_MISSING, "annual income missing");
            income = null;
        }

        BigDecimal premiumTotal = readDecimal(coverage, "annualPremiumTotal");
        if (premiumTotal == null) {
            warnings.add(InsuranceWarningCodes.INSURANCE_PREMIUM_MISSING, "annual premium missing");
        }

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("annualIncome", income);
        inputs.put("dependentsCount", hasDependentsInfo ? (dependents + children) : null);
        inputs.put("annualPremiumTotal", premiumTotal);

        Map<String, Object> gaps = new LinkedHashMap<>();
        GapResult medicalGap = buildMedicalGap(coverage, warnings);
        gaps.put("medical", medicalGap.toMap());

        GapResult accidentGap = buildNumericGap("ACCIDENT",
                income, thresholds.getAccidentMultiplier(),
                readDecimal(coverage, "accident"),
                warnings);
        gaps.put("accident", accidentGap.toMap());

        GapResult ciGap = buildNumericGap("CRITICAL_ILLNESS",
                income, thresholds.getCriticalIllnessMultiplier(),
                readDecimal(coverage, "criticalIllness"),
                warnings);
        gaps.put("criticalIllness", ciGap.toMap());

        GapResult lifeGap = buildLifeGap(income, thresholds, totalDebt, highResponsibility,
                readDecimal(coverage, "life"), warnings);
        gaps.put("life", lifeGap.toMap());

        PremiumRatioResult premiumRatio = buildPremiumRatio(premiumTotal, income, thresholds.getPremiumRatioMax(), warnings);

        List<Map<String, Object>> priority = buildPriority(highResponsibility, totalDebt, emergencyFundMonths, warnings);

        String summaryLevel = resolveSummaryLevel(medicalGap, accidentGap, ciGap, lifeGap, premiumRatio, warnings);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("inputs", inputs);
        metrics.put("gaps", gaps);
        metrics.put("premiumRatio", premiumRatio.toMap());
        metrics.put("priority", priority);
        metrics.put("warnings", warnings.codes());

        Map<String, Object> advice = buildAdvice(summaryLevel, premiumRatio, medicalGap, accidentGap, ciGap, lifeGap,
                emergencyFundMonths, warnings);

        result.setMetrics(metrics);
        result.setAdvice(advice);
        result.setWarnings(warnings.codes());
        result.setWarningDetails(warnings.detailMaps());
        result.setSummaryLevel(summaryLevel);
        result.setPremiumRatio(premiumRatio.value == null ? null : premiumRatio.value.doubleValue());
        GapResult topGap = maxGap(medicalGap, accidentGap, ciGap, lifeGap);
        if (topGap != null) {
            result.setTopGapType(topGap.type);
            result.setTopGapValue(topGap.gap == null ? null : topGap.gap.doubleValue());
        }

        return result;
    }

    private GapResult buildMedicalGap(Map<String, Object> coverage, WarningCollector warnings) {
        Object hasMedicalObj = coverage == null ? null : coverage.get("hasMedical");
        if (hasMedicalObj == null) {
            warnings.add(InsuranceWarningCodes.MISSING_INSURANCE_MEDICAL_INFO, "hasMedical missing");
            return GapResult.missing("MEDICAL");
        }
        boolean hasMedical = toBoolean(hasMedicalObj);
        if (hasMedical) {
            return GapResult.ok("MEDICAL", BigDecimal.ONE, BigDecimal.ONE);
        }
        return GapResult.low("MEDICAL", BigDecimal.ONE, BigDecimal.ZERO);
    }

    private GapResult buildNumericGap(String type,
                                      BigDecimal income,
                                      double multiplier,
                                      BigDecimal current,
                                      WarningCollector warnings) {
        if (income == null) {
            warnings.add(InsuranceWarningCodes.INSURANCE_INCOME_MISSING, "income missing for " + type);
            return GapResult.recommendedMissing(type);
        }
        BigDecimal recommended = income.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP);
        if (current == null) {
            warnings.add(InsuranceWarningCodes.INSURANCE_CURRENT_MISSING_PREFIX + type, "current missing");
            return GapResult.currentMissing(type, recommended);
        }
        return GapResult.from(type, recommended, current);
    }

    private GapResult buildLifeGap(BigDecimal income,
                                   InsuranceGapThresholds thresholds,
                                   BigDecimal totalDebt,
                                   boolean highResponsibility,
                                   BigDecimal current,
                                   WarningCollector warnings) {
        if (income == null) {
            warnings.add(InsuranceWarningCodes.INSURANCE_INCOME_MISSING, "income missing for LIFE");
            return GapResult.recommendedMissing("LIFE");
        }
        BigDecimal multiplier = BigDecimal.valueOf(thresholds.getLifeMultiplier());
        BigDecimal recommended = income.multiply(multiplier);
        if (highResponsibility) {
            recommended = recommended.add(income.multiply(BigDecimal.valueOf(thresholds.getLifeDependentBonusMultiplier())));
        }
        if (totalDebt != null && totalDebt.compareTo(BigDecimal.ZERO) > 0) {
            recommended = recommended.add(totalDebt);
        }
        recommended = recommended.setScale(2, RoundingMode.HALF_UP);
        if (current == null) {
            warnings.add(InsuranceWarningCodes.INSURANCE_CURRENT_MISSING_PREFIX + "LIFE", "current missing");
            return GapResult.currentMissing("LIFE", recommended);
        }
        return GapResult.from("LIFE", recommended, current);
    }

    private PremiumRatioResult buildPremiumRatio(BigDecimal premiumTotal,
                                                 BigDecimal income,
                                                 double maxRatio,
                                                 WarningCollector warnings) {
        if (premiumTotal == null || income == null || income.compareTo(BigDecimal.ZERO) <= 0) {
            if (premiumTotal == null) {
                warnings.add(InsuranceWarningCodes.INSURANCE_PREMIUM_MISSING, "premium missing");
            }
            if (income == null) {
                warnings.add(InsuranceWarningCodes.INSURANCE_INCOME_MISSING, "income missing for premium ratio");
            }
            return PremiumRatioResult.missing(maxRatio);
        }
        BigDecimal ratio = premiumTotal.divide(income, 4, RoundingMode.HALF_UP);
        String level = ratio.doubleValue() > maxRatio ? "HIGH" : "OK";
        return new PremiumRatioResult(ratio, level, maxRatio);
    }

    private List<Map<String, Object>> buildPriority(boolean highResponsibility,
                                                    BigDecimal totalDebt,
                                                    BigDecimal emergencyFundMonths,
                                                    WarningCollector warnings) {
        List<String> order = new ArrayList<>();
        if (highResponsibility || (totalDebt != null && totalDebt.compareTo(BigDecimal.ZERO) > 0)) {
            order.add("MEDICAL");
            order.add("LIFE");
            order.add("CI");
            order.add("ACCIDENT");
        } else {
            order.add("MEDICAL");
            order.add("ACCIDENT");
            order.add("CI");
            order.add("LIFE");
        }

        List<Map<String, Object>> priority = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            String type = order.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", type);
            item.put("priorityRank", i + 1);
            item.put("reason", buildPriorityReason(type, highResponsibility, totalDebt, emergencyFundMonths));
            priority.add(item);
        }
        return priority;
    }

    private String buildPriorityReason(String type,
                                       boolean highResponsibility,
                                       BigDecimal totalDebt,
                                       BigDecimal emergencyFundMonths) {
        if ("MEDICAL".equals(type) && emergencyFundMonths != null && emergencyFundMonths.doubleValue() < 3) {
            return "应急金不足，优先补齐医疗基础";
        }
        if ("LIFE".equals(type) && (highResponsibility || (totalDebt != null && totalDebt.compareTo(BigDecimal.ZERO) > 0))) {
            return "有家庭责任或负债，寿险优先级提升";
        }
        return "默认优先级排序";
    }

    private Map<String, Object> buildAdvice(String summaryLevel,
                                            PremiumRatioResult premiumRatio,
                                            GapResult medical,
                                            GapResult accident,
                                            GapResult ci,
                                            GapResult life,
                                            BigDecimal emergencyFundMonths,
                                            WarningCollector warnings) {
        Map<String, Object> advice = new LinkedHashMap<>();
        List<Map<String, Object>> advices = new ArrayList<>();

        if ("LOW".equals(medical.level)) {
            advices.add(adviceItem("INSURANCE_MEDICAL_GAP", "优先补齐医疗基础",
                    "医疗保障不足，建议配置基础医疗/百万医疗险。", 1,
                    List.of("medical")));
        }
        addGapAdvice(advices, "ACCIDENT", accident, "意外保障不足", 2);
        addGapAdvice(advices, "CRITICAL_ILLNESS", ci, "重疾保障不足", 2);
        addGapAdvice(advices, "LIFE", life, "寿险保障不足", 2);

        if ("HIGH".equals(premiumRatio.level)) {
            advices.add(adviceItem("INSURANCE_PREMIUM_HIGH", "保费占比偏高",
                    "年保费占收入比例超过阈值，建议优化险种结构，避免过度投保。", 2,
                    List.of("premiumRatio")));
        }

        if (emergencyFundMonths != null && emergencyFundMonths.doubleValue() < 3) {
            advices.add(adviceItem("INSURANCE_EMERGENCY_LOW", "先补应急金",
                    "应急金不足3个月，建议先补充应急储备再完善保障。", 1,
                    List.of("emergencyFundMonths")));
        }

        advice.put("summaryLevel", summaryLevel);
        advice.put("advices", advices);
        advice.put("warnings", warnings.codes());
        return advice;
    }

    private void addGapAdvice(List<Map<String, Object>> advices,
                              String type,
                              GapResult gap,
                              String title,
                              int priority) {
        if (gap == null || !"LOW".equals(gap.level)) return;
        advices.add(adviceItem("INSURANCE_GAP_" + type, title,
                type + " 缺口=" + formatAmount(gap.gap) + "，建议补齐保额。",
                priority, List.of(type.toLowerCase())));
    }

    private Map<String, Object> adviceItem(String code,
                                           String title,
                                           String detail,
                                           int priority,
                                           List<String> relatedMetrics) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("title", title);
        item.put("detail", detail);
        item.put("priority", priority);
        item.put("relatedMetrics", relatedMetrics == null ? new ArrayList<>() : relatedMetrics);
        return item;
    }

    private GapResult maxGap(GapResult... gaps) {
        GapResult max = null;
        for (GapResult gap : gaps) {
            if (gap == null || gap.gap == null) continue;
            if (max == null || gap.gap.compareTo(max.gap) > 0) {
                max = gap;
            }
        }
        return max;
    }

    private String resolveSummaryLevel(GapResult medical,
                                       GapResult accident,
                                       GapResult ci,
                                       GapResult life,
                                       PremiumRatioResult premiumRatio,
                                       WarningCollector warnings) {
        boolean hasLow = hasLow(medical) || hasLow(accident) || hasLow(ci) || hasLow(life) || "HIGH".equals(premiumRatio.level);
        boolean hasMissing = hasMissing(medical) || hasMissing(accident) || hasMissing(ci) || hasMissing(life)
                || "MISSING".equals(premiumRatio.level) || !warnings.codes().isEmpty();
        if (hasLow) return "HIGH";
        if (hasMissing) return "MED";
        return "LOW";
    }

    private boolean hasLow(GapResult gap) {
        return gap != null && "LOW".equals(gap.level);
    }

    private boolean hasMissing(GapResult gap) {
        return gap != null && "MISSING".equals(gap.level);
    }

    private BigDecimal resolveAnnualIncome(FcInsuranceProfileEntity profile,
                                           BigDecimal annualIncome,
                                           BigDecimal monthlyIncome) {
        if (annualIncome != null && annualIncome.compareTo(BigDecimal.ZERO) > 0) {
            return annualIncome;
        }
        if (profile != null && profile.getAnnualIncome() != null && profile.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            return profile.getAnnualIncome();
        }
        if (monthlyIncome != null && monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            return monthlyIncome.multiply(BigDecimal.valueOf(12));
        }
        return null;
    }

    private BigDecimal readDecimal(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String formatAmount(BigDecimal value) {
        if (value == null) return "N/A";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static class GapResult {
        private final String type;
        private final BigDecimal recommended;
        private final BigDecimal current;
        private final BigDecimal gap;
        private final String level;

        private GapResult(String type,
                          BigDecimal recommended,
                          BigDecimal current,
                          BigDecimal gap,
                          String level) {
            this.type = type;
            this.recommended = recommended;
            this.current = current;
            this.gap = gap;
            this.level = level;
        }

        static GapResult from(String type, BigDecimal recommended, BigDecimal current) {
            BigDecimal gap = recommended.subtract(current);
            if (gap.compareTo(BigDecimal.ZERO) < 0) gap = BigDecimal.ZERO;
            String level = gap.compareTo(BigDecimal.ZERO) > 0 ? "LOW" : "OK";
            return new GapResult(type, recommended, current, gap, level);
        }

        static GapResult low(String type, BigDecimal recommended, BigDecimal current) {
            BigDecimal gap = recommended.subtract(current);
            if (gap.compareTo(BigDecimal.ZERO) < 0) gap = BigDecimal.ZERO;
            return new GapResult(type, recommended, current, gap, "LOW");
        }

        static GapResult ok(String type, BigDecimal recommended, BigDecimal current) {
            return new GapResult(type, recommended, current, BigDecimal.ZERO, "OK");
        }

        static GapResult currentMissing(String type, BigDecimal recommended) {
            return new GapResult(type, recommended, null, null, "MISSING");
        }

        static GapResult recommendedMissing(String type) {
            return new GapResult(type, null, null, null, "MISSING");
        }

        static GapResult missing(String type) {
            return new GapResult(type, null, null, null, "MISSING");
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("recommended", recommended);
            map.put("current", current);
            map.put("gap", gap);
            map.put("level", level);
            return map;
        }
    }

    private static class PremiumRatioResult {
        private final BigDecimal value;
        private final String level;
        private final double threshold;

        PremiumRatioResult(BigDecimal value, String level, double threshold) {
            this.value = value;
            this.level = level;
            this.threshold = threshold;
        }

        static PremiumRatioResult missing(double threshold) {
            return new PremiumRatioResult(null, "MISSING", threshold);
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", value);
            map.put("level", level);
            map.put("threshold", BigDecimal.valueOf(threshold).setScale(2, RoundingMode.HALF_UP));
            return map;
        }
    }
}
