package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 评分引擎 (M3)
 *
 * 输出 riskScore / healthScore / behaviorScore + breakdown (可解释)
 * 每个分数 0-100，各由多个维度加权组成
 */
@Slf4j
@Component
public class ScoreEngine {

    /**
     * 计算三维评分 + 可解释 breakdown
     */
    public Map<String, Object> compute(List<FcAssetEntity> assets,
                                        List<FcLiabilityEntity> liabilities,
                                        FcCashflowEntity cashflow,
                                        List<FcGoalEntity> goals,
                                        FcInsuranceProfileEntity insurance,
                                        Map<String, Object> performanceResult,
                                        Map<String, Object> allocation,
                                        Map<String, Object> concentration,
                                        BigDecimal emergencyMonths,
                                        BigDecimal dti,
                                        BigDecimal totalAssets,
                                        BigDecimal totalDebt,
                                        Map<String, Object> behaviorStats) {

        Map<String, Object> result = new LinkedHashMap<>();

        // ========= Health Score (5 维) =========
        List<Map<String, Object>> healthBreakdown = new ArrayList<>();

        // 1. Liquidity (权重 25)
        int liqScore = scoreLiquidity(emergencyMonths);
        healthBreakdown.add(dim("Liquidity", liqScore, 25, describeLiquidity(emergencyMonths)));

        // 2. DebtHealth (权重 25)
        int debtScore = scoreDebtHealth(dti, liabilities);
        healthBreakdown.add(dim("DebtHealth", debtScore, 25, describeDebtHealth(dti)));

        // 3. Diversification (权重 20)
        int divScore = scoreDiversification(allocation, concentration);
        healthBreakdown.add(dim("Diversification", divScore, 20, describeDiversification(concentration)));

        // 4. RiskAdjustedReturn (权重 15)
        int rarScore = scoreRiskAdjustedReturn(performanceResult);
        healthBreakdown.add(dim("RiskAdjustedReturn", rarScore, 15, describeRAR(performanceResult)));

        // 5. Behavior (权重 15)
        int behaviorScore = scoreBehavior(behaviorStats);
        String behaviorReason = describeBehavior(behaviorStats);
        healthBreakdown.add(dim("Behavior", behaviorScore, 15, behaviorReason));

        int healthScore = calcWeighted(healthBreakdown);
        result.put("healthScore", clamp(healthScore));

        // ========= Risk Score (5 维，越高风险越大) =========
        List<Map<String, Object>> riskBreakdown = new ArrayList<>();

        // 1. EquityRatio (权重 25)
        int eqScore = scoreEquityRatio(allocation);
        riskBreakdown.add(dim("EquityRatio", eqScore, 25, describeEquityRatio(allocation)));

        // 2. MaxDrawdown (权重 20)
        int ddScore = scoreMaxDrawdown(performanceResult);
        riskBreakdown.add(dim("MaxDrawdown", ddScore, 20, describeMaxDrawdown(performanceResult)));

        // 3. Concentration (权重 20)
        int concScore = scoreConcentration(concentration);
        riskBreakdown.add(dim("Concentration", concScore, 20, describeConcentration(concentration)));

        // 4. CashflowFragility (权重 20)
        int fragScore = scoreCashflowFragility(dti, emergencyMonths);
        riskBreakdown.add(dim("CashflowFragility", fragScore, 20, describeCashflowFragility(dti, emergencyMonths)));

        // 5. NetWorthNegative (权重 15)
        BigDecimal netWorth = totalAssets.subtract(totalDebt);
        int nwScore = netWorth.compareTo(BigDecimal.ZERO) < 0 ? 90 : 20;
        riskBreakdown.add(dim("NetWorthNegative", nwScore, 15,
                netWorth.compareTo(BigDecimal.ZERO) < 0 ? "净资产为负，风险显著" : "净资产为正"));

        int riskScore = calcWeighted(riskBreakdown);
        result.put("riskScore", clamp(riskScore));

        // ========= Behavior Score (M5-A 规则版) =========
        List<Map<String, Object>> behaviorBreakdown = new ArrayList<>();
        int finalBehaviorScore = clamp(behaviorScore);
        behaviorBreakdown.add(dim("BehaviorRule30d", finalBehaviorScore, 100, behaviorReason));
        result.put("behaviorScore", finalBehaviorScore);

        // ========= Breakdown =========
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("health", healthBreakdown);
        breakdown.put("risk", riskBreakdown);
        breakdown.put("behavior", behaviorBreakdown);
        result.put("breakdown", breakdown);

        return result;
    }

