package com.fincoach.core.healthv2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleDefaults;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleParamValue;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleSnapshot;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleValueType;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleRegistry;
import com.fincoach.core.healthv2.dto.AdviceRebalanceResponseDTO;
import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.mapper.FcAssetMapper;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateRegistry;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateSnapshot;
import com.fincoach.core.healthv2.service.impl.AdviceRebalanceServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

public class AdviceRebalanceServiceTest {

    @Test
    public void testRebalanceSuggestionsGenerated() {
        FcAssetMapper assetMapper = Mockito.mock(FcAssetMapper.class);
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        BehaviorEventService behaviorEventService = Mockito.mock(BehaviorEventService.class);
        AdviceRuleRegistry ruleRegistry = Mockito.mock(AdviceRuleRegistry.class);
        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);

        List<FcAssetEntity> assets = List.of(asset("STOCK", 80), asset("BOND", 20));
        Mockito.when(assetMapper.selectList(any())).thenReturn(assets);
        Mockito.when(reportMapper.selectOne(any())).thenReturn(null);
        Mockito.when(behaviorEventService.countByType(anyLong(), anyInt())).thenReturn(Map.of());

        AdviceRuleSnapshot snapshot = snapshotWith(
                Map.of(
                        AdviceRuleDefaults.REBALANCE_DRIFT_PCT, "0.05",
                        AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, "{\"tradeFreq\":0.4,\"concentration\":0.3,\"cashDrag\":0.3}",
                        AdviceRuleDefaults.MAX_POSITIONS, "8",
                        AdviceRuleDefaults.RISK_SCORE_LOW, "30",
                        AdviceRuleDefaults.RISK_SCORE_MID, "60",
                        AdviceRuleDefaults.RISK_SCORE_HIGH, "80"
                )
        );
        Mockito.when(ruleRegistry.get()).thenReturn(snapshot);

        Map<String, Double> targets = new LinkedHashMap<>();
        targets.put("STOCK", 0.5);
        targets.put("BOND", 0.5);
        RebalanceTemplateSnapshot templateSnapshot = new RebalanceTemplateSnapshot(1L, "BALANCED", 1,
                RebalanceTemplateSnapshot.SOURCE_DB_ACTIVE, targets, List.of());
        Mockito.when(templateRegistry.getActive()).thenReturn(templateSnapshot);

        AdviceRebalanceServiceImpl service = new AdviceRebalanceServiceImpl(
                assetMapper, reportMapper, behaviorEventService, ruleRegistry, templateRegistry, new ObjectMapper());

        AdviceRebalanceResponseDTO dto = service.buildRebalanceAdvice(1L);
        assertNotNull(dto);
        assertNotNull(dto.getRebalanceSuggestions());
        assertTrue(dto.getRebalanceSuggestions().stream()
                .anyMatch(s -> "STOCK".equals(s.getAssetType()) && "SELL".equals(s.getAction())));
    }

    @Test
    public void testBehaviorScoreAffectedByWeights() {
        FcAssetMapper assetMapper = Mockito.mock(FcAssetMapper.class);
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        BehaviorEventService behaviorEventService = Mockito.mock(BehaviorEventService.class);
        AdviceRuleRegistry ruleRegistry = Mockito.mock(AdviceRuleRegistry.class);
        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);

        List<FcAssetEntity> assets = List.of(asset("CASH", 60), asset("STOCK", 40));
        Mockito.when(assetMapper.selectList(any())).thenReturn(assets);
        Mockito.when(reportMapper.selectOne(any())).thenReturn(null);
        Mockito.when(behaviorEventService.countByType(anyLong(), anyInt())).thenReturn(Map.of());
        Mockito.when(templateRegistry.getActive()).thenReturn(null);

        AdviceRuleSnapshot cashHeavy = snapshotWith(
                Map.of(
                        AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, "{\"tradeFreq\":0.0,\"concentration\":0.0,\"cashDrag\":1.0}",
                        AdviceRuleDefaults.REBALANCE_DRIFT_PCT, "0.05",
                        AdviceRuleDefaults.MAX_POSITIONS, "8",
                        AdviceRuleDefaults.RISK_SCORE_LOW, "30",
                        AdviceRuleDefaults.RISK_SCORE_MID, "60",
                        AdviceRuleDefaults.RISK_SCORE_HIGH, "80"
                )
        );
        AdviceRuleSnapshot tradeHeavy = snapshotWith(
                Map.of(
                        AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, "{\"tradeFreq\":1.0,\"concentration\":0.0,\"cashDrag\":0.0}",
                        AdviceRuleDefaults.REBALANCE_DRIFT_PCT, "0.05",
                        AdviceRuleDefaults.MAX_POSITIONS, "8",
                        AdviceRuleDefaults.RISK_SCORE_LOW, "30",
                        AdviceRuleDefaults.RISK_SCORE_MID, "60",
                        AdviceRuleDefaults.RISK_SCORE_HIGH, "80"
                )
        );
        Mockito.when(ruleRegistry.get()).thenReturn(cashHeavy, tradeHeavy);

        AdviceRebalanceServiceImpl service = new AdviceRebalanceServiceImpl(
                assetMapper, reportMapper, behaviorEventService, ruleRegistry, templateRegistry, new ObjectMapper());

        AdviceRebalanceResponseDTO dto1 = service.buildRebalanceAdvice(1L);
        AdviceRebalanceResponseDTO dto2 = service.buildRebalanceAdvice(1L);

        assertTrue(dto1.getBehaviorScore() < dto2.getBehaviorScore());
    }

    private AdviceRuleSnapshot snapshotWith(Map<String, String> values) {
        Map<String, com.fincoach.core.healthv2.advice.rules.AdviceRuleParamValue> params = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : values.entrySet()) {
            AdviceRuleValueType type = AdviceRuleValueType.JSON;
            if (AdviceRuleDefaults.REBALANCE_DRIFT_PCT.equals(e.getKey())) {
                type = AdviceRuleValueType.DECIMAL;
            } else if (AdviceRuleDefaults.MAX_POSITIONS.equals(e.getKey())
                    || AdviceRuleDefaults.RISK_SCORE_LOW.equals(e.getKey())
                    || AdviceRuleDefaults.RISK_SCORE_MID.equals(e.getKey())
                    || AdviceRuleDefaults.RISK_SCORE_HIGH.equals(e.getKey())) {
                type = AdviceRuleValueType.INT;
            }
            AdviceRuleParamValue value = AdviceRuleParamValue.parse(e.getKey(), type, e.getValue(), "TEST");
            params.put(e.getKey(), value);
        }
        return new AdviceRuleSnapshot(1L, AdviceRuleDefaults.DEFAULT_CODE, 1,
                AdviceRuleSnapshot.SOURCE_DB_ACTIVE, params, List.of(), List.of());
    }

    private FcAssetEntity asset(String type, double amount) {
        FcAssetEntity entity = new FcAssetEntity();
        entity.setType(type);
        entity.setAmount(BigDecimal.valueOf(amount));
        return entity;
    }
}
