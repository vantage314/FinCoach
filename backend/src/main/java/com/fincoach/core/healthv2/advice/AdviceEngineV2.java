package com.fincoach.core.healthv2.advice;

import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.entity.FcCashflowEntity;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateRegistry;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateSnapshot;
import com.fincoach.core.healthv2.rules.ScoreRuleSetRegistry;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdviceEngineV2 {

    private final ScoreRuleSetRegistry scoreRuleSetRegistry;
    private final RebalanceTemplateRegistry rebalanceTemplateRegistry;

    public AdviceEngineV2(ScoreRuleSetRegistry scoreRuleSetRegistry,
                          RebalanceTemplateRegistry rebalanceTemplateRegistry) {
        this.scoreRuleSetRegistry = scoreRuleSetRegistry;
        this.rebalanceTemplateRegistry = rebalanceTemplateRegistry;
    }

    public AdviceEngineResult build(List<FcAssetEntity> assets,
                                    List<FcLiabilityEntity> liabilities,
                                    FcCashflowEntity cashflow,
                                    Map<String, Object> allocation,
                                    BigDecimal totalAssets) {
        AdviceEngineResult result = new AdviceEngineResult();
        List<String> warnings = new ArrayList<>();

        ScoreRuleSnapshot scoreSnapshot = scoreRuleSetRegistry == null ? null : scoreRuleSetRegistry.get();
        AdviceThresholds thresholds = AdviceThresholds.fromSnapshot(scoreSnapshot);
        warnings.addAll(thresholds.getWarnings());

        RebalanceTemplateSnapshot templateSnapshot = rebalanceTemplateRegistry == null
                ? null
                : rebalanceTemplateRegistry.getActive();
        if (templateSnapshot != null && templateSnapshot.getWarnings() != null) {
            warnings.addAll(templateSnapshot.getWarnings());
        }

        BigDecimal liquidAssets = sumCashAssets(assets);
        if (liquidAssets.compareTo(BigDecimal.ZERO) == 0 && totalAssets != null && totalAssets.compareTo(BigDecimal.ZERO) > 0) {
            liquidAssets = totalAssets;
            warnings.add("LIQUID_ASSET_FALLBACK_TOTAL");
        }

        BigDecimal monthlyExpense = calcMonthlyExpense(cashflow);
        BigDecimal income = cashflow == null ? null : cashflow.getIncome();
        BigDecimal monthlyDebtPayment = cashflow == null ? null : cashflow.getMonthlyDebtPayment();

        Double emergencyMonths = null;
        if (monthlyExpense != null && monthlyExpense.compareTo(BigDecimal.ZERO) > 0) {
            emergencyMonths = liquidAssets.divide(monthlyExpense, 2, RoundingMode.HALF_UP).doubleValue();
        } else {
            warnings.add("EMERGENCY_MONTHS_UNAVAILABLE");
        }

        Double debtPaymentRatio = null;
        if (income != null && income.compareTo(BigDecimal.ZERO) > 0 && monthlyDebtPayment != null) {
            debtPaymentRatio = monthlyDebtPayment.divide(income, 4, RoundingMode.HALF_UP).doubleValue();
        } else {
            warnings.add("DEBT_PAYMENT_RATIO_UNAVAILABLE");
        }

        Double surplusRate = null;
        if (income != null && income.compareTo(BigDecimal.ZERO) > 0 && monthlyExpense != null) {
            BigDecimal surplus = income.subtract(monthlyExpense);
            surplusRate = surplus.divide(income, 4, RoundingMode.HALF_UP).doubleValue();
        } else {
            warnings.add("SURPLUS_RATE_UNAVAILABLE");
        }

        result.getAdvices().add(buildEmergencyAdvice(emergencyMonths, thresholds.getEmergencyMonthsMin()));
        result.getAdvices().add(buildDebtAdvice(debtPaymentRatio, thresholds.getDebtPaymentRatioMax()));
        result.getAdvices().add(buildSurplusAdvice(surplusRate, thresholds.getSurplusRateMin()));
        result.getAdvices().add(buildRebalanceAdvice(allocation, templateSnapshot, thresholds.getRebalanceThreshold(), warnings));

        Map<String, Object> meta = result.getMeta();
        if (scoreSnapshot != null) {
            meta.put("ruleSetCode", scoreSnapshot.getCode());
            meta.put("ruleSetVersion", scoreSnapshot.getVersion());
            meta.put("ruleSetSource", scoreSnapshot.getSource());
        }
        if (templateSnapshot != null) {
            meta.put("templateCode", templateSnapshot.getCode());
            meta.put("templateVersion", templateSnapshot.getVersion());
            meta.put("templateSource", templateSnapshot.getSource());
        }
        meta.put("thresholds", thresholds.toMap());
        meta.put("warnings", dedup(warnings));

        return result;
    }

    private AdviceDTO buildEmergencyAdvice(Double emergencyMonths, int minMonths) {
        AdviceDTO advice = base("EMERGENCY_FUND_LOW", "应急金储备");
        if (emergencyMonths == null) {
            advice.setPriority("P1");
            advice.setReason("现金流或资产数据缺失，无法评估应急金覆盖月数");
            advice.setImpact("可能低估短期流动性风险");
            advice.setAction("补充现金流/资产数据，并建立6个月以上应急金");
            advice.setEvidence(Map.of("emergencyMonths", null, "minMonths", minMonths));
            return advice;
        }
        if (emergencyMonths < minMonths) {
            advice.setPriority("P0");
            advice.setReason("应急金覆盖仅" + emergencyMonths + "个月，低于阈值" + minMonths + "个月");
            advice.setImpact("短期流动性风险偏高");
            advice.setAction("优先补齐应急金至" + minMonths + "个月以上");
        } else {
            advice.setPriority("P2");
            advice.setReason("应急金覆盖" + emergencyMonths + "个月，已达到阈值");
            advice.setImpact("流动性风险可控");
            advice.setAction("持续维护应急金规模");
        }
        advice.setEvidence(Map.of("emergencyMonths", emergencyMonths, "minMonths", minMonths));
        return advice;
    }

    private AdviceDTO buildDebtAdvice(Double debtPaymentRatio, double maxRatio) {
        AdviceDTO advice = base("DEBT_PAYMENT_RATIO_HIGH", "债务压力");
        if (debtPaymentRatio == null) {
            advice.setPriority("P1");
            advice.setReason("缺少收入或负债月供数据，无法评估债务压力");
            advice.setImpact("可能低估债务风险");
            advice.setAction("补充现金流/负债月供数据");
            advice.setEvidence(Map.of("debtPaymentRatio", null, "maxRatio", maxRatio));
            return advice;
        }
        if (debtPaymentRatio > maxRatio) {
            advice.setPriority("P0");
            advice.setReason("负债月供占收入比例为" + pct(debtPaymentRatio) + "，高于阈值" + pct(maxRatio));
            advice.setImpact("现金流压力较大，偿债风险升高");
            advice.setAction("优化债务结构或提高收入，降低月供占比");
        } else {
            advice.setPriority("P2");
            advice.setReason("负债月供占比为" + pct(debtPaymentRatio) + "，低于阈值");
            advice.setImpact("债务压力可控");
            advice.setAction("继续保持健康负债水平");
        }
        advice.setEvidence(Map.of("debtPaymentRatio", debtPaymentRatio, "maxRatio", maxRatio));
        return advice;
    }

    private AdviceDTO buildSurplusAdvice(Double surplusRate, double minRate) {
        AdviceDTO advice = base("SURPLUS_RATE_LOW", "结余率");
        if (surplusRate == null) {
            advice.setPriority("P1");
            advice.setReason("缺少收入或支出数据，无法计算结余率");
            advice.setImpact("难以判断长期储蓄能力");
            advice.setAction("补充现金流数据");
            advice.setEvidence(Map.of("surplusRate", null, "minRate", minRate));
            return advice;
        }
        if (surplusRate < minRate) {
            advice.setPriority("P1");
            advice.setReason("结余率为" + pct(surplusRate) + "，低于阈值" + pct(minRate));
            advice.setImpact("长期资产积累速度偏慢");
            advice.setAction("控制支出或提升收入，提高结余率");
        } else {
            advice.setPriority("P2");
            advice.setReason("结余率为" + pct(surplusRate) + "，达到阈值");
            advice.setImpact("储蓄能力良好");
            advice.setAction("保持当前收支结构");
        }
        advice.setEvidence(Map.of("surplusRate", surplusRate, "minRate", minRate));
        return advice;
    }

    private AdviceDTO buildRebalanceAdvice(Map<String, Object> allocation,
                                           RebalanceTemplateSnapshot templateSnapshot,
                                           double threshold,
                                           List<String> warnings) {
        AdviceDTO advice = base("REBALANCE_RECOMMENDATION", "再平衡建议");
        if (templateSnapshot == null || templateSnapshot.getTargets().isEmpty()) {
            warnings.add("REBALANCE_TEMPLATE_MISSING");
            advice.setPriority("P2");
            advice.setReason("未找到可用的再平衡模板");
            advice.setImpact("无法生成目标配置建议");
            advice.setAction("检查并发布再平衡模板");
            return advice;
        }
        if (allocation == null || allocation.isEmpty()) {
            warnings.add("REBALANCE_INPUT_MISSING");
            advice.setPriority("P2");
            advice.setReason("当前资产配置数据缺失");
            advice.setImpact("无法判断偏离情况");
            advice.setAction("补充资产分类数据后再评估");
            return advice;
        }

        Map<String, Double> targets = templateSnapshot.getTargets();
        List<Map<String, Object>> deviations = new ArrayList<>();
        for (String type : unionKeys(targets, allocation)) {
            double current = toDouble(allocation.get(type));
            double target = targets.getOrDefault(type, 0.0);
            double diff = current - target;
            if (Math.abs(diff) >= threshold) {
                Map<String, Object> dev = new LinkedHashMap<>();
                dev.put("type", type);
                dev.put("current", round4(current));
                dev.put("target", round4(target));
                dev.put("diff", round4(diff));
                deviations.add(dev);
            }
        }

        Map<String, Object> evidence = new HashMap<>();
        evidence.put("threshold", threshold);
        evidence.put("deviations", deviations);
        evidence.put("targets", targets);
        advice.setEvidence(evidence);

        if (deviations.isEmpty()) {
            advice.setPriority("P2");
            advice.setReason("配置偏离未超过阈值" + pct(threshold));
            advice.setImpact("当前配置较接近目标");
            advice.setAction("保持配置，持续观察");
        } else {
            advice.setPriority("P1");
            advice.setReason("存在" + deviations.size() + "项配置偏离超过阈值" + pct(threshold));
            advice.setImpact("风险暴露可能与目标不一致");
            advice.setAction("优先通过新增资金或小幅调仓回归目标配置");
        }
        return advice;
    }

    private AdviceDTO base(String code, String title) {
        AdviceDTO advice = new AdviceDTO();
        advice.setCode(code);
        advice.setTitle(title);
        return advice;
    }

    private BigDecimal sumCashAssets(List<FcAssetEntity> assets) {
        if (assets == null) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (FcAssetEntity a : assets) {
            if (a != null && "CASH".equals(a.getType()) && a.getAmount() != null) {
                sum = sum.add(a.getAmount());
            }
        }
        return sum;
    }

    private BigDecimal calcMonthlyExpense(FcCashflowEntity cashflow) {
        if (cashflow == null) return null;
        BigDecimal total = BigDecimal.ZERO;
        if (cashflow.getFixedExpense() != null) total = total.add(cashflow.getFixedExpense());
        if (cashflow.getVariableExpense() != null) total = total.add(cashflow.getVariableExpense());
        if (cashflow.getMonthlyDebtPayment() != null) total = total.add(cashflow.getMonthlyDebtPayment());
        return total;
    }

    private List<String> unionKeys(Map<String, Double> targets, Map<String, Object> allocation) {
        List<String> keys = new ArrayList<>();
        if (targets != null) {
            for (String k : targets.keySet()) {
                if (!keys.contains(k)) keys.add(k);
            }
        }
        if (allocation != null) {
            for (String k : allocation.keySet()) {
                if (!keys.contains(k)) keys.add(k);
            }
        }
        return keys;
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof BigDecimal bd) return bd.doubleValue();
        if (obj instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private String pct(double v) {
        return BigDecimal.valueOf(v * 100).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private List<String> dedup(List<String> input) {
        List<String> result = new ArrayList<>();
        for (String w : input) {
            if (w == null) continue;
            if (!result.contains(w)) result.add(w);
        }
        return result;
    }
}