    // ============================= Health 维度 =============================

    private int scoreLiquidity(BigDecimal emergencyMonths) {
        if (emergencyMonths == null) return 30;
        double m = emergencyMonths.doubleValue();
        if (m >= 6) return 95;
        if (m >= 3) return 75;
        if (m >= 1) return 50;
        return 20;
    }

    private String describeLiquidity(BigDecimal emergencyMonths) {
        if (emergencyMonths == null) return "未录入现金流，无法评估应急金";
        double m = emergencyMonths.doubleValue();
        if (m >= 6) return "应急金充裕，覆盖" + emergencyMonths + "个月";
        if (m >= 3) return "应急金基本达标（" + emergencyMonths + "个月），建议逐步补至6个月";
        return "应急金不足（仅" + emergencyMonths + "个月），需优先补充";
    }

    private int scoreDebtHealth(BigDecimal dti, List<FcLiabilityEntity> liabilities) {
        int score = 90;
        if (dti != null) {
            double d = dti.doubleValue();
            if (d > 0.5) score -= 50;
            else if (d > 0.4) score -= 35;
            else if (d > 0.3) score -= 15;
        }
        // 高息债扣分
        if (liabilities != null) {
            long highRateCount = liabilities.stream()
                    .filter(l -> l.getInterestRate() != null && l.getInterestRate().compareTo(new BigDecimal("0.12")) >= 0)
                    .count();
            score -= (int)(highRateCount * 10);
        }
        return Math.max(10, score);
    }

    private String describeDebtHealth(BigDecimal dti) {
        if (dti == null) return "未录入负债或现金流数据";
        double d = dti.doubleValue();
        String pct = dti.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%";
        if (d > 0.5) return "DTI=" + pct + "，超过50%严重警戒线";
        if (d > 0.4) return "DTI=" + pct + "，超过40%警戒线";
        if (d > 0.3) return "DTI=" + pct + "，接近警戒区间";
        return "DTI=" + pct + "，债务负担健康";
    }

    private int scoreDiversification(Map<String, Object> allocation, Map<String, Object> concentration) {
        if (allocation == null || allocation.isEmpty()) return 40;
        int typeCount = allocation.size();
        int score = 50;
        if (typeCount >= 4) score += 25;
        else if (typeCount >= 3) score += 15;
        else if (typeCount >= 2) score += 5;

        // 集中度惩罚
        if (concentration != null && concentration.get("topRatio") != null) {
            double topRatio = toDouble(concentration.get("topRatio"));
            if (topRatio > 0.7) score -= 20;
            else if (topRatio > 0.5) score -= 10;
        }
        return Math.max(10, Math.min(100, score));
    }

    private String describeDiversification(Map<String, Object> concentration) {
        if (concentration == null || concentration.isEmpty()) return "无资产配置数据";
        Object topType = concentration.get("topType");
        Object topRatio = concentration.get("topRatio");
        if (topRatio != null) {
            double r = toDouble(topRatio);
            if (r > 0.7) return "最大类别" + topType + "占比" + pct(r) + "，集中度过高";
            if (r > 0.5) return "最大类别" + topType + "占比" + pct(r) + "，建议适度分散";
        }
        return "资产配置分散度尚可";
    }

    private int scoreRiskAdjustedReturn(Map<String, Object> perf) {
        if (perf == null) return 50;
        Double sharpe = toDoubleObj(perf.get("sharpe"));
        Double maxDD = toDoubleObj(perf.get("maxDrawdown"));
        int score = 50;
        if (sharpe != null) {
            if (sharpe > 1.0) score += 25;
            else if (sharpe > 0.5) score += 15;
            else if (sharpe > 0) score += 5;
            else score -= 15;
        }
        if (maxDD != null) {
            if (maxDD < 0.1) score += 15;
            else if (maxDD < 0.2) score += 5;
            else if (maxDD > 0.4) score -= 15;
            else if (maxDD > 0.3) score -= 5;
        }
        return Math.max(10, Math.min(100, score));
    }

    private String describeRAR(Map<String, Object> perf) {
        if (perf == null) return "无性能指标数据";
        Double sharpe = toDoubleObj(perf.get("sharpe"));
        Double maxDD = toDoubleObj(perf.get("maxDrawdown"));
        StringBuilder sb = new StringBuilder();
        if (sharpe != null) sb.append("Sharpe=").append(sharpe);
        if (maxDD != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("MaxDD=").append(pct(maxDD));
        }
        if (sb.length() == 0) return "性能指标暂无数据";
        return sb.toString();
    }

