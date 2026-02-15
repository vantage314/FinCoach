package com.fincoach.core.healthv2.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Rebalance Advisor (M2)
 *
 * 根据用户风险评分、现金流安全条件生成 targetAllocation + 调仓建议
 *
 * 规则：
 * - emergencyMonths < 3 或 DTI > 0.5 → 稳健模式（提高 CASH/BOND）
 * - riskScore <= 35 → 保守
 * - 35 < riskScore <= 65 → 平衡
 * - riskScore > 65 → 进取
 * 执行逻辑：优先 monthlySurplus 买入，偏离超 sellIfDeviation 才建议卖出
 */
@Slf4j
@Component
public class RebalanceAdvisor {

    @Value("${healthv2.rebalance.rebalanceDeviation:0.05}")
    private double rebalanceDeviation;

    @Value("${healthv2.rebalance.sellIfDeviation:0.10}")
    private double sellIfDeviation;

    /**
     * 生成再平衡建议
     *
     * @param currentAllocation 当前资产占比 (type -> ratio BigDecimal)
     * @param riskScore         风险评分 (0-100)
     * @param emergencyMonths   应急月数 (nullable)
     * @param dti               DTI (nullable)
     * @param monthlySurplus    月结余 (nullable)
     * @param totalAssets       总资产
     * @return rebalance advice map
     */
    public Map<String, Object> advise(Map<String, Object> currentAllocation,
                                       int riskScore,
                                       BigDecimal emergencyMonths,
                                       BigDecimal dti,
                                       BigDecimal monthlySurplus,
                                       BigDecimal totalAssets) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (currentAllocation == null || currentAllocation.isEmpty() ||
                totalAssets == null || totalAssets.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("targetAllocation", null);
            result.put("currentAllocation", currentAllocation);
            result.put("deviations", Collections.emptyList());
            result.put("strategy", null);
            result.put("actions", Collections.emptyList());
            result.put("thresholds", buildThresholds());
            result.put("reason", "资产数据不足，无法生成建议");
            return result;
        }

        // 1. 确定策略模式
        String strategy;
        Map<String, Double> target;

        boolean cashflowDanger = (emergencyMonths != null && emergencyMonths.compareTo(new BigDecimal("3")) < 0)
                || (dti != null && dti.compareTo(new BigDecimal("0.5")) > 0);

        if (cashflowDanger) {
            strategy = "CASHFLOW_FIRST";
            target = buildConservativeTarget();
            log.info("[M2-Rebalance] 现金流紧张，走稳健模式 (emergencyMonths={}, dti={})",
                    emergencyMonths, dti);
        } else if (riskScore <= 35) {
            strategy = "CONSERVATIVE";
            target = buildConservativeTarget();
        } else if (riskScore <= 65) {
            strategy = "BALANCED";
            target = buildBalancedTarget();
        } else {
            strategy = "AGGRESSIVE";
            target = buildAggressiveTarget();
        }

        result.put("targetAllocation", target);
        result.put("currentAllocation", currentAllocation);
        result.put("strategy", strategy);

        // 2. 计算偏差
        List<Map<String, Object>> deviations = new ArrayList<>();
        Set<String> allTypes = new LinkedHashSet<>();
        allTypes.addAll(target.keySet());
        allTypes.addAll(currentAllocation.keySet());

        for (String type : allTypes) {
            double curr = toDouble(currentAllocation.get(type));
            double tgt = target.getOrDefault(type, 0.0);
            double diff = curr - tgt;

            if (Math.abs(diff) >= rebalanceDeviation) {
                Map<String, Object> dev = new LinkedHashMap<>();
                dev.put("type", type);
                dev.put("current", round4(curr));
                dev.put("target", round4(tgt));
                dev.put("diff", round4(diff));
                deviations.add(dev);
            }
        }
        result.put("deviations", deviations);

        // 3. 生成操作建议
        List<Map<String, Object>> actions = new ArrayList<>();
        double surplus = (monthlySurplus != null) ? monthlySurplus.doubleValue() : 0;
        double totalAmt = totalAssets.doubleValue();

        // 找出需要增配的类别
        List<Map<String, Object>> buyNeeds = new ArrayList<>();
        List<Map<String, Object>> sellNeeds = new ArrayList<>();

        for (Map<String, Object> dev : deviations) {
            double diff = (Double) dev.get("diff");
            String type = (String) dev.get("type");

            if (diff < -rebalanceDeviation) {
                // 需要增配
                Map<String, Object> act = new LinkedHashMap<>();
                act.put("action", "BUY");
                act.put("type", type);
                double amountNeeded = Math.abs(diff) * totalAmt;

                if (surplus > 0) {
                    double buyAmount = Math.min(surplus, amountNeeded);
                    act.put("amount", Math.round(buyAmount));
                    act.put("source", "monthlySurplus");
                    surplus -= buyAmount;
                } else {
                    act.put("amount", Math.round(amountNeeded));
                    act.put("source", "rebalance");
                }
                buyNeeds.add(act);
            } else if (diff > sellIfDeviation) {
                // 超配且偏离超过 sellIfDeviation，建议减持
                Map<String, Object> act = new LinkedHashMap<>();
                act.put("action", "SELL");
                act.put("type", type);
                act.put("amount", Math.round(diff * totalAmt));
                act.put("note", "偏离超过" + (int)(sellIfDeviation * 100) + "%阈值");
                sellNeeds.add(act);
            } else if (diff > rebalanceDeviation) {
                // 轻微超配，建议持有
                Map<String, Object> act = new LinkedHashMap<>();
                act.put("action", "HOLD");
                act.put("type", type);
                act.put("note", "偏离未达卖出阈值，暂不操作");
                actions.add(act);
            }
        }

        actions.addAll(0, buyNeeds);
        actions.addAll(sellNeeds);
        result.put("actions", actions);
        result.put("thresholds", buildThresholds());

        return result;
    }

    // ============================= 目标配置模板 =============================

    private Map<String, Double> buildConservativeTarget() {
        Map<String, Double> t = new LinkedHashMap<>();
        t.put("CASH", 0.30);
        t.put("BOND", 0.40);
        t.put("STOCK", 0.15);
        t.put("GOLD", 0.10);
        t.put("OTHER", 0.05);
        return t;
    }

    private Map<String, Double> buildBalancedTarget() {
        Map<String, Double> t = new LinkedHashMap<>();
        t.put("CASH", 0.15);
        t.put("BOND", 0.30);
        t.put("STOCK", 0.35);
        t.put("GOLD", 0.10);
        t.put("ETF", 0.05);
        t.put("OTHER", 0.05);
        return t;
    }

    private Map<String, Double> buildAggressiveTarget() {
        Map<String, Double> t = new LinkedHashMap<>();
        t.put("CASH", 0.10);
        t.put("BOND", 0.15);
        t.put("STOCK", 0.45);
        t.put("ETF", 0.15);
        t.put("GOLD", 0.10);
        t.put("OTHER", 0.05);
        return t;
    }

    // ============================= 工具 =============================

    private Map<String, Object> buildThresholds() {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("rebalanceDeviation", rebalanceDeviation);
        t.put("sellIfDeviation", sellIfDeviation);
        return t;
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof BigDecimal) return ((BigDecimal) obj).doubleValue();
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
