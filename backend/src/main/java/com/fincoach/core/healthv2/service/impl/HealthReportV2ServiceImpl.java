package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.analyzer.PortfolioPerformanceAnalyzer;
import com.fincoach.core.healthv2.analyzer.RebalanceAdvisor;
import com.fincoach.core.healthv2.analyzer.ScoreEngine;
import com.fincoach.core.healthv2.analyzer.DebtOptimizer;
import com.fincoach.core.healthv2.analyzer.CashflowPlanner;
import com.fincoach.core.healthv2.analyzer.GoalPlanner;
import com.fincoach.core.healthv2.analyzer.InsuranceGapAnalyzer;
import com.fincoach.core.healthv2.dto.HealthReportV2VO;
import com.fincoach.core.healthv2.entity.*;
import com.fincoach.core.healthv2.mapper.*;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.HealthReportV2Service;
import com.fincoach.core.healthv2.util.ConfigJsonHelper;
import com.fincoach.core.healthv2.util.HealthV2ConfigDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 体检报告生成服务实现（M1+M2）
 *
 * M1: 基础指标、简单规则评分、建议骨架
 * M2: Portfolio Performance (sharpe/maxDrawdown/corrMatrix) + Rebalance Advice
 */
@Slf4j
@Service
public class HealthReportV2ServiceImpl implements HealthReportV2Service {

    @Autowired
    private FcAssetMapper assetMapper;
    @Autowired
    private FcLiabilityMapper liabilityMapper;
    @Autowired
    private FcCashflowMapper cashflowMapper;
    @Autowired
    private FcGoalMapper goalMapper;
    @Autowired
    private FcInsuranceProfileMapper insuranceMapper;
    @Autowired
    private FcHealthReportMapper reportMapper;
    @Autowired
    private AuditService auditService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PortfolioPerformanceAnalyzer performanceAnalyzer;
    @Autowired
    private RebalanceAdvisor rebalanceAdvisor;
    @Autowired
    private ScoreEngine scoreEngine;
    @Autowired
    private DebtOptimizer debtOptimizer;
    @Autowired
    private CashflowPlanner cashflowPlanner;
    @Autowired
    private GoalPlanner goalPlanner;
    @Autowired
    private InsuranceGapAnalyzer insuranceGapAnalyzer;
    @Autowired
    private FcStrategyFlagMapper strategyFlagMapper;
    @Autowired
    private FcAlertRuleMapper alertRuleMapper;
    @Autowired
    private FcAlertRecordMapper alertRecordMapper;
    @Autowired
    private FcAdviceTemplateMapper adviceTemplateMapper;
    @Autowired
    private FcScoreRuleVersionMapper scoreRuleVersionMapper;
    @Autowired
    private ConfigJsonHelper configJsonHelper;
    @Autowired
    private com.fincoach.core.healthv2.service.BehaviorEventService behaviorEventService;
    @Autowired
    private com.fincoach.core.healthv2.service.NotificationService notificationService;

