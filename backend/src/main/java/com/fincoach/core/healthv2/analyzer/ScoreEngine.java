package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.advice.WarningCollector;
import com.fincoach.core.healthv2.entity.*;
import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;
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
                                        Map<String, Object> behaviorStats,
                                        ScoreRuleSnapshot ruleSnapshot) {

        Map<String, Object> result = new LinkedHashMap<>();

        ScoreRuleSnapshot snapshot = ruleSnapshot != null ? ruleSnapshot : defaultSnapshot();
        WarningCollector warningCollector = new WarningCollector();
        if (snapshot != null) {
            warningCollector.addAll(snapshot.getWarnings());
        }

        double wSharpe = toDouble(snapshot.getDecimal(ScoreRuleDefaults.W_SHARPE, new BigDecimal("0.35")));
        double wMdd = toDouble(snapshot.getDecimal(ScoreRuleDefaults.W_MDD, new BigDecimal("0.35")));
        double wVol = toDouble(snapshot.getDecimal(ScoreRuleDefaults.W_VOL, new BigDecimal("0.15")));
        double wDiv = toDouble(snapshot.getDecimal(ScoreRuleDefaults.W_DIVERSIFICATION, new BigDecimal("0.15")));

        double sharpeOk = toDouble(snapshot.getDecimal(ScoreRuleDefaults.SHARPE_OK, new BigDecimal("0.5")));
        double sharpeGood = toDouble(snapshot.getDecimal(ScoreRuleDefaults.SHARPE_GOOD, new BigDecimal("1.0")));
        double mddOk = toDouble(snapshot.getDecimal(ScoreRuleDefaults.MDD_OK, new BigDecimal("0.2")));
        double mddBad = toDouble(snapshot.getDecimal(ScoreRuleDefaults.MDD_BAD, new BigDecimal("0.4")));

        double cashFlowRateMin = toDouble(snapshot.getDecimal(ScoreRuleDefaults.CASH_FLOW_RATE_MIN, new BigDecimal("0.1")));
        double debtRatioMax = toDouble(snapshot.getDecimal(ScoreRuleDefaults.DEBT_RATIO_MAX, new BigDecimal("0.5")));
        int emergencyMonthsMin = snapshot.getInt(ScoreRuleDefaults.EMERGENCY_MONTHS_MIN, 3);
        double assetLiabilityRatioGood = toDouble(snapshot.getDecimal(ScoreRuleDefaults.ASSET_LIABILITY_RATIO_GOOD, new BigDecimal("2.0")));
        double liquidityRatioGood = toDouble(snapshot.getDecimal(ScoreRuleDefaults.LIQUIDITY_RATIO_GOOD, new BigDecimal("0.2")));
        double wHealthLiquidity = toDouble(snapshot.getDecimal(ScoreRuleDefaults.HEALTH_W_LIQUIDITY, new BigDecimal("0.25")));
        double wHealthDebt = toDouble(snapshot.getDecimal(ScoreRuleDefaults.HEALTH_W_DEBT, new BigDecimal("0.25")));
        double wHealthDiversification = toDouble(snapshot.getDecimal(ScoreRuleDefaults.HEALTH_W_DIVERSIFICATION, new BigDecimal("0.20")));
        double wHealthRar = toDouble(snapshot.getDecimal(ScoreRuleDefaults.HEALTH_W_RAR, new BigDecimal("0.15")));
        double wHealthBehavior = toDouble(snapshot.getDecimal(ScoreRuleDefaults.HEALTH_W_BEHAVIOR, new BigDecimal("0.15")));

        int riskHighMin = snapshot.getInt(ScoreRuleDefaults.RISK_LEVEL_HIGH_MIN, 70);
        int riskMedMin = snapshot.getInt(ScoreRuleDefaults.RISK_LEVEL_MED_MIN, 40);
        int healthHighMin = snapshot.getInt(ScoreRuleDefaults.HEALTH_LEVEL_HIGH_MIN, 70);
        int healthMedMin = snapshot.getInt(ScoreRuleDefaults.HEALTH_LEVEL_MED_MIN, 40);
        int behaviorHighMin = snapshot.getInt(ScoreRuleDefaults.BEHAVIOR_LEVEL_HIGH_MIN, 70);
        int behaviorMedMin = snapshot.getInt(ScoreRuleDefaults.BEHAVIOR_LEVEL_MED_MIN, 40);

        BigDecimal cashAssets = sumCashAssets(assets);
        BigDecimal liquidityRatio = calcRatio(cashAssets, totalAssets);
        BigDecimal assetLiabilityRatio = calcRatio(totalAssets, totalDebt);
        Double cashFlowRate = calcCashFlowRate(cashflow);

        List<String> baseWarnings = new ArrayList<>(warningCollector.codes());

        // ========= Health Score (5 维) =========
        List<Map<String, Object>> healthBreakdown = new ArrayList<>();
        List<Map<String, Object>> healthBreakdownV1 = new ArrayList<>();

        // 1. Liquidity (权重 25)
        int liqScore = scoreLiquidity(emergencyMonths, liquidityRatio, liquidityRatioGood);
        int liqWeight;
        int debtWeight;
        int divWeight;
        int rarWeight;
        int behWeight;
        double healthWeightSum = wHealthLiquidity + wHealthDebt + wHealthDiversification + wHealthRar + wHealthBehavior;
        if (healthWeightSum <= 0) {
            wHealthLiquidity = 0.25;
            wHealthDebt = 0.25;
            wHealthDiversification = 0.20;
            wHealthRar = 0.15;
            wHealthBehavior = 0.15;
            healthWeightSum = wHealthLiquidity + wHealthDebt + wHealthDiversification + wHealthRar + wHealthBehavior;
        }
        liqWeight = weightPercent(wHealthLiquidity, healthWeightSum);
        debtWeight = weightPercent(wHealthDebt, healthWeightSum);
        divWeight = weightPercent(wHealthDiversification, healthWeightSum);
        rarWeight = weightPercent(wHealthRar, healthWeightSum);
        behWeight = Math.max(0, 100 - liqWeight - debtWeight - divWeight - rarWeight);

        healthBreakdown.add(dim("Liquidity", liqScore, liqWeight, describeLiquidity(emergencyMonths, liquidityRatio, liquidityRatioGood)));
        healthBreakdownV1.add(breakdownItem(
                "LIQUIDITY", "Liquidity", liqWeight,
                rawOrNa(emergencyMonths), liqScore,
                describeLiquidity(emergencyMonths, liquidityRatio, liquidityRatioGood)));

        // 2. DebtHealth (权重 25)
        int debtScore = scoreDebtHealth(dti, liabilities, assetLiabilityRatio, assetLiabilityRatioGood);
        healthBreakdown.add(dim("DebtHealth", debtScore, debtWeight, describeDebtHealth(dti, assetLiabilityRatio, assetLiabilityRatioGood)));
        healthBreakdownV1.add(breakdownItem(
                "DEBT_HEALTH", "DebtHealth", debtWeight,
                rawOrNa(dti), debtScore,
                describeDebtHealth(dti, assetLiabilityRatio, assetLiabilityRatioGood)));

        // 3. Diversification (权重 20)
        int divScore = scoreDiversification(allocation, concentration);
        healthBreakdown.add(dim("Diversification", divScore, divWeight, describeDiversification(concentration)));
        healthBreakdownV1.add(breakdownItem(
                "DIVERSIFICATION", "Diversification", divWeight,
                rawOrNa(concentration == null ? null : concentration.get("topRatio")), divScore,
                describeDiversification(concentration)));

        // 4. RiskAdjustedReturn (权重 15)
        int rarScore = scoreRiskAdjustedReturn(performanceResult, sharpeOk, sharpeGood, mddOk, mddBad);
        healthBreakdown.add(dim("RiskAdjustedReturn", rarScore, rarWeight, describeRAR(performanceResult)));
        healthBreakdownV1.add(breakdownItem(
                "RISK_ADJUSTED_RETURN", "RiskAdjustedReturn", rarWeight,
                rawOrNa(performanceResult == null ? null : performanceResult.get("sharpe")), rarScore,
                describeRAR(performanceResult)));

        // 5. Behavior (权重 15)
        int behaviorScore = scoreBehavior(behaviorStats, cashFlowRate, dti, emergencyMonths,
                cashFlowRateMin, debtRatioMax, emergencyMonthsMin);
        String behaviorReason = describeBehavior(behaviorStats, cashFlowRate, dti, emergencyMonths,
                cashFlowRateMin, debtRatioMax, emergencyMonthsMin);
        healthBreakdown.add(dim("Behavior", behaviorScore, behWeight, behaviorReason));
        healthBreakdownV1.add(breakdownItem(
                "BEHAVIOR", "Behavior", behWeight,
                rawOrNa(intValue(behaviorStats, "eventCount30d")), behaviorScore,
                behaviorReason));

        int healthScore = calcWeighted(healthBreakdown);
        result.put("healthScore", clamp(healthScore));

        // ========= Risk Score (规则权重) =========
        List<Map<String, Object>> riskBreakdown = new ArrayList<>();
        List<Map<String, Object>> riskBreakdownV1 = new ArrayList<>();

        Double sharpe = toDoubleObj(performanceResult == null ? null : performanceResult.get("sharpe"));
        Double maxDD = toDoubleObj(performanceResult == null ? null : performanceResult.get("maxDrawdown"));
        Double volatility = toDoubleObj(performanceResult == null ? null : performanceResult.get("volatility"));

        int sharpeRisk = scoreSharpeRisk(sharpe, sharpeOk, sharpeGood);
        int mddRisk = scoreMddRisk(maxDD, mddOk, mddBad);
        int volRisk = scoreVolRisk(volatility);
        int divRisk = scoreDiversificationRisk(concentration);

        double weightSum = wSharpe + wMdd + wVol + wDiv;
        if (weightSum <= 0) {
            wSharpe = 0.35;
            wMdd = 0.35;
            wVol = 0.15;
            wDiv = 0.15;
            weightSum = wSharpe + wMdd + wVol + wDiv;
        }

        int sharpeWeight = weightPercent(wSharpe, weightSum);
        int mddWeight = weightPercent(wMdd, weightSum);
        int volWeight = weightPercent(wVol, weightSum);
        int divRiskWeight = Math.max(0, 100 - sharpeWeight - mddWeight - volWeight);

        riskBreakdown.add(dim("Sharpe", sharpeRisk, sharpeWeight, describeSharpeRisk(sharpe, sharpeOk, sharpeGood)));
        riskBreakdown.add(dim("MaxDrawdown", mddRisk, mddWeight, describeMddRisk(maxDD, mddOk, mddBad)));
        riskBreakdown.add(dim("Volatility", volRisk, volWeight, describeVolRisk(volatility)));
        riskBreakdown.add(dim("Diversification", divRisk, divRiskWeight, describeDiversification(concentration)));
        riskBreakdownV1.add(breakdownItem(
                "SHARPE", "Sharpe", sharpeWeight,
                rawOrNa(sharpe), sharpeRisk,
                describeSharpeRisk(sharpe, sharpeOk, sharpeGood)));
        riskBreakdownV1.add(breakdownItem(
                "MAX_DRAWDOWN", "MaxDrawdown", mddWeight,
                rawOrNa(maxDD), mddRisk,
                describeMddRisk(maxDD, mddOk, mddBad)));
        riskBreakdownV1.add(breakdownItem(
                "VOLATILITY", "Volatility", volWeight,
                rawOrNa(volatility), volRisk,
                describeVolRisk(volatility)));
        riskBreakdownV1.add(breakdownItem(
                "DIVERSIFICATION", "Diversification", divRiskWeight,
                rawOrNa(concentration == null ? null : concentration.get("topRatio")), divRisk,
                describeDiversification(concentration)));

        int riskScore = (int) Math.round(
                (sharpeRisk * wSharpe + mddRisk * wMdd + volRisk * wVol + divRisk * wDiv) / weightSum);
        result.put("riskScore", clamp(riskScore));

        // ========= Behavior Score (M5-A 规则版) =========
        List<Map<String, Object>> behaviorBreakdown = new ArrayList<>();
        List<Map<String, Object>> behaviorBreakdownV1 = new ArrayList<>();
        int finalBehaviorScore = clamp(behaviorScore);
        behaviorBreakdown.add(dim("BehaviorRule30d", finalBehaviorScore, 100, behaviorReason));
        behaviorBreakdownV1.add(breakdownItem(
                "BEHAVIOR_RULE_30D", "BehaviorRule30d", 100,
                rawOrNa(intValue(behaviorStats, "eventCount30d")), finalBehaviorScore,
                behaviorReason));
        result.put("behaviorScore", finalBehaviorScore);

        // ========= Breakdown =========
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("health", healthBreakdown);
        breakdown.put("risk", riskBreakdown);
        breakdown.put("behavior", behaviorBreakdown);
        result.put("breakdown", breakdown);

        List<String> riskWarnings = new ArrayList<>(baseWarnings);
        if (sharpe == null) addWarning(riskWarnings, ScoreWarningCodes.SCORE_MISSING_SHARPE);
        if (maxDD == null) addWarning(riskWarnings, ScoreWarningCodes.SCORE_MISSING_MDD);
        if (volatility == null) addWarning(riskWarnings, ScoreWarningCodes.SCORE_MISSING_VOLATILITY);
        if (allocation == null || allocation.isEmpty()) addWarning(riskWarnings, ScoreWarningCodes.SCORE_MISSING_ALLOCATION);

        List<String> healthWarnings = new ArrayList<>(baseWarnings);
        if (cashflow == null) addWarning(healthWarnings, ScoreWarningCodes.SCORE_MISSING_CASHFLOW);
        if (liabilities == null) addWarning(healthWarnings, ScoreWarningCodes.SCORE_MISSING_LIABILITIES);
        if (allocation == null || allocation.isEmpty()) addWarning(healthWarnings, ScoreWarningCodes.SCORE_MISSING_ALLOCATION);
        if (totalAssets == null || totalAssets.compareTo(BigDecimal.ZERO) <= 0) {
            addWarning(healthWarnings, ScoreWarningCodes.SCORE_MISSING_TOTAL_ASSETS);
        }
        if (sharpe == null) addWarning(healthWarnings, ScoreWarningCodes.SCORE_MISSING_SHARPE);
        if (maxDD == null) addWarning(healthWarnings, ScoreWarningCodes.SCORE_MISSING_MDD);

        List<String> behaviorWarnings = new ArrayList<>(baseWarnings);
        int eventCount = intValue(behaviorStats, "eventCount30d");
        if (behaviorStats == null || behaviorStats.isEmpty() || eventCount <= 0) {
            addWarning(behaviorWarnings, ScoreWarningCodes.SCORE_BEHAVIOR_DATA_UNAVAILABLE);
        }

        String riskLevel = levelForScore(clamp(riskScore), riskMedMin, riskHighMin);
        String healthLevel = levelForScore(clamp(healthScore), healthMedMin, healthHighMin);
        String behaviorLevel = levelForScore(finalBehaviorScore, behaviorMedMin, behaviorHighMin);

        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("riskScore", scorePayload(clamp(riskScore), riskLevel, riskBreakdownV1, riskWarnings));
        scores.put("assetHealthScore", scorePayload(clamp(healthScore), healthLevel, healthBreakdownV1, healthWarnings));
        scores.put("behaviorScore", scorePayload(finalBehaviorScore, behaviorLevel, behaviorBreakdownV1, behaviorWarnings));
        result.put("scores", scores);
        result.put("scoreWarnings", mergeWarnings(riskWarnings, healthWarnings, behaviorWarnings));
        result.put("ruleSet", snapshot.toDebugMap());

        return result;
    }

    // ============================= Health 维度 =============================

    private int scoreLiquidity(BigDecimal emergencyMonths, BigDecimal liquidityRatio, double liquidityRatioGood) {
        int score = 30;
        if (emergencyMonths != null) {
            double m = emergencyMonths.doubleValue();
            if (m >= 6) score = 95;
            else if (m >= 3) score = 75;
            else if (m >= 1) score = 50;
            else score = 20;
        }
        if (liquidityRatio != null) {
            if (liquidityRatio.doubleValue() >= liquidityRatioGood) {
                score += 5;
            } else if (liquidityRatio.doubleValue() < liquidityRatioGood / 2) {
                score -= 5;
            }
        }
        return Math.max(10, Math.min(100, score));
    }

    private String describeLiquidity(BigDecimal emergencyMonths, BigDecimal liquidityRatio, double liquidityRatioGood) {
        String base;
        if (emergencyMonths == null) {
            base = "未录入现金流，无法评估应急金";
        } else {
            double m = emergencyMonths.doubleValue();
            if (m >= 6) base = "应急金充裕，覆盖" + emergencyMonths + "个月";
            else if (m >= 3) base = "应急金基本达标（" + emergencyMonths + "个月），建议逐步补至6个月";
            else base = "应急金不足（仅" + emergencyMonths + "个月），需优先补充";
        }
        if (liquidityRatio != null) {
            String ratio = pct(liquidityRatio.doubleValue());
            return base + "，流动性占比=" + ratio + " (目标>=" + pct(liquidityRatioGood) + ")";
        }
        return base;
    }

    private int scoreDebtHealth(BigDecimal dti,
                                List<FcLiabilityEntity> liabilities,
                                BigDecimal assetLiabilityRatio,
                                double assetLiabilityRatioGood) {
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
        if (assetLiabilityRatio != null) {
            double ratio = assetLiabilityRatio.doubleValue();
            if (ratio >= assetLiabilityRatioGood) score += 5;
            else if (ratio < assetLiabilityRatioGood / 2) score -= 5;
        }
        return Math.max(10, score);
    }

    private String describeDebtHealth(BigDecimal dti, BigDecimal assetLiabilityRatio, double assetLiabilityRatioGood) {
        if (dti == null) return "未录入负债或现金流数据";
        double d = dti.doubleValue();
        String pct = dti.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%";
        String base;
        if (d > 0.5) base = "DTI=" + pct + "，超过50%严重警戒线";
        else if (d > 0.4) base = "DTI=" + pct + "，超过40%警戒线";
        else if (d > 0.3) base = "DTI=" + pct + "，接近警戒区间";
        else base = "DTI=" + pct + "，债务负担健康";
        if (assetLiabilityRatio != null) {
            return base + "，资产负债比=" + assetLiabilityRatio.setScale(2, RoundingMode.HALF_UP)
                    + " (目标>=" + assetLiabilityRatioGood + ")";
        }
        return base;
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

    private int scoreRiskAdjustedReturn(Map<String, Object> perf,
                                        double sharpeOk,
                                        double sharpeGood,
                                        double mddOk,
                                        double mddBad) {
        if (perf == null) return 50;
        Double sharpe = toDoubleObj(perf.get("sharpe"));
        Double maxDD = toDoubleObj(perf.get("maxDrawdown"));
        int score = 50;
        if (sharpe != null) {
            if (sharpe >= sharpeGood) score += 25;
            else if (sharpe >= sharpeOk) score += 15;
            else if (sharpe > 0) score += 5;
            else score -= 15;
        }
        if (maxDD != null) {
            if (maxDD <= mddOk / 2) score += 15;
            else if (maxDD <= mddOk) score += 5;
            else if (maxDD >= mddBad) score -= 15;
            else if (maxDD >= (mddOk + mddBad) / 2) score -= 5;
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

    private int scoreSharpeRisk(Double sharpe, double sharpeOk, double sharpeGood) {
        if (sharpe == null) return 50;
        if (sharpe >= sharpeGood) return 10;
        if (sharpe >= sharpeOk) return 30;
        if (sharpe >= 0) return 50;
        return 80;
    }

    private String describeSharpeRisk(Double sharpe, double sharpeOk, double sharpeGood) {
        if (sharpe == null) return "Sharpe 缺失，采用中性风险";
        if (sharpe >= sharpeGood) return "Sharpe=" + sharpe + "，表现优秀";
        if (sharpe >= sharpeOk) return "Sharpe=" + sharpe + "，表现达标";
        if (sharpe >= 0) return "Sharpe=" + sharpe + "，表现一般";
        return "Sharpe=" + sharpe + "，表现较弱";
    }

    private int scoreMddRisk(Double maxDD, double mddOk, double mddBad) {
        if (maxDD == null) return 50;
        if (maxDD <= mddOk) return 20;
        if (maxDD <= mddBad) return 50;
        return 80;
    }

    private String describeMddRisk(Double maxDD, double mddOk, double mddBad) {
        if (maxDD == null) return "MaxDD 缺失，采用中性风险";
        if (maxDD <= mddOk) return "MaxDD=" + pct(maxDD) + "，回撤可控";
        if (maxDD <= mddBad) return "MaxDD=" + pct(maxDD) + "，回撤偏高";
        return "MaxDD=" + pct(maxDD) + "，回撤过高";
    }

    private int scoreVolRisk(Double volatility) {
        if (volatility == null) return 50;
        if (volatility <= 0.15) return 20;
        if (volatility <= 0.30) return 55;
        return 80;
    }

    private String describeVolRisk(Double volatility) {
        if (volatility == null) return "Volatility 缺失，采用中性风险";
        return "Volatility=" + pct(volatility);
    }

    private int scoreDiversificationRisk(Map<String, Object> concentration) {
        if (concentration == null || concentration.isEmpty()) return 50;
        double topRatio = toDouble(concentration.get("topRatio"));
        if (topRatio > 0.7) return 80;
        if (topRatio > 0.5) return 60;
        if (topRatio > 0.3) return 40;
        return 20;
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

    private int scoreBehavior(Map<String, Object> stats,
                              Double cashFlowRate,
                              BigDecimal dti,
                              BigDecimal emergencyMonths,
                              double cashFlowRateMin,
                              double debtRatioMax,
                              int emergencyMonthsMin) {
        int eventCount = intValue(stats, "eventCount30d");
        if (eventCount < 3) {
            return adjustBehaviorScore(60, cashFlowRate, dti, emergencyMonths,
                    cashFlowRateMin, debtRatioMax, emergencyMonthsMin);
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
        return adjustBehaviorScore(score, cashFlowRate, dti, emergencyMonths,
                cashFlowRateMin, debtRatioMax, emergencyMonthsMin);
    }

    private String describeBehavior(Map<String, Object> stats,
                                    Double cashFlowRate,
                                    BigDecimal dti,
                                    BigDecimal emergencyMonths,
                                    double cashFlowRateMin,
                                    double debtRatioMax,
                                    int emergencyMonthsMin) {
        int eventCount = intValue(stats, "eventCount30d");
        int reportGenerateCount = intValue(stats, "reportGenerateCount30d");
        int rebalanceConfirmCount = intValue(stats, "rebalanceConfirmCount30d");
        if (eventCount < 3) {
            return appendBehaviorRuleNotes(
                    "近30天行为事件不足3条（当前" + eventCount + "条），采用基线60分",
                    cashFlowRate, dti, emergencyMonths, cashFlowRateMin, debtRatioMax, emergencyMonthsMin);
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
        String base = String.join("；", reasons);
        return appendBehaviorRuleNotes(base, cashFlowRate, dti, emergencyMonths,
                cashFlowRateMin, debtRatioMax, emergencyMonthsMin);
    }

    private int adjustBehaviorScore(int base,
                                    Double cashFlowRate,
                                    BigDecimal dti,
                                    BigDecimal emergencyMonths,
                                    double cashFlowRateMin,
                                    double debtRatioMax,
                                    int emergencyMonthsMin) {
        int score = base;
        if (cashFlowRate != null && cashFlowRate < cashFlowRateMin) {
            score -= 10;
        }
        if (dti != null && dti.doubleValue() > debtRatioMax) {
            score -= 10;
        }
        if (emergencyMonths != null && emergencyMonths.doubleValue() < emergencyMonthsMin) {
            score -= 10;
        }
        return clamp(score);
    }

    private String appendBehaviorRuleNotes(String base,
                                           Double cashFlowRate,
                                           BigDecimal dti,
                                           BigDecimal emergencyMonths,
                                           double cashFlowRateMin,
                                           double debtRatioMax,
                                           int emergencyMonthsMin) {
        List<String> notes = new ArrayList<>();
        if (cashFlowRate != null && cashFlowRate < cashFlowRateMin) {
            notes.add("现金流率低于" + pct(cashFlowRateMin));
        }
        if (dti != null && dti.doubleValue() > debtRatioMax) {
            notes.add("DTI高于" + pct(debtRatioMax));
        }
        if (emergencyMonths != null && emergencyMonths.doubleValue() < emergencyMonthsMin) {
            notes.add("应急金不足" + emergencyMonthsMin + "个月");
        }
        if (notes.isEmpty()) return base;
        return base + "；" + String.join("，", notes);
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

    private BigDecimal sumCashAssets(List<FcAssetEntity> assets) {
        if (assets == null || assets.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (FcAssetEntity asset : assets) {
            if (asset != null && "CASH".equals(asset.getType()) && asset.getAmount() != null) {
                sum = sum.add(asset.getAmount());
            }
        }
        return sum;
    }

    private BigDecimal calcRatio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null) return null;
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) return null;
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private Double calcCashFlowRate(FcCashflowEntity cashflow) {
        if (cashflow == null || cashflow.getIncome() == null) return null;
        BigDecimal income = cashflow.getIncome();
        if (income.compareTo(BigDecimal.ZERO) <= 0) return null;
        BigDecimal essential = BigDecimal.ZERO;
        if (cashflow.getFixedExpense() != null) essential = essential.add(cashflow.getFixedExpense());
        if (cashflow.getVariableExpense() != null) essential = essential.add(cashflow.getVariableExpense());
        if (cashflow.getMonthlyDebtPayment() != null) essential = essential.add(cashflow.getMonthlyDebtPayment());
        BigDecimal surplus = income.subtract(essential);
        return surplus.divide(income, 4, RoundingMode.HALF_UP).doubleValue();
    }

    private int weightPercent(double weight, double total) {
        if (total <= 0) return 0;
        return (int) Math.round((weight / total) * 100.0);
    }

    private ScoreRuleSnapshot defaultSnapshot() {
        Map<String, com.fincoach.core.healthv2.rules.ScoreRuleParamValue> params = new LinkedHashMap<>();
        for (com.fincoach.core.healthv2.rules.ScoreRuleParamDefinition def : ScoreRuleDefaults.defaultParams().values()) {
            params.put(def.getKey(), com.fincoach.core.healthv2.rules.ScoreRuleParamValue.fromDefinition(def, "DEFAULT"));
        }
        return new ScoreRuleSnapshot(null,
                ScoreRuleDefaults.DEFAULT_CODE,
                ScoreRuleDefaults.DEFAULT_VERSION,
                ScoreRuleSnapshot.SOURCE_FALLBACK_DEFAULT,
                params,
                new ArrayList<>(),
                new ArrayList<>());
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

    private Map<String, Object> breakdownItem(String code,
                                              String name,
                                              int weight,
                                              Object rawValue,
                                              int score,
                                              String detail) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("name", name);
        d.put("weight", weight);
        d.put("rawValue", rawValue);
        d.put("scoreContribution", contribution(score, weight));
        d.put("detail", detail);
        return d;
    }

    private Map<String, Object> scorePayload(int value,
                                             String level,
                                             List<Map<String, Object>> breakdown,
                                             List<String> warnings) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("value", clamp(value));
        payload.put("level", level);
        payload.put("breakdown", breakdown == null ? new ArrayList<>() : breakdown);
        payload.put("warnings", warnings == null ? new ArrayList<>() : warnings);
        return payload;
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

    private int contribution(int score, int weight) {
        return (int) Math.round(clamp(score) * (weight / 100.0));
    }

    private Object rawOrNa(Object value) {
        return value == null ? "N/A" : value;
    }

    private void addWarning(List<String> warnings, String code) {
        if (warnings == null || code == null) return;
        if (!warnings.contains(code)) {
            warnings.add(code);
        }
    }

    private List<String> mergeWarnings(List<String>... lists) {
        List<String> merged = new ArrayList<>();
        if (lists == null) return merged;
        for (List<String> list : lists) {
            if (list == null) continue;
            for (String w : list) {
                if (!merged.contains(w)) merged.add(w);
            }
        }
        return merged;
    }

    private String levelForScore(int score, int medMin, int highMin) {
        int high = Math.max(0, Math.min(100, highMin));
        int med = Math.max(0, Math.min(100, medMin));
        if (med >= high) {
            med = Math.max(0, high - 1);
        }
        if (score >= high) return "HIGH";
        if (score >= med) return "MED";
        return "LOW";
    }

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
