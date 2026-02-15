package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.entity.FcCashflowEntity;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 债务优化器 (M3)
 *
 * 输出 advice.debtPlan：优先级排序 + 还款建议 + 提前还贷 vs 投资比较
 */
@Slf4j
@Component
public class DebtOptimizer {

    private static final BigDecimal HIGH_RATE_THRESHOLD = new BigDecimal("0.12");

    /**
     * @param liabilities    用户负债列表
     * @param cashflow       最新现金流（nullable）
     * @param emergencyMonths 应急月数（nullable）
     * @param monthlySurplus  月结余（nullable）
     * @return debtPlan map
     */
    public Map<String, Object> optimize(List<FcLiabilityEntity> liabilities,
                                         FcCashflowEntity cashflow,
                                         BigDecimal emergencyMonths,
                                         BigDecimal monthlySurplus) {
        Map<String, Object> plan = new LinkedHashMap<>();

        BigDecimal totalMonthlyDebt = cashflow != null ? cashflow.getMonthlyDebtPayment() : null;
        BigDecimal dti = null;
        if (cashflow != null && cashflow.getIncome() != null && cashflow.getIncome().compareTo(BigDecimal.ZERO) > 0
                && totalMonthlyDebt != null) {
            dti = totalMonthlyDebt.divide(cashflow.getIncome(), 4, RoundingMode.HALF_UP);
        }

        plan.put("dti", dti);
        plan.put("monthlyDebtPayment", totalMonthlyDebt);

        if (liabilities == null || liabilities.isEmpty()) {
            plan.put("priorityList", Collections.emptyList());
            plan.put("recommendation", buildEmptyRecommendation("无负债，无需优化"));
            plan.put("comparePayoffVsInvest", null);
            return plan;
        }

        // 1. 按利率降序排优先级
        List<Map<String, Object>> priorityList = new ArrayList<>();
        List<FcLiabilityEntity> sorted = new ArrayList<>(liabilities);
        sorted.sort((a, b) -> {
            BigDecimal ra = a.getInterestRate() != null ? a.getInterestRate() : BigDecimal.ZERO;
            BigDecimal rb = b.getInterestRate() != null ? b.getInterestRate() : BigDecimal.ZERO;
            return rb.compareTo(ra);
        });

        int priority = 1;
        for (FcLiabilityEntity l : sorted) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("liabilityId", l.getId());
            item.put("type", l.getType());
            item.put("principal", l.getPrincipal());
            item.put("interestRate", l.getInterestRate());
            item.put("priority", priority++);
            item.put("reason", describeReason(l));
            priorityList.add(item);
        }
        plan.put("priorityList", priorityList);

        // 2. 确定模式
        boolean cashflowDanger = (emergencyMonths != null && emergencyMonths.compareTo(new BigDecimal("3")) < 0)
                || (monthlySurplus != null && monthlySurplus.compareTo(BigDecimal.ZERO) <= 0);

        boolean hasHighRate = sorted.stream()
                .anyMatch(l -> l.getInterestRate() != null && l.getInterestRate().compareTo(HIGH_RATE_THRESHOLD) >= 0);

        String mode;
        if (cashflowDanger) {
            mode = "CASHFLOW_PROTECT";
        } else if (hasHighRate) {
            mode = "PAY_HIGH_INTEREST_FIRST";
        } else {
            mode = "BALANCED";
        }

        // 3. 生成 actions
        List<Map<String, Object>> actions = new ArrayList<>();
        double surplus = monthlySurplus != null ? monthlySurplus.doubleValue() : 0;

        if ("CASHFLOW_PROTECT".equals(mode)) {
            for (FcLiabilityEntity l : sorted) {
                Map<String, Object> act = new LinkedHashMap<>();
                act.put("action", "HOLD");
                act.put("liabilityId", l.getId());
                act.put("note", "现金流紧张，暂不建议额外还款，优先保障应急金");
                actions.add(act);
            }
        } else {
            for (FcLiabilityEntity l : sorted) {
                Map<String, Object> act = new LinkedHashMap<>();
                if (l.getInterestRate() != null && l.getInterestRate().compareTo(HIGH_RATE_THRESHOLD) >= 0 && surplus > 0) {
                    double extra = Math.min(surplus, l.getPrincipal() != null ? l.getPrincipal().doubleValue() * 0.05 : 1000);
                    act.put("action", "EXTRA_PAY");
                    act.put("liabilityId", l.getId());
                    act.put("amount", Math.round(extra));
                    act.put("source", "monthlySurplus");
                    surplus -= extra;
                } else {
                    act.put("action", "HOLD");
                    act.put("liabilityId", l.getId());
                    act.put("note", "暂不建议提前还款（利率低/现金流需优先保障）");
                }
                actions.add(act);
            }
        }

        Map<String, Object> recommendation = new LinkedHashMap<>();
        recommendation.put("mode", mode);
        recommendation.put("actions", actions);
        plan.put("recommendation", recommendation);

        // 4. 提前还贷 vs 投资比较
        BigDecimal avgRate = calcWeightedAvgRate(liabilities);
        if (avgRate != null) {
            Map<String, Object> compare = new LinkedHashMap<>();
            compare.put("debtRate", avgRate);
            compare.put("investReturnRange", Arrays.asList(0.03, 0.08));

            String suggestion;
            String reason;
            if (avgRate.compareTo(new BigDecimal("0.06")) > 0) {
                suggestion = "倾向还贷";
                reason = "负债利率(" + avgRate.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%)高于多数稳健投资预期收益";
            } else if (avgRate.compareTo(new BigDecimal("0.04")) < 0) {
                suggestion = "倾向投资";
                reason = "负债利率较低，历史数据显示长期投资大概率跑赢此利率";
            } else {
                suggestion = "视风险等级而定";
                reason = "负债利率处于中间区间，保守型用户建议还贷，进取型可考虑投资";
            }
            compare.put("suggestion", suggestion);
            compare.put("reason", reason);
            plan.put("comparePayoffVsInvest", compare);
        } else {
            plan.put("comparePayoffVsInvest", null);
        }

        return plan;
    }

    private String describeReason(FcLiabilityEntity l) {
        if (l.getInterestRate() == null) return "利率未知";
        if (l.getInterestRate().compareTo(HIGH_RATE_THRESHOLD) >= 0) return "高息优先还款";
        if (l.getInterestRate().compareTo(new BigDecimal("0.06")) >= 0) return "中高息，建议关注";
        return "低息长期，可维持最低还款";
    }

    private BigDecimal calcWeightedAvgRate(List<FcLiabilityEntity> liabilities) {
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (FcLiabilityEntity l : liabilities) {
            if (l.getInterestRate() != null && l.getPrincipal() != null) {
                totalPrincipal = totalPrincipal.add(l.getPrincipal());
                weightedSum = weightedSum.add(l.getInterestRate().multiply(l.getPrincipal()));
            }
        }
        if (totalPrincipal.compareTo(BigDecimal.ZERO) <= 0) return null;
        return weightedSum.divide(totalPrincipal, 6, RoundingMode.HALF_UP);
    }

    private Map<String, Object> buildEmptyRecommendation(String note) {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("mode", "NONE");
        rec.put("actions", Collections.emptyList());
        rec.put("note", note);
        return rec;
    }
}