    // ============================= Risk 维度 =============================

    private int scoreEquityRatio(Map<String, Object> allocation) {
        if (allocation == null || allocation.isEmpty()) return 30;
        double equity = 0;
        for (Map.Entry<String, Object> e : allocation.entrySet()) {
            String type = e.getKey();
            if ("STOCK".equals(type) || "ETF".equals(type)) {
                equity += toDouble(e.getValue());
            }
        }
        if (equity > 0.8) return 90;
        if (equity > 0.6) return 70;
        if (equity > 0.4) return 50;
        if (equity > 0.2) return 35;
        return 20;
    }

    private String describeEquityRatio(Map<String, Object> allocation) {
        if (allocation == null) return "无配置数据";
        double equity = 0;
        for (Map.Entry<String, Object> e : allocation.entrySet()) {
            if ("STOCK".equals(e.getKey()) || "ETF".equals(e.getKey())) {
                equity += toDouble(e.getValue());
            }
        }
        return "权益类占比" + pct(equity);
    }

    private int scoreMaxDrawdown(Map<String, Object> perf) {
        if (perf == null) return 40;
        Double maxDD = toDoubleObj(perf.get("maxDrawdown"));
        if (maxDD == null) return 40;
        if (maxDD > 0.5) return 95;
        if (maxDD > 0.3) return 75;
        if (maxDD > 0.2) return 55;
        if (maxDD > 0.1) return 35;
        return 20;
    }

    private String describeMaxDrawdown(Map<String, Object> perf) {
        if (perf == null) return "无回撤数据";
        Double maxDD = toDoubleObj(perf.get("maxDrawdown"));
        if (maxDD == null) return "无回撤数据";
        return "最大回撤" + pct(maxDD);
    }

    private int scoreConcentration(Map<String, Object> concentration) {
        if (concentration == null || concentration.isEmpty()) return 40;
        double topRatio = toDouble(concentration.get("topRatio"));
        if (topRatio > 0.8) return 85;
        if (topRatio > 0.6) return 65;
        if (topRatio > 0.4) return 45;
        return 25;
    }

    private String describeConcentration(Map<String, Object> concentration) {
        if (concentration == null) return "无集中度数据";
        return "最大类别占比" + pct(toDouble(concentration.get("topRatio")));
    }

    private int scoreCashflowFragility(BigDecimal dti, BigDecimal emergencyMonths) {
        int score = 20;
        if (dti != null && dti.doubleValue() > 0.4) score += 30;
        else if (dti != null && dti.doubleValue() > 0.3) score += 15;
        if (emergencyMonths != null && emergencyMonths.doubleValue() < 1) score += 30;
        else if (emergencyMonths != null && emergencyMonths.doubleValue() < 3) score += 15;
        return Math.min(100, score);
    }

    private String describeCashflowFragility(BigDecimal dti, BigDecimal emergencyMonths) {
        List<String> parts = new ArrayList<>();
        if (dti != null && dti.doubleValue() > 0.4)
            parts.add("DTI偏高(" + dti.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%)");
        if (emergencyMonths != null && emergencyMonths.doubleValue() < 3)
            parts.add("应急金不足(" + emergencyMonths + "个月)");
        if (parts.isEmpty()) return "现金流韧性良好";
        return String.join(", ", parts);
    }

    // ============================= Behavior 维度 (M5) =============================

    private int scoreBehavior(Map<String, Object> stats) {
        int eventCount = intValue(stats, "eventCount30d");
        if (eventCount < 3) {
            return 60;
        }
        int score = 60;
        int reportGenerateCount = intValue(stats, "reportGenerateCount30d");
        int rebalanceConfirmCount = intValue(stats, "rebalanceConfirmCount30d");
        if (rebalanceConfirmCount > 0) {
            score = Math.min(85, score + 15);
        }
        if (reportGenerateCount >= 10) {
            score -= 10;
        }
        if (rebalanceConfirmCount >= 3) {
            score += 5;
        }
        return clamp(score);
    }