    @Override
    public HealthReportV2VO generate(Long userId) {
        log.info("[HealthV2-Report] 开始生成体检报告, userId={}", userId);

        // ========= 1. 拉取用户最新数据 =========
        List<FcAssetEntity> assets = assetMapper.selectList(
                new LambdaQueryWrapper<FcAssetEntity>().eq(FcAssetEntity::getUserId, userId));

        List<FcLiabilityEntity> liabilities = liabilityMapper.selectList(
                new LambdaQueryWrapper<FcLiabilityEntity>().eq(FcLiabilityEntity::getUserId, userId));

        // 取最近月份的现金流
        FcCashflowEntity cashflow = cashflowMapper.selectOne(
                new LambdaQueryWrapper<FcCashflowEntity>()
                        .eq(FcCashflowEntity::getUserId, userId)
                        .orderByDesc(FcCashflowEntity::getMonth)
                        .last("LIMIT 1"));

        List<FcGoalEntity> goals = goalMapper.selectList(
                new LambdaQueryWrapper<FcGoalEntity>().eq(FcGoalEntity::getUserId, userId));

        FcInsuranceProfileEntity insurance = insuranceMapper.selectOne(
                new LambdaQueryWrapper<FcInsuranceProfileEntity>().eq(FcInsuranceProfileEntity::getUserId, userId));

        // ========= 2. 计算基础指标（分模块） =========

        // --- 2a. 基础数据提取 ---
        BigDecimal cashAssets = assets.stream()
                .filter(a -> "CASH".equals(a.getType()))
                .map(FcAssetEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAssets = assets.stream()
                .map(FcAssetEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebt = liabilities.stream()
                .map(FcLiabilityEntity::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netWorth = totalAssets.subtract(totalDebt);

        // --- 2b. 现金流模块 ---
        Map<String, Object> cashflowMetrics = new LinkedHashMap<>();
        BigDecimal dti = null;
        BigDecimal essentialExpense = BigDecimal.ZERO;
        BigDecimal monthlySurplus = null;
        BigDecimal emergencyMonths = null;

        if (cashflow != null) {
            if (cashflow.getFixedExpense() != null) essentialExpense = essentialExpense.add(cashflow.getFixedExpense());
            if (cashflow.getVariableExpense() != null) essentialExpense = essentialExpense.add(cashflow.getVariableExpense());
            if (cashflow.getMonthlyDebtPayment() != null) essentialExpense = essentialExpense.add(cashflow.getMonthlyDebtPayment());

            if (cashflow.getIncome() != null && cashflow.getIncome().compareTo(BigDecimal.ZERO) > 0) {
                if (cashflow.getMonthlyDebtPayment() != null) {
                    dti = cashflow.getMonthlyDebtPayment()
                            .divide(cashflow.getIncome(), 4, RoundingMode.HALF_UP);
                }
                monthlySurplus = cashflow.getIncome().subtract(essentialExpense);
            }

            if (essentialExpense.compareTo(BigDecimal.ZERO) > 0) {
                emergencyMonths = cashAssets.divide(essentialExpense, 2, RoundingMode.HALF_UP);
            }
        }
        cashflowMetrics.put("dti", dti);
        cashflowMetrics.put("essentialExpense", essentialExpense);
        cashflowMetrics.put("monthlySurplus", monthlySurplus);
        cashflowMetrics.put("emergencyMonths", emergencyMonths);

        // --- 2c. 资产组合模块 ---
        Map<String, Object> portfolioMetrics = new LinkedHashMap<>();
        portfolioMetrics.put("cashAssets", cashAssets);
        portfolioMetrics.put("totalAssets", totalAssets);
        portfolioMetrics.put("netWorth", netWorth);

        // 资产类别占比（M1 简版）
        Map<String, Object> allocation = new LinkedHashMap<>();
        if (totalAssets.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, BigDecimal> typeSum = new LinkedHashMap<>();
            for (FcAssetEntity a : assets) {
                String type = a.getType() != null ? a.getType() : "OTHER";
                typeSum.merge(type, a.getAmount(), BigDecimal::add);
            }
            for (Map.Entry<String, BigDecimal> e : typeSum.entrySet()) {
                allocation.put(e.getKey(), e.getValue().divide(totalAssets, 4, RoundingMode.HALF_UP));
            }
        }
        portfolioMetrics.put("allocation", allocation);

        // 最大类别集中度
        Map<String, Object> concentration = new LinkedHashMap<>();
        if (!allocation.isEmpty()) {
            String maxType = null;
            BigDecimal maxRatio = BigDecimal.ZERO;
            for (Map.Entry<String, Object> e : allocation.entrySet()) {
                BigDecimal ratio = (BigDecimal) e.getValue();
                if (ratio.compareTo(maxRatio) > 0) {
                    maxRatio = ratio;
                    maxType = e.getKey();
                }
            }
            concentration.put("topType", maxType);
            concentration.put("topRatio", maxRatio);
        }
        portfolioMetrics.put("concentration", concentration);

        // M2: Portfolio Performance（双路径：HISTORY / PARAM）
        Map<String, Object> performance;
        try {
            performance = performanceAnalyzer.computePerformance(userId, allocation);
        } catch (Exception e) {
            log.error("[HealthV2-Report] Performance 计算异常, fallback to null", e);
            performance = new LinkedHashMap<>();
            performance.put("sharpe", null);
            performance.put("maxDrawdown", null);
            performance.put("corrMatrix", null);
            performance.put("method", "ERROR");
            performance.put("reason", e.getMessage());
        }
        portfolioMetrics.put("performance", performance);

        // --- 2d. 负债模块 ---
        Map<String, Object> debtMetrics = new LinkedHashMap<>();
        debtMetrics.put("totalDebt", totalDebt);
        debtMetrics.put("debtCount", liabilities.size());
        debtMetrics.put("monthlyDebtPayment",
                cashflow != null ? cashflow.getMonthlyDebtPayment() : null);

        // 加权平均利率
        BigDecimal avgInterestRate = null;
        if (!liabilities.isEmpty() && totalDebt.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal weightedSum = BigDecimal.ZERO;
            for (FcLiabilityEntity l : liabilities) {
                if (l.getInterestRate() != null && l.getPrincipal() != null) {
                    weightedSum = weightedSum.add(l.getInterestRate().multiply(l.getPrincipal()));
                }
            }
            avgInterestRate = weightedSum.divide(totalDebt, 6, RoundingMode.HALF_UP);
        }
        debtMetrics.put("avgInterestRate", avgInterestRate);

        // --- 2e. 目标模块 ---
        Map<String, Object> goalsMetrics = new LinkedHashMap<>();
        goalsMetrics.put("goalCount", goals.size());

        // --- 2f. 保险模块 ---
        Map<String, Object> insuranceMetrics = new LinkedHashMap<>();
        insuranceMetrics.put("insuranceProfileComplete", insurance != null);

        // --- 组装 metrics ---
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("cashflow", cashflowMetrics);
        metrics.put("portfolio", portfolioMetrics);
        metrics.put("debt", debtMetrics);
        metrics.put("goals", goalsMetrics);
        metrics.put("insurance", insuranceMetrics);

        // ========= 3. M3 评分引擎（多维加权 + 可解释 breakdown） =========
        // M5: 准备行为数据
        Map<String, Object> behaviorStats = new HashMap<>();;
        try {
            // 简单统计近30天数据
            Map<String, Integer> counts = behaviorEventService.countByType(userId, 30);
            behaviorStats.putAll(counts);
            // 补充特殊统计
            int totalEvents = counts.values().stream().mapToInt(Integer::intValue).sum();
            behaviorStats.put("eventCount", totalEvents);
            behaviorStats.put("rebalanceConfirmCount", counts.getOrDefault("REBALANCE_CONFIRM", 0));
            behaviorStats.put("assetUpdateCount", counts.getOrDefault("ASSET_UPDATE", 0));
            // FIXME: 注册天数暂无，mock 100
            behaviorStats.put("daysSinceRegister", 100); 
        } catch (Exception e) {
            log.warn("[HealthV2-Report] 获取行为数据失败", e);
        }

        Map<String, Object> scoreResult;
        try {
            scoreResult = scoreEngine.compute(
                    assets, liabilities, cashflow, goals, insurance,
                    performance, allocation, concentration,
                    emergencyMonths, dti, totalAssets, totalDebt, behaviorStats);
        } catch (Exception e) {
            log.error("[HealthV2-Report] ScoreEngine 计算异常, fallback", e);
            scoreResult = new LinkedHashMap<>();
            scoreResult.put("healthScore", 50);
            scoreResult.put("riskScore", 50);
            scoreResult.put("behaviorScore", 60);
            scoreResult.put("breakdown", Collections.emptyMap());
        }
        int healthScore = (Integer) scoreResult.get("healthScore");
        int riskScore = (Integer) scoreResult.get("riskScore");
        int behaviorScore = (Integer) scoreResult.get("behaviorScore");

        // scoreBreakdown 写入 metrics
        metrics.put("scoreBreakdown", scoreResult.get("breakdown"));

        // ========= 4. M4 策略开关 + 生成建议 =========
        Map<String, Boolean> flags = loadStrategyFlags();
        Map<String, Object> advice = new LinkedHashMap<>();

        // 4a. 现金流计划
        Map<String, Object> cashflowPlan = null;
        if (flags.getOrDefault("ENABLE_CASHFLOW_PLANNER", true)) {
            try {
                cashflowPlan = cashflowPlanner.plan(
                        cashflow, cashAssets, emergencyMonths, monthlySurplus,
                        essentialExpense, insurance, !liabilities.isEmpty());
            } catch (Exception e) {
                log.error("[HealthV2-Report] CashflowPlanner 异常", e);
            }
        }
        advice.put("cashflowPlan", cashflowPlan);

        // 4b. 债务优化
        Map<String, Object> debtPlan = null;
        if (flags.getOrDefault("ENABLE_DEBT_OPTIMIZER", true)) {
            try {
                debtPlan = debtOptimizer.optimize(liabilities, cashflow, emergencyMonths, monthlySurplus);
            } catch (Exception e) {
                log.error("[HealthV2-Report] DebtOptimizer 异常", e);
            }
        }
        advice.put("debtPlan", debtPlan);

        // 4c. 再平衡（M2 延续）
        Map<String, Object> rebalance = null;
        if (flags.getOrDefault("ENABLE_REBALANCE", true)) {
            try {
                rebalance = rebalanceAdvisor.advise(
                        allocation, riskScore, emergencyMonths, dti, monthlySurplus, totalAssets);
            } catch (Exception e) {
                log.error("[HealthV2-Report] Rebalance 计算异常", e);
            }
        }
        advice.put("rebalance", rebalance);

        // 4d. 目标账户计划
        List<Map<String, Object>> goalAdvice = Collections.emptyList();
        if (flags.getOrDefault("ENABLE_GOAL_PLANNER", true)) {
            try {
                goalAdvice = goalPlanner.plan(goals, monthlySurplus);
            } catch (Exception e) {
                log.error("[HealthV2-Report] GoalPlanner 异常", e);
            }
        }
        advice.put("goals", goalAdvice);

        // 4e. 保险缺口评估
        Map<String, Object> insuranceAdvice = null;
        if (flags.getOrDefault("ENABLE_INSURANCE_GAP", true)) {
            try {
                BigDecimal annualIncome = cashflow != null && cashflow.getIncome() != null
                        ? cashflow.getIncome().multiply(BigDecimal.valueOf(12)) : null;
                insuranceAdvice = insuranceGapAnalyzer.analyze(insurance, annualIncome, liabilities);
            } catch (Exception e) {
                log.error("[HealthV2-Report] InsuranceGapAnalyzer 异常", e);
            }
        }
        advice.put("insurance", insuranceAdvice);

        // 4f. Summary 提炼（Top 关键结论）
        List<String> summary = buildSummary(emergencyMonths, dti, goals, insurance, goalAdvice, debtPlan);
        advice.put("summary", summary);

        // ========= 5. 落库 =========
        FcHealthReportEntity entity = new FcHealthReportEntity();
        entity.setUserId(userId);
        entity.setReportDate(LocalDateTime.now());
        entity.setRiskScore(riskScore);
        entity.setHealthScore(healthScore);
        entity.setBehaviorScore(behaviorScore);
        entity.setRuleVersion("M4");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        try {
            entity.setMetricsJson(objectMapper.writeValueAsString(metrics));
            entity.setAdviceJson(objectMapper.writeValueAsString(advice));
        } catch (Exception e) {
            log.error("[HealthV2-Report] JSON 序列化失败", e);
            entity.setMetricsJson("{}");
            entity.setAdviceJson("{}");
        }

        reportMapper.insert(entity);

        // ========= 6. 审计日志 =========
        auditService.log(userId, "GENERATE_REPORT", "HEALTH_REPORT", entity.getId(), null, entity);

        // ========= 7. M4 触发预警 =========
        if (flags.getOrDefault("ENABLE_ALERTS", true)) {
            try {
                triggerAlerts(userId, emergencyMonths, dti, concentration);
            } catch (Exception e) {
                log.error("[HealthV2-Report] 触发预警异常", e);
            }
        }

        log.info("[HealthV2-Report] 报告生成完成, reportId={}, healthScore={}, riskScore={}, behaviorScore={}",
                entity.getId(), healthScore, riskScore, behaviorScore);

        return toVO(entity);
    }

    @Override
    public HealthReportV2VO getLatest(Long userId) {
        FcHealthReportEntity entity = reportMapper.selectOne(
                new LambdaQueryWrapper<FcHealthReportEntity>()
                        .eq(FcHealthReportEntity::getUserId, userId)
                        .orderByDesc(FcHealthReportEntity::getReportDate)
                        .last("LIMIT 1"));
        if (entity == null) {
            return null;
        }
        return toVO(entity);
    }

    @Override
    public HealthReportV2VO getById(Long reportId) {
        FcHealthReportEntity entity = reportMapper.selectById(reportId);
        if (entity == null) {
            return null;
        }
        return toVO(entity);
    }

    // ============================= 私有方法 =============================

    /**
     * 健康评分计算（M1 简单规则）
     * 满分 100，根据应急月数和 DTI 扣分
     */
    private int calculateHealthScore(BigDecimal emergencyMonths, BigDecimal dti, BigDecimal totalAssets) {
        int score = 70; // 基线

        if (totalAssets.compareTo(BigDecimal.ZERO) == 0) {
            return 30; // 无资产
        }

        // 应急月数评估
        if (emergencyMonths != null) {
            if (emergencyMonths.compareTo(new BigDecimal("6")) >= 0) {
                score += 15; // 6个月以上：优秀
            } else if (emergencyMonths.compareTo(new BigDecimal("3")) >= 0) {
                score += 10; // 3-6个月：良好
            } else if (emergencyMonths.compareTo(new BigDecimal("1")) >= 0) {
                score += 0;  // 1-3个月：一般
            } else {
                score -= 10; // 不足1个月：危险
            }
        }

        // DTI 评估
        if (dti != null) {
            if (dti.compareTo(new BigDecimal("0.2")) <= 0) {
                score += 15; // DTI <= 20%：优秀
            } else if (dti.compareTo(new BigDecimal("0.4")) <= 0) {
                score += 5;  // DTI 20-40%：可控
            } else if (dti.compareTo(new BigDecimal("0.6")) <= 0) {
                score -= 5;  // DTI 40-60%：偏高
            } else {
                score -= 15; // DTI > 60%：危险
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 风险评分计算（M1 简单规则）
     * 0=低风险, 100=高风险
     */
    private int calculateRiskScore(BigDecimal emergencyMonths, BigDecimal dti,
                                   BigDecimal totalDebt, BigDecimal totalAssets) {
        int score = 30; // 基线

        // DTI 越高，风险越大
        if (dti != null) {
            if (dti.compareTo(new BigDecimal("0.6")) > 0) {
                score += 30;
            } else if (dti.compareTo(new BigDecimal("0.4")) > 0) {
                score += 20;
            } else if (dti.compareTo(new BigDecimal("0.2")) > 0) {
                score += 10;
            }
        }

        // 应急金不足，风险升高
        if (emergencyMonths != null) {
            if (emergencyMonths.compareTo(new BigDecimal("1")) < 0) {
                score += 25;
            } else if (emergencyMonths.compareTo(new BigDecimal("3")) < 0) {
                score += 15;
            } else if (emergencyMonths.compareTo(new BigDecimal("6")) < 0) {
                score += 5;
            }
        }

        // 资不抵债
        if (totalAssets.compareTo(BigDecimal.ZERO) > 0 && totalDebt.compareTo(totalAssets) > 0) {
            score += 15;
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Entity → VO 转换
     */
    private HealthReportV2VO toVO(FcHealthReportEntity entity) {
        HealthReportV2VO vo = new HealthReportV2VO();
        vo.setReportId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setReportDate(entity.getReportDate());
        vo.setRiskScore(entity.getRiskScore());
        vo.setHealthScore(entity.getHealthScore());
        vo.setBehaviorScore(entity.getBehaviorScore());
        vo.setRuleVersion(entity.getRuleVersion());

        // 反序列化 JSON
        try {
            if (entity.getMetricsJson() != null) {
                vo.setMetrics(objectMapper.readValue(entity.getMetricsJson(),
                        new TypeReference<Map<String, Object>>() {}));
            }
            if (entity.getAdviceJson() != null) {
                vo.setAdvice(objectMapper.readValue(entity.getAdviceJson(),
                        new TypeReference<Map<String, Object>>() {}));
            }
        } catch (Exception e) {
            log.error("[HealthV2-Report] JSON 反序列化失败, reportId={}", entity.getId(), e);
            vo.setMetrics(Collections.emptyMap());
            vo.setAdvice(Collections.emptyMap());
        }

        return vo;
    }

    /**
     * 自动提炼 summary（Top 2-4 条关键结论）
     */
    @SuppressWarnings("unchecked")
    private List<String> buildSummary(BigDecimal emergencyMonths, BigDecimal dti,
                                       List<FcGoalEntity> goals, FcInsuranceProfileEntity insurance,
                                       List<Map<String, Object>> goalAdvice,
                                       Map<String, Object> debtPlan) {
        List<String> summary = new ArrayList<>();

        // 应急金
        if (emergencyMonths != null && emergencyMonths.compareTo(new BigDecimal("3")) < 0) {
            summary.add("⚠️ 优先建立应急金：现金储备仅" + emergencyMonths + "个月，建议至少积累3-6个月。");
        } else if (emergencyMonths != null) {
            summary.add("✅ 应急金健康：覆盖" + emergencyMonths + "个月支出。");
        } else {
            summary.add("💡 请录入现金流数据以评估应急储备。");
        }

        // DTI
        if (dti != null && dti.compareTo(new BigDecimal("0.4")) > 0) {
            summary.add("⚠️ 负债压力偏大：DTI=" + dti.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%，建议优化债务结构。");
        }

        // 目标风险
        if (goalAdvice != null) {
            long atRisk = goalAdvice.stream()
                    .filter(g -> "AT_RISK".equals(g.get("status")))
                    .count();
            if (atRisk > 0) {
                summary.add("⚠️ " + atRisk + "个财务目标存在风险，建议调整投入或期限。");
            }
        }
        if (goals != null && goals.isEmpty()) {
            summary.add("💡 建议设定至少一个财务目标。");
        }

        // 保险
        if (insurance == null) {
            summary.add("💡 建议完善保险档案，以便评估保障缺口。");
        }

        // 负债优化摘要
        if (debtPlan != null) {
            Object rec = debtPlan.get("recommendation");
            if (rec instanceof Map) {
                String mode = (String) ((Map<String, Object>) rec).get("mode");
                if ("CASHFLOW_PROTECT".equals(mode)) {
                    summary.add("⚠️ 现金流紧张模式：暂停额外还款，优先保障生活与应急。");
                } else if ("PAY_HIGH_INTEREST_FIRST".equals(mode)) {
                    summary.add("💰 存在高息负债，建议优先偿还以减少利息支出。");
                }
            }
        }

        return summary;
    }

    /**
     * M4: 加载策略开关
     */
    private Map<String, Boolean> loadStrategyFlags() {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        try {
            List<FcStrategyFlagEntity> list = strategyFlagMapper.selectList(null);
            for (FcStrategyFlagEntity f : list) {
                flags.put(f.getFlagKey(), f.getEnabled());
            }
        } catch (Exception e) {
            log.warn("[HealthV2-Report] 加载策略开关失败，使用全部默认开启", e);
        }
        return flags;
    }

    /**
     * M4: 根据预警规则触发预警记录
     */
    private void triggerAlerts(Long userId, BigDecimal emergencyMonths, BigDecimal dti, Map<String, Object> concentration) {
        BigDecimal concentrationRatio = null;
        if (concentration != null && concentration.get("topRatio") instanceof BigDecimal) {
            concentrationRatio = (BigDecimal) concentration.get("topRatio");
        }
        List<FcAlertRuleEntity> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<FcAlertRuleEntity>().eq(FcAlertRuleEntity::getEnabled, true));

        for (FcAlertRuleEntity rule : rules) {
            boolean triggered = false;
            String message = rule.getMessageTemplate() != null ? rule.getMessageTemplate() : rule.getRuleKey();

            try {
                Map<String, Object> thresholds = parseAlertThresholds(rule, userId);

                switch (rule.getRuleKey()) {
                    case "DTI_WARNING":
                    case "DTI_CRITICAL":
                        if (dti != null) {
                            Double threshold = toDoubleObj(thresholds.get("threshold"));
                            if (threshold != null) {
                                triggered = dti.doubleValue() > threshold;
                            }
                        }
                        break;
                    case "EMERGENCY_LOW":
                    case "EMERGENCY_WARN":
                        if (emergencyMonths != null) {
                            Double months = toDoubleObj(thresholds.get("months"));
                            if (months != null) {
                                triggered = emergencyMonths.doubleValue() < months;
                            }
                        }
                        break;
                    case "CONCENTRATION":
                        if (concentrationRatio != null) {
                            Double threshold = toDoubleObj(thresholds.get("threshold"));
                            if (threshold != null) {
                                triggered = concentrationRatio.doubleValue() > threshold;
                            }
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                log.warn("[HealthV2-Alert] 解析规则阈值失败: ruleKey={}", rule.getRuleKey(), e);
                continue;
            }

            if (triggered) {
                FcAlertRecordEntity record = new FcAlertRecordEntity();
                record.setUserId(userId);
                record.setRuleKey(rule.getRuleKey());
                record.setSeverity(rule.getSeverity());
                record.setMessage(message);
                record.setStatus("ACTIVE");
                record.setCreatedAt(LocalDateTime.now());

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("dti", dti);
                payload.put("emergencyMonths", emergencyMonths);
                payload.put("concentration", concentrationRatio);
                try {
                    record.setPayloadJson(objectMapper.writeValueAsString(payload));
                } catch (Exception e) {
                    record.setPayloadJson("{}");
                }

                alertRecordMapper.insert(record);
                log.info("[HealthV2-Alert] 触发预警: userId={}, ruleKey={}, severity={}", userId, rule.getRuleKey(), rule.getSeverity());

                // M5: 同步写入通知中心
                try {
                    String title = "⚠️ 健康预警: " + message;
                    // 尝试从 adviceTemplate 获取更友好的文案 (TODO: 暂复用message)
                    notificationService.create(userId, "ALERT", title, message, record.getPayloadJson());
                } catch (Exception e) {
                    log.error("[HealthV2-Alert] 写入通知失败", e);
                }
            }
        }
    }

    private Map<String, Object> parseAlertThresholds(FcAlertRuleEntity rule, Long actorUserId) {
        String fallbackJson = HealthV2ConfigDefaults.defaultAlertThresholds(rule.getRuleKey());
        String safeJson = configJsonHelper.parseOrDefault(
                rule.getThresholdsJson(),
                fallbackJson,
                actorUserId,
                "ALERT_RULE",
                rule.getId(),
                "thresholdsJson");
        if (safeJson == null || safeJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(safeJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[HealthV2-Alert] fallback JSON 解析失败: ruleKey={}", rule.getRuleKey(), e);
            return Collections.emptyMap();
        }
    }

    private Double toDoubleObj(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
