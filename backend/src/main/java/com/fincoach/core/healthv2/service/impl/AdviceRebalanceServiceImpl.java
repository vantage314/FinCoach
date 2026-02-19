package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleDefaults;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleSnapshot;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleRegistry;
import com.fincoach.core.healthv2.dto.AdviceRebalanceResponseDTO;
import com.fincoach.core.healthv2.dto.AdviceRebalanceSuggestionDTO;
import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.mapper.FcAssetMapper;
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

    public AdviceRebalanceServiceImpl(FcAssetMapper assetMapper,
                                      FcHealthReportMapper healthReportMapper,
                                      BehaviorEventService behaviorEventService,
                                      AdviceRuleRegistry adviceRuleRegistry,
                                      RebalanceTemplateRegistry rebalanceTemplateRegistry,
                                      ObjectMapper objectMapper) {
        this.assetMapper = assetMapper;
        this.healthReportMapper = healthReportMapper;
        this.behaviorEventService = behaviorEventService;
        this.adviceRuleRegistry = adviceRuleRegistry;
        this.rebalanceTemplateRegistry = rebalanceTemplateRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public AdviceRebalanceResponseDTO buildRebalanceAdvice(Long userId) {
        AdviceRebalanceResponseDTO dto = new AdviceRebalanceResponseDTO();
        if (userId == null) {
            return dto;
        }

        List<String> warnings = new ArrayList<>();
        AdviceRuleSnapshot ruleSnapshot = adviceRuleRegistry == null ? null : adviceRuleRegistry.get();
        double driftThreshold = readDecimal(ruleSnapshot, AdviceRuleDefaults.REBALANCE_DRIFT_PCT, 0.05);
        int maxPositions = readInt(ruleSnapshot, AdviceRuleDefaults.MAX_POSITIONS, 8);

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

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("threshold", driftThreshold);
        meta.put("currentAllocation", allocation);
        meta.put("targetAllocation", targets);
        meta.put("maxPositions", maxPositions);
        meta.put("cashflowStatus", "NOT_AVAILABLE");
        meta.put("debtStatus", "NOT_AVAILABLE");
        warnings.add("CASHFLOW_NOT_AVAILABLE");
        warnings.add("DEBT_NOT_AVAILABLE");
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
}