    private String describeBehavior(Map<String, Object> stats) {
        int eventCount = intValue(stats, "eventCount30d");
        int reportGenerateCount = intValue(stats, "reportGenerateCount30d");
        int rebalanceConfirmCount = intValue(stats, "rebalanceConfirmCount30d");
        if (eventCount < 3) {
            return "近30天行为事件不足3条（当前" + eventCount + "条），采用基线60分";
        }
        List<String> reasons = new ArrayList<>();
        reasons.add("基线60分");
        reasons.add("近30天事件数=" + eventCount);
        if (rebalanceConfirmCount > 0) {
            reasons.add("存在再平衡确认(+15)");
        } else {
            reasons.add("近30天无再平衡确认");
        }
        if (reportGenerateCount >= 10) {
            reasons.add("报告生成次数>=10(-10)");
        }
        if (rebalanceConfirmCount >= 3) {
            reasons.add("再平衡确认次数>=3(+5)");
        }
        return String.join("；", reasons);
    }

    private int scoreDataSufficiency(Map<String, Object> stats) {
        int eventCount = intValue(stats, "eventCount");
        if (eventCount < 3) return 55;
        if (eventCount < 8) return 70;
        if (eventCount < 20) return 80;
        return 90;
    }

    private String describeDataSufficiency(Map<String, Object> stats) {
        int eventCount = intValue(stats, "eventCount");
        if (eventCount < 3) return "数据不足(<3条)，采用基线";
        return "近30天事件数=" + eventCount;
    }

    private int scoreDiscipline(Map<String, Object> stats) {
        int reportGenerate7d = intValue(stats, "reportGenerate7d");
        int rebalanceConfirm7d = intValue(stats, "rebalanceConfirm7d");
        boolean rebalanceFollowThrough = boolValue(stats, "rebalanceFollowThrough");
        if (reportGenerate7d == 0) return 65;
        if (rebalanceFollowThrough) return 85;
        if (rebalanceConfirm7d > 0) return 75;
        return 55;
    }

    private String describeDiscipline(Map<String, Object> stats) {
        int reportGenerate7d = intValue(stats, "reportGenerate7d");
        int rebalanceConfirm7d = intValue(stats, "rebalanceConfirm7d");
        if (reportGenerate7d == 0) return "近7天无新建议生成";
        if (rebalanceConfirm7d > 0) return "近7天存在再平衡确认";
        return "近7天有建议但无执行确认";
    }

    private int scoreStability(Map<String, Object> stats) {
        int assetUpdateCount = intValue(stats, "assetUpdateCount");
        int largeAdjustCount = intValue(stats, "largeAdjustCount");
        int score = 80;
        if (assetUpdateCount > 10) score -= 20;
        else if (assetUpdateCount > 6) score -= 10;
        if (largeAdjustCount >= 3) score -= 20;
        else if (largeAdjustCount >= 1) score -= 10;
        return clamp(score);
    }

    private String describeStability(Map<String, Object> stats) {
        int assetUpdateCount = intValue(stats, "assetUpdateCount");
        int largeAdjustCount = intValue(stats, "largeAdjustCount");
        return "资产调整次数=" + assetUpdateCount + ", 大幅调整次数=" + largeAdjustCount;
    }

    private int intValue(Map<String, Object> stats, String key) {
        if (stats == null) return 0;
        Object value = stats.get(key);
        if (value instanceof Number number) return number.intValue();
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean boolValue(Map<String, Object> stats, String key) {
        if (stats == null) return false;
        Object value = stats.get(key);
        if (value instanceof Boolean b) return b;
        if (value == null) return false;
        return Boolean.parseBoolean(value.toString());
    }

    // ============================= 工具 =============================

    private Map<String, Object> dim(String key, int score, int weight, String reason) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("key", key);
        d.put("score", clamp(score));
        d.put("weight", weight);
        d.put("reason", reason);
        return d;
    }

    private int calcWeighted(List<Map<String, Object>> dims) {
        double total = 0;
        double weightSum = 0;
        for (Map<String, Object> d : dims) {
            int score = (Integer) d.get("score");
            int weight = (Integer) d.get("weight");
            total += score * weight;
            weightSum += weight;
        }
        if (weightSum == 0) return 50;
        return (int) Math.round(total / weightSum);
    }

    private int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    private double toDouble(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof BigDecimal) return ((BigDecimal) obj).doubleValue();
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return 0; }
    }

    private Double toDoubleObj(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return null; }
    }

    private String pct(double v) {
        return BigDecimal.valueOf(v * 100).setScale(1, RoundingMode.HALF_UP) + "%";
    }
}
