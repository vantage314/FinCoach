package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleDefaults;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleSnapshot;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleRegistry;
import com.fincoach.core.healthv2.dto.AdviceActionSuggestionDTO;
import com.fincoach.core.healthv2.dto.AdviceRebalanceResponseDTO;
import com.fincoach.core.healthv2.dto.AdviceRebalanceSuggestionDTO;
import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.entity.FcCashflowMonthEntity;
import com.fincoach.core.healthv2.entity.FcDebtEntity;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.mapper.FcAssetMapper;
import com.fincoach.core.healthv2.mapper.FcCashflowMonthMapper;
import com.fincoach.core.healthv2.mapper.FcDebtMapper;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateDefaults;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateRegistry;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateSnapshot;
import com.fincoach.core.healthv2.service.AdviceRebalanceService;
import com.fincoach.core.healthv2.service.BehaviorEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AdviceRebalanceServiceImpl implements AdviceRebalanceService {

    private final FcAssetMapper assetMapper;
    private final FcHealthReportMapper healthReportMapper;
    private final BehaviorEventService behaviorEventService;
    private final AdviceRuleRegistry adviceRuleRegistry;
    private final RebalanceTemplateRegistry rebalanceTemplateRegistry;
    private final ObjectMapper objectMapper;
    private final FcDebtMapper debtMapper;
    private final FcCashflowMonthMapper cashflowMonthMapper;

    public AdviceRebalanceServiceImpl(FcAssetMapper assetMapper,
                                      FcHealthReportMapper healthReportMapper,
                                      BehaviorEventService behaviorEventService,
                                      AdviceRuleRegistry adviceRuleRegistry,
                                      RebalanceTemplateRegistry rebalanceTemplateRegistry,
                                      ObjectMapper objectMapper,
                                      FcDebtMapper debtMapper,
                                      FcCashflowMonthMapper cashflowMonthMapper) {
        this.assetMapper = assetMapper;
        this.healthReportMapper = healthReportMapper;
        this.behaviorEventService = behaviorEventService;
        this.adviceRuleRegistry = adviceRuleRegistry;
        this.rebalanceTemplateRegistry = rebalanceTemplateRegistry;
        this.objectMapper = objectMapper;
        this.debtMapper = debtMapper;
        this.cashflowMonthMapper = cashflowMonthMapper;
    }

    @Override
    public AdviceRebalanceResponseDTO buildRebalanceAdvice(Long userId) {
        AdviceRebalanceResponseDTO dto = new AdviceRebalanceResponseDTO();
        if (userId == null) {
            dto.setRebalanceSuggestions(new ArrayList<>());
            dto.setDebtSuggestions(new ArrayList<>());
            dto.setCashflowSuggestions(new ArrayList<>());
            dto.setMeta(new LinkedHashMap<>());
            return dto;
        }

        List<String> warnings = new ArrayList<>();
        AdviceRuleSnapshot ruleSnapshot = adviceRuleRegistry == null ? null : adviceRuleRegistry.get();
        if (ruleSnapshot != null && ruleSnapshot.getWarnings() != null) {
            warnings.addAll(ruleSnapshot.getWarnings());
        }
        double driftThreshold = readDecimal(ruleSnapshot, AdviceRuleDefaults.REBALANCE_DRIFT_PCT, 0.05);
        int maxPositions = readInt(ruleSnapshot, AdviceRuleDefaults.MAX_POSITIONS, 8);
        int emergencyTargetMonths = readInt(ruleSnapshot, AdviceRuleDefaults.EMERGENCY_FUND_MONTHS_TARGET, 3, warnings);
        double dtiWarn = readDecimal(ruleSnapshot, AdviceRuleDefaults.DTI_WARN, 0.35, warnings);
        double dtiDanger = readDecimal(ruleSnapshot, AdviceRuleDefaults.DTI_DANGER, 0.50, warnings);
        double minNetForExtraPayment = readDecimal(ruleSnapshot, AdviceRuleDefaults.MIN_NET_FOR_EXTRA_DEBT_PAYMENT, 0.0, warnings);
        String debtStrategy = readString(ruleSnapshot, AdviceRuleDefaults.DEBT_STRATEGY, "AVALANCHE", warnings);
        if (debtStrategy == null || debtStrategy.isBlank()) {
            debtStrategy = "AVALANCHE";
        }
        if (!"AVALANCHE".equalsIgnoreCase(debtStrategy)) {
            addWarning(warnings, "DEBT_STRATEGY_UNSUPPORTED");
            debtStrategy = "AVALANCHE";
        }

        Map<String, Double> weights = parseWeights(ruleSnapshot);
        double wTrade = weights.getOrDefault("tradeFreq", 0.4);
        double wConcentration = weights.getOrDefault("concentration", 0.3);
        double wCash = weights.getOrDefault("cashDrag", 0.3);
        double wSum = wTrade + wConcentration + wCash;
        if (wSum <= 0) {
            wTrade = 0.4;
            wConcentration = 0.3;
            wCash = 0.3;
            wSum = 1.0;
        }

        List<FcAssetEntity> assets = assetMapper.selectList(
                new LambdaQueryWrapper<FcAssetEntity>().eq(FcAssetEntity::getUserId, userId));
        BigDecimal totalAssets = sumAssets(assets);
        if (totalAssets == null || totalAssets.compareTo(BigDecimal.ZERO) <= 0) {
            warnings.add("TOTAL_ASSETS_MISSING");
        }
        Map<String, Double> allocation = buildAllocation(assets, totalAssets);
        double cashRatio = allocation.getOrDefault("CASH", 0.0);
        double topRatio = maxRatio(allocation);
        int positionsCount = allocation.size();

        Map<String, Integer> counts = behaviorEventService == null ? Collections.emptyMap() : behaviorEventService.countByType(userId, 30);
        int tradeCount = counts.getOrDefault("REBALANCE_CONFIRM", 0);

        double tradePenalty = Math.min(40, tradeCount * 4.0);
        double concentrationPenalty = Math.max(0, (topRatio - 0.5) * 100.0);
        double cashDragPenalty = Math.max(0, (cashRatio - 0.2) * 100.0);
        double positionsPenalty = Math.max(0, positionsCount - maxPositions) * 2.0;

        double weightedPenalty = tradePenalty * (wTrade / wSum)
                + (concentrationPenalty + positionsPenalty) * (wConcentration / wSum)
                + cashDragPenalty * (wCash / wSum);
        int behaviorScore = clampScore(Math.round(80 - weightedPenalty));
        dto.setBehaviorScore(behaviorScore);

        FcHealthReportEntity latest = healthReportMapper.selectOne(
                new LambdaQueryWrapper<FcHealthReportEntity>()
                        .eq(FcHealthReportEntity::getUserId, userId)
                        .orderByDesc(FcHealthReportEntity::getReportDate)
                        .last("LIMIT 1"));
        int riskScore = latest == null || latest.getRiskScore() == null ? 50 : latest.getRiskScore();
        int healthScore = latest == null || latest.getHealthScore() == null ? 60 : latest.getHealthScore();
        dto.setRiskScore(riskScore);
        dto.setHealthScore(healthScore);
        dto.setRiskBand(resolveRiskBand(ruleSnapshot, riskScore));

        Map<String, Double> targets = resolveTargets();
        List<AdviceRebalanceSuggestionDTO> suggestions = buildSuggestions(allocation, targets, driftThreshold, totalAssets);
        dto.setRebalanceSuggestions(suggestions == null ? new ArrayList<>() : suggestions);

        List<FcDebtEntity> debts = loadActiveDebts(userId);
        CashflowSummary cashflowSummary = summarizeCashflow(loadRecentCashflow(userId, 3));
        double avgIncome = toDouble(cashflowSummary.avgIncome(), 0.0);
        double avgExpense = toDouble(cashflowSummary.avgExpense(), 0.0);
        double avgNet = toDouble(cashflowSummary.avgNet(), 0.0);
        if (cashflowSummary.monthsCount() < 2) {
            addWarning(warnings, "CASHFLOW_ADVICE_INSUFFICIENT_DATA");
        }

        BigDecimal totalMonthlyPayment = BigDecimal.ZERO;
        boolean missingApr = false;
        if (debts != null) {
            for (FcDebtEntity debt : debts) {
                if (debt == null) continue;
                if (debt.getApr() == null) {
                    missingApr = true;
                }
                if (debt.getMonthlyPayment() != null) {
                    totalMonthlyPayment = totalMonthlyPayment.add(debt.getMonthlyPayment());
                }
            }
        }
        if (missingApr) {
            addWarning(warnings, "DEBT_MISSING_APR");
        }

        double dti = 0.0;
        if (avgIncome <= 0) {
            addWarning(warnings, "DEBT_ADVICE_INSUFFICIENT_INCOME");
        } else {
            dti = totalMonthlyPayment.doubleValue() / avgIncome;
        }

        BigDecimal liquidCash = sumCashAssets(assets);
        double emergencyFundMonths = 0.0;
        if (avgExpense <= 0) {
            addWarning(warnings, "EMERGENCY_FUND_INSUFFICIENT_EXPENSE");
        } else if (liquidCash == null || liquidCash.compareTo(BigDecimal.ZERO) <= 0) {
            addWarning(warnings, "EMERGENCY_FUND_UNKNOWN");
        } else {
            emergencyFundMonths = liquidCash.divide(BigDecimal.valueOf(avgExpense), 4, RoundingMode.HALF_UP).doubleValue();
        }

        List<AdviceActionSuggestionDTO> debtSuggestions = buildDebtSuggestions(
                debts,
                cashflowSummary,
                emergencyFundMonths,
                emergencyTargetMonths,
                dti,
                dtiWarn,
                dtiDanger,
                debtStrategy,
                minNetForExtraPayment,
                warnings);
        List<AdviceActionSuggestionDTO> cashflowSuggestions = buildCashflowSuggestions(
                cashflowSummary,
                emergencyFundMonths,
                emergencyTargetMonths,
                warnings);
        dto.setDebtSuggestions(debtSuggestions);
        dto.setCashflowSuggestions(cashflowSuggestions);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("threshold", driftThreshold);
        meta.put("currentAllocation", allocation);
        meta.put("targetAllocation", targets);
        meta.put("maxPositions", maxPositions);
        meta.put("cashflowStatus", cashflowSummary.monthsCount() >= 2 ? "AVAILABLE" : "INSUFFICIENT");
        meta.put("debtStatus", debts == null || debts.isEmpty() ? "MISSING" : "AVAILABLE");
        meta.put("cashflowMonths", cashflowSummary.monthsCount());
        meta.put("avgMonthlyIncome", cashflowSummary.avgIncome());
        meta.put("avgMonthlyExpense", cashflowSummary.avgExpense());
        meta.put("avgMonthlyNet", cashflowSummary.avgNet());
        meta.put("debtMonthlyPaymentTotal", totalMonthlyPayment);
        meta.put("emergencyFundMonths", emergencyFundMonths);
        meta.put("dti", dti);
        meta.put("warnings", warnings);
        if (ruleSnapshot != null) {
            meta.put("ruleSetCode", ruleSnapshot.getCode());
            meta.put("ruleSetVersion", ruleSnapshot.getVersion());
            meta.put("ruleSetSource", ruleSnapshot.getSource());
        }
        dto.setMeta(meta);
        return dto;
    }

    private Map<String, Double> resolveTargets() {
        if (rebalanceTemplateRegistry == null) {
            return new LinkedHashMap<>(RebalanceTemplateDefaults.defaultTargets());
        }
        RebalanceTemplateSnapshot snapshot = rebalanceTemplateRegistry.getActive();
        if (snapshot == null || snapshot.getTargets() == null || snapshot.getTargets().isEmpty()) {
            return new LinkedHashMap<>(RebalanceTemplateDefaults.defaultTargets());
        }
        return new LinkedHashMap<>(snapshot.getTargets());
    }

    private List<AdviceRebalanceSuggestionDTO> buildSuggestions(Map<String, Double> allocation,
                                                                Map<String, Double> targets,
                                                                double threshold,
                                                                BigDecimal totalAssets) {
        List<AdviceRebalanceSuggestionDTO> suggestions = new ArrayList<>();
        for (String key : unionKeys(allocation, targets)) {
            double current = allocation.getOrDefault(key, 0.0);
            double target = targets.getOrDefault(key, 0.0);
            double diff = current - target;
            if (Math.abs(diff) < threshold) continue;
            AdviceRebalanceSuggestionDTO dto = new AdviceRebalanceSuggestionDTO();
            dto.setAssetType(key);
            dto.setCurrentWeight(round4(current));
            dto.setTargetWeight(round4(target));
            dto.setDiffWeight(round4(diff));
            dto.setAction(diff > 0 ? "SELL" : "BUY");
            dto.setActionWeight(round4(Math.abs(diff)));
            if (totalAssets != null) {
                BigDecimal amount = totalAssets.multiply(BigDecimal.valueOf(Math.abs(diff)));
                dto.setAmountHint(amount.setScale(2, RoundingMode.HALF_UP));
            }
            suggestions.add(dto);
        }
        return suggestions;
    }

    private Map<String, Double> buildAllocation(List<FcAssetEntity> assets, BigDecimal totalAssets) {
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        if (assets != null) {
            for (FcAssetEntity a : assets) {
                if (a == null || a.getAmount() == null) continue;
                String type = a.getType() != null ? a.getType() : "OTHER";
                sums.merge(type, a.getAmount(), BigDecimal::add);
            }
        }
        Map<String, Double> allocation = new LinkedHashMap<>();
        if (totalAssets == null || totalAssets.compareTo(BigDecimal.ZERO) <= 0) {
            return allocation;
        }
        for (Map.Entry<String, BigDecimal> entry : sums.entrySet()) {
            double ratio = entry.getValue().divide(totalAssets, 6, RoundingMode.HALF_UP).doubleValue();
            allocation.put(entry.getKey(), ratio);
        }
        return allocation;
    }

    private BigDecimal sumAssets(List<FcAssetEntity> assets) {
        if (assets == null) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (FcAssetEntity a : assets) {
            if (a != null && a.getAmount() != null) {
                sum = sum.add(a.getAmount());
            }
        }
        return sum;
    }

    private double maxRatio(Map<String, Double> allocation) {
        double max = 0.0;
        for (Double v : allocation.values()) {
            if (v != null && v > max) max = v;
        }
        return max;
    }

    private Map<String, Double> parseWeights(AdviceRuleSnapshot snapshot) {
        String raw = snapshot == null ? null : snapshot.getString(AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, null);
        String fallback = AdviceRuleDefaults.defaultParams().get(AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON).getDefaultValue();
        if (raw == null || raw.isBlank()) {
            raw = fallback;
        }
        try {
            Map<String, Double> parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Double>>() {});
            if (parsed == null || parsed.isEmpty()) {
                return objectMapper.readValue(fallback, new TypeReference<Map<String, Double>>() {});
            }
            return parsed;
        } catch (Exception e) {
            try {
                return objectMapper.readValue(fallback, new TypeReference<Map<String, Double>>() {});
            } catch (Exception ex) {
                log.warn("[AdviceRebalance] parse weights failed", ex);
                Map<String, Double> defaults = new LinkedHashMap<>();
                defaults.put("tradeFreq", 0.4);
                defaults.put("concentration", 0.3);
                defaults.put("cashDrag", 0.3);
                return defaults;
            }
        }
    }

    private double readDecimal(AdviceRuleSnapshot snapshot, String key, double fallback) {
        if (snapshot == null) return fallback;
        return snapshot.getDecimal(key, BigDecimal.valueOf(fallback)).doubleValue();
    }

    private int readInt(AdviceRuleSnapshot snapshot, String key, int fallback) {
        if (snapshot == null) return fallback;
        return snapshot.getInt(key, fallback);
    }

    private double readDecimal(AdviceRuleSnapshot snapshot, String key, double fallback, List<String> warnings) {
        addRuleParamParseWarning(snapshot, key, warnings);
        return readDecimal(snapshot, key, fallback);
    }

    private int readInt(AdviceRuleSnapshot snapshot, String key, int fallback, List<String> warnings) {
        addRuleParamParseWarning(snapshot, key, warnings);
        return readInt(snapshot, key, fallback);
    }

    private String readString(AdviceRuleSnapshot snapshot, String key, String fallback, List<String> warnings) {
        addRuleParamParseWarning(snapshot, key, warnings);
        if (snapshot == null) return fallback;
        return snapshot.getString(key, fallback);
    }

    private String resolveRiskBand(AdviceRuleSnapshot snapshot, int riskScore) {
        int low = readInt(snapshot, AdviceRuleDefaults.RISK_SCORE_LOW, 30);
        int mid = readInt(snapshot, AdviceRuleDefaults.RISK_SCORE_MID, 60);
        int high = readInt(snapshot, AdviceRuleDefaults.RISK_SCORE_HIGH, 80);
        if (riskScore >= high) return "HIGH";
        if (riskScore >= mid) return "MID";
        if (riskScore >= low) return "LOW";
        return "VERY_LOW";
    }

    private List<String> unionKeys(Map<String, Double> a, Map<String, Double> b) {
        List<String> keys = new ArrayList<>();
        if (a != null) {
            for (String k : a.keySet()) {
                if (!keys.contains(k)) keys.add(k);
            }
        }
        if (b != null) {
            for (String k : b.keySet()) {
                if (!keys.contains(k)) keys.add(k);
            }
        }
        return keys;
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private int clampScore(float value) {
        int v = Math.round(value);
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }

    private void addRuleParamParseWarning(AdviceRuleSnapshot snapshot, String key, List<String> warnings) {
        if (snapshot == null || snapshot.getWarnings() == null) return;
        String invalid = AdviceRuleRegistry.WARN_PARAM_INVALID_PREFIX + key;
        if (snapshot.getWarnings().contains(invalid)) {
            addWarning(warnings, "RULE_PARAM_PARSE_ERROR");
        }
    }

    private void addWarning(List<String> warnings, String code) {
        if (code == null || warnings == null) return;
        if (!warnings.contains(code)) {
            warnings.add(code);
        }
    }

    private List<FcDebtEntity> loadActiveDebts(Long userId) {
        if (debtMapper == null || userId == null) {
            return new ArrayList<>();
        }
        return debtMapper.selectList(
                new LambdaQueryWrapper<FcDebtEntity>()
                        .eq(FcDebtEntity::getUserId, userId)
                        .eq(FcDebtEntity::getIsActive, 1));
    }

    private List<FcCashflowMonthEntity> loadRecentCashflow(Long userId, int limit) {
        if (cashflowMonthMapper == null || userId == null) {
            return new ArrayList<>();
        }
        return cashflowMonthMapper.selectList(
                new LambdaQueryWrapper<FcCashflowMonthEntity>()
                        .eq(FcCashflowMonthEntity::getUserId, userId)
                        .orderByDesc(FcCashflowMonthEntity::getMonth)
                        .last("LIMIT " + Math.max(1, limit)));
    }

    private CashflowSummary summarizeCashflow(List<FcCashflowMonthEntity> months) {
        if (months == null || months.isEmpty()) {
            return new CashflowSummary(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpense = BigDecimal.ZERO;
        BigDecimal sumNet = BigDecimal.ZERO;
        for (FcCashflowMonthEntity m : months) {
            if (m == null) continue;
            if (m.getIncome() != null) sumIncome = sumIncome.add(m.getIncome());
            if (m.getExpense() != null) sumExpense = sumExpense.add(m.getExpense());
            if (m.getNet() != null) sumNet = sumNet.add(m.getNet());
        }
        BigDecimal count = BigDecimal.valueOf(months.size());
        BigDecimal avgIncome = sumIncome.divide(count, 4, RoundingMode.HALF_UP);
        BigDecimal avgExpense = sumExpense.divide(count, 4, RoundingMode.HALF_UP);
        BigDecimal avgNet = sumNet.divide(count, 4, RoundingMode.HALF_UP);
        return new CashflowSummary(months.size(), avgIncome, avgExpense, avgNet);
    }

    private BigDecimal sumCashAssets(List<FcAssetEntity> assets) {
        if (assets == null) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (FcAssetEntity a : assets) {
            if (a != null && "CASH".equalsIgnoreCase(a.getType()) && a.getAmount() != null) {
                sum = sum.add(a.getAmount());
            }
        }
        return sum;
    }

    private List<AdviceActionSuggestionDTO> buildDebtSuggestions(List<FcDebtEntity> debts,
                                                                 CashflowSummary cashflow,
                                                                 double emergencyFundMonths,
                                                                 int emergencyTargetMonths,
                                                                 double dti,
                                                                 double dtiWarn,
                                                                 double dtiDanger,
                                                                 String debtStrategy,
                                                                 double minNetForExtraPayment,
                                                                 List<String> warnings) {
        List<AdviceActionSuggestionDTO> suggestions = new ArrayList<>();
        double avgIncome = toDouble(cashflow.avgIncome(), 0.0);
        double avgNet = toDouble(cashflow.avgNet(), 0.0);
        if (avgIncome <= 0) {
            addWarning(warnings, "DEBT_ADVICE_INSUFFICIENT_INCOME");
            suggestions.add(suggestion("DEBT_PRIORITY", "建立收入基线",
                    "补充或校准月收入数据，先明确债务负担水平。", "HIGH"));
            return suggestions;
        }

        if (avgNet < 0) {
            suggestions.add(suggestion("CASHFLOW_STABILIZE", "先稳定现金流",
                    "当前月度结余为负，建议先稳住收支再进行额外还款。", "HIGH"));
        }

        if (emergencyFundMonths < emergencyTargetMonths) {
            suggestions.add(suggestion("EMERGENCY_FUND", "补足应急金",
                    "应急金覆盖" + round1(emergencyFundMonths) + "个月，目标为" + emergencyTargetMonths + "个月。", "HIGH"));
        }

        if (dti >= dtiDanger) {
            suggestions.add(suggestion("DTI_RISK", "降低债务压力",
                    "负债月供占收入比为" + pct(dti) + "，高于危险阈值" + pct(dtiDanger) + "。", "HIGH"));
        } else if (dti >= dtiWarn) {
            suggestions.add(suggestion("DTI_RISK", "控制新增债务",
                    "负债月供占收入比为" + pct(dti) + "，接近/超过警戒阈值" + pct(dtiWarn) + "。", "MEDIUM"));
        }

        if (avgNet >= minNetForExtraPayment && debts != null && !debts.isEmpty()) {
            List<FcDebtEntity> sorted = new ArrayList<>(debts);
            sorted.sort((a, b) -> Double.compare(toDouble(b == null ? null : b.getApr(), 0.0),
                    toDouble(a == null ? null : a.getApr(), 0.0)));
            int count = 0;
            for (FcDebtEntity debt : sorted) {
                if (debt == null) continue;
                AdviceActionSuggestionDTO suggestion = new AdviceActionSuggestionDTO();
                suggestion.setType("DEBT_PRIORITY");
                suggestion.setPriority("MEDIUM");
                suggestion.setTitle("优先偿还高息债务");
                String aprText = debt.getApr() == null ? "N/A" : debt.getApr().toPlainString();
                suggestion.setDetail("优先偿还 " + debt.getDebtType() + "/" + debt.getId()
                        + "，APR=" + aprText + "（策略: " + debtStrategy + "）");
                suggestions.add(suggestion);
                count++;
                if (count >= 3) break;
            }
        }
        return suggestions;
    }

    private List<AdviceActionSuggestionDTO> buildCashflowSuggestions(CashflowSummary cashflow,
                                                                     double emergencyFundMonths,
                                                                     int emergencyTargetMonths,
                                                                     List<String> warnings) {
        List<AdviceActionSuggestionDTO> suggestions = new ArrayList<>();
        if (cashflow.monthsCount() < 2) {
            addWarning(warnings, "CASHFLOW_ADVICE_INSUFFICIENT_DATA");
            suggestions.add(suggestion("CASHFLOW_STABILIZE", "完善现金流记录",
                    "至少记录2个月收支数据，才能评估现金流趋势。", "MEDIUM"));
            return suggestions;
        }

        double avgNet = toDouble(cashflow.avgNet(), 0.0);
        if (avgNet < 0) {
            suggestions.add(suggestion("CASHFLOW_STABILIZE", "提升月度结余",
                    "当前月度结余为负，优先控制支出或提高收入，目标净结余>=0。", "HIGH"));
            return suggestions;
        }

        if (emergencyFundMonths < emergencyTargetMonths) {
            suggestions.add(suggestion("EMERGENCY_FUND", "补足应急金",
                    "建议将净结余的50%用于应急金，直到覆盖" + emergencyTargetMonths + "个月支出。", "HIGH"));
        }
        suggestions.add(suggestion("REBALANCE_ALLOCATE", "利用结余进行再平衡",
                "当应急金达标后，将剩余净结余按目标配置进行再平衡。", "LOW"));
        return suggestions;
    }

    private AdviceActionSuggestionDTO suggestion(String type, String title, String detail, String priority) {
        AdviceActionSuggestionDTO dto = new AdviceActionSuggestionDTO();
        dto.setType(type);
        dto.setTitle(title);
        dto.setDetail(detail);
        dto.setPriority(priority);
        return dto;
    }

    private double toDouble(BigDecimal value, double fallback) {
        if (value == null) return fallback;
        return value.doubleValue();
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String pct(double v) {
        return BigDecimal.valueOf(v * 100).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private record CashflowSummary(int monthsCount, BigDecimal avgIncome, BigDecimal avgExpense, BigDecimal avgNet) {}
}
