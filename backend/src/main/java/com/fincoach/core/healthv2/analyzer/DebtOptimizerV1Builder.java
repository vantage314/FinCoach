package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.advice.WarningCollector;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DebtOptimizerV1Builder {

    public DebtOptimizerV1Result build(List<FcLiabilityEntity> liabilities,
                                       BigDecimal monthlySurplus,
                                       String stressLevel,
                                       ScoreRuleSnapshot ruleSnapshot) {
        DebtOptimizerV1Result result = new DebtOptimizerV1Result();
        WarningCollector warnings = new WarningCollector();
        DebtOptimizerThresholds thresholds = DebtOptimizerThresholds.fromSnapshot(ruleSnapshot);
        warnings.addAll(thresholds.getWarnings());

        String summaryLevel = stressLevel == null ? "LOW" : stressLevel;
        result.setSummaryLevel(summaryLevel);
        result.setMonthlySurplus(monthlySurplus);

        List<FcLiabilityEntity> debts = liabilities == null ? new ArrayList<>() : new ArrayList<>(liabilities);
        if (debts.isEmpty()) {
            warnings.add(DebtOptimizerWarningCodes.DEBT_ITEMS_EMPTY, "liabilities empty");
            result.setStrategy("N/A");
            result.setBudgetForExtraPayment(BigDecimal.ZERO);
            result.setPlan(new ArrayList<>());
            result.setTradeoffHint(tradeoffHint("INSUFFICIENT_DATA", "债务数据缺失，无法评估"));
            finalizeResult(result, warnings);
            return result;
        }

        boolean hasRate = false;
        boolean missingRate = false;
        for (FcLiabilityEntity debt : debts) {
            if (debt == null) continue;
            if (debt.getInterestRate() == null) {
                missingRate = true;
            } else {
                hasRate = true;
            }
        }

        String strategy = resolveStrategy(thresholds.getDefaultStrategy(), hasRate, missingRate, warnings);
        result.setStrategy(strategy);

        BigDecimal budget = calcBudget(monthlySurplus, thresholds.getExtraPayRatio());
        boolean surplusPositive = monthlySurplus != null && monthlySurplus.compareTo(BigDecimal.ZERO) > 0;
        if (!surplusPositive) {
            warnings.add(DebtOptimizerWarningCodes.DEBT_OPTIMIZER_SURPLUS_NOT_POSITIVE, "monthly surplus <= 0");
            budget = BigDecimal.ZERO;
        }
        result.setBudgetForExtraPayment(budget);

        List<FcLiabilityEntity> ordered = sortDebts(debts, strategy);
        List<Map<String, Object>> plan = new ArrayList<>();
        FcLiabilityEntity top = ordered.isEmpty() ? null : ordered.get(0);
        result.setTopDebtName(resolveDebtName(top));
        for (int i = 0; i < ordered.size(); i++) {
            FcLiabilityEntity debt = ordered.get(i);
            BigDecimal extraPay = (i == 0) ? budget : BigDecimal.ZERO;
            BigDecimal payoffMonths = estimatePayoffMonths(debt, extraPay, warnings);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("debtId", debt == null || debt.getId() == null ? "" : debt.getId().toString());
            item.put("name", resolveDebtName(debt));
            item.put("priorityRank", i + 1);
            item.put("reason", buildReason(debt, strategy, i == 0));
            item.put("recommendedExtraPayment", scale(extraPay));
            item.put("estimatedMonthsToPayoff", payoffMonths == null ? null : payoffMonths.intValue());
            plan.add(item);
        }
        result.setPlan(plan);

        Map<String, Object> tradeoff = buildTradeoffHint(
                thresholds, topRate(ordered), summaryLevel, surplusPositive, warnings);
        result.setTradeoffHint(tradeoff);

        finalizeResult(result, warnings);
        return result;
    }

    private void finalizeResult(DebtOptimizerV1Result result, WarningCollector warnings) {
        result.setWarnings(warnings.codes());
        result.setWarningDetails(warnings.detailMaps());
        if (result.getPlan() == null) {
            result.setPlan(new ArrayList<>());
        }
        if (result.getTradeoffHint() == null) {
            result.setTradeoffHint(new LinkedHashMap<>());
        }
    }

    private String resolveStrategy(String configured, boolean hasRate, boolean missingRate, WarningCollector warnings) {
        String base = configured == null ? "AVALANCHE" : configured;
        if (!hasRate && missingRate) {
            warnings.add(DebtOptimizerWarningCodes.DEBT_RATE_MISSING_ALL, "all rates missing");
            return "SNOWBALL";
        }
        if (hasRate && missingRate) {
            warnings.add(DebtOptimizerWarningCodes.DEBT_RATE_PARTIAL, "partial rates missing");
            return "MIXED";
        }
        if (!"AVALANCHE".equals(base) && !"SNOWBALL".equals(base)) {
            warnings.add("SCORE_RULE_PARAM_INVALID:DEBT_OPTIMIZER_STRATEGY",
                    "invalid strategy=" + base);
            return "AVALANCHE";
        }
        return base;
    }

    private List<FcLiabilityEntity> sortDebts(List<FcLiabilityEntity> debts, String strategy) {
        if ("SNOWBALL".equals(strategy)) {
            return debts.stream()
                    .sorted(Comparator.comparing(d -> safeDecimal(d == null ? null : d.getPrincipal()), Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }
        if ("MIXED".equals(strategy)) {
            List<FcLiabilityEntity> withRate = new ArrayList<>();
            List<FcLiabilityEntity> withoutRate = new ArrayList<>();
            for (FcLiabilityEntity debt : debts) {
                if (debt != null && debt.getInterestRate() != null) {
                    withRate.add(debt);
                } else {
                    withoutRate.add(debt);
                }
            }
            withRate.sort(Comparator.comparing((FcLiabilityEntity d) -> safeDecimal(d.getInterestRate())).reversed());
            withoutRate.sort(Comparator.comparing(d -> safeDecimal(d == null ? null : d.getPrincipal()), Comparator.nullsLast(Comparator.naturalOrder())));
            List<FcLiabilityEntity> combined = new ArrayList<>();
            combined.addAll(withRate);
            combined.addAll(withoutRate);
            return combined;
        }
        return debts.stream()
                .sorted(Comparator.comparing((FcLiabilityEntity d) -> safeDecimal(d == null ? null : d.getInterestRate()), Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    private BigDecimal calcBudget(BigDecimal monthlySurplus, double ratio) {
        if (monthlySurplus == null) return BigDecimal.ZERO;
        BigDecimal budget = monthlySurplus.multiply(BigDecimal.valueOf(ratio));
        if (budget.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        return budget.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal estimatePayoffMonths(FcLiabilityEntity debt, BigDecimal extraPay, WarningCollector warnings) {
        if (debt == null || debt.getPrincipal() == null) {
            warnings.add(DebtOptimizerWarningCodes.DEBT_PAYOFF_ESTIMATE_UNAVAILABLE, "principal missing");
            return null;
        }
        BigDecimal payment = debt.getMonthlyPayment();
        if (payment == null) {
            warnings.add(DebtOptimizerWarningCodes.DEBT_PAYOFF_ESTIMATE_UNAVAILABLE, "monthly payment missing");
            return null;
        }
        BigDecimal totalPayment = payment.add(extraPay == null ? BigDecimal.ZERO : extraPay);
        if (totalPayment.compareTo(BigDecimal.ZERO) <= 0) {
            warnings.add(DebtOptimizerWarningCodes.DEBT_PAYOFF_ESTIMATE_UNAVAILABLE, "payment <= 0");
            return null;
        }
        return debt.getPrincipal().divide(totalPayment, 0, RoundingMode.CEILING);
    }

    private Map<String, Object> buildTradeoffHint(DebtOptimizerThresholds thresholds,
                                                  BigDecimal maxRate,
                                                  String stressLevel,
                                                  boolean surplusPositive,
                                                  WarningCollector warnings) {
        if ("HIGH".equals(stressLevel) || !surplusPositive) {
            return tradeoffHint("PAY_DEBT_FIRST", "现金流压力较大，优先偿债与稳住现金流");
        }
        if (maxRate == null) {
            warnings.add(DebtOptimizerWarningCodes.TRADEOFF_DEBT_RATE_MISSING, "rate missing");
            return tradeoffHint("INSUFFICIENT_DATA", "缺少债务利率，无法评估还贷/投资取舍");
        }
        Double expectedReturn = thresholds.getExpectedReturn();
        List<String> codes = warnings.codes();
        boolean expectedReturnMissing = codes.contains("SCORE_RULE_PARAM_MISSING:DEBT_OPTIMIZER_EXPECTED_RETURN")
                || codes.contains("SCORE_RULE_PARAM_INVALID:DEBT_OPTIMIZER_EXPECTED_RETURN");
        if (expectedReturn == null || expectedReturnMissing) {
            warnings.add(DebtOptimizerWarningCodes.TRADEOFF_EXPECTED_RETURN_MISSING, "expected return missing");
            return tradeoffHint("INSUFFICIENT_DATA", "缺少预期收益率，无法评估还贷/投资取舍");
        }
        double margin = thresholds.getReturnMargin();
        double debtRate = maxRate.doubleValue();
        String detail = "债务最高利率=" + formatPercent(debtRate)
                + "，预期收益=" + formatPercent(expectedReturn)
                + "，差异阈值=" + formatPercent(margin);
        if (debtRate >= expectedReturn + margin) {
            return tradeoffHint("PAY_DEBT_FIRST", detail);
        }
        if (debtRate <= expectedReturn - margin) {
            return tradeoffHint("INVEST_FIRST", detail);
        }
        return tradeoffHint("BALANCED", detail);
    }

    private Map<String, Object> tradeoffHint(String recommendation, String detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("recommendation", recommendation);
        map.put("detail", detail);
        return map;
    }

    private String buildReason(FcLiabilityEntity debt, String strategy, boolean isTop) {
        String base;
        if ("SNOWBALL".equals(strategy)) {
            base = "按本金由小到大排序";
        } else if ("MIXED".equals(strategy)) {
            base = debt != null && debt.getInterestRate() != null ? "利率优先" : "缺失利率，按本金排序";
        } else {
            base = "按利率由高到低排序";
        }
        if (isTop) {
            return base + "，优先集中还款";
        }
        return base + "，先保留最低还款";
    }

    private String resolveDebtName(FcLiabilityEntity debt) {
        if (debt == null) return "DEBT";
        if (debt.getType() != null && !debt.getType().isBlank()) {
            return debt.getType();
        }
        return debt.getId() == null ? "DEBT" : "DEBT#" + debt.getId();
    }

    private BigDecimal topRate(List<FcLiabilityEntity> debts) {
        BigDecimal max = null;
        for (FcLiabilityEntity debt : debts) {
            if (debt == null || debt.getInterestRate() == null) continue;
            if (max == null || debt.getInterestRate().compareTo(max) > 0) {
                max = debt.getInterestRate();
            }
        }
        return max;
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        if (value == null) return null;
        return value;
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatPercent(double value) {
        return BigDecimal.valueOf(value * 100).setScale(1, RoundingMode.HALF_UP) + "%";
    }
}
